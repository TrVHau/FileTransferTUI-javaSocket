package FileTransfer.ui;

import FileTransfer.core.discovery.UdpBroadcaster;
import FileTransfer.core.discovery.UdpListener;
import FileTransfer.core.network.PeerConnection;
import FileTransfer.core.network.TcpServer;
import FileTransfer.core.peer.Peer;
import FileTransfer.core.transfer.TransferCallback;
import FileTransfer.core.transfer.TransferInfo;
import FileTransfer.core.transfer.TransferManager;
import FileTransfer.core.util.FormatUtils;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class TUIManager implements TransferCallback, TransferManager.TransferManagerListener {

    private Terminal terminal;
    private Screen screen;
    private MultiWindowTextGUI gui;
    private BasicWindow mainWindow;

    private final UdpListener listener;
    private final UdpBroadcaster broadcaster;
    private final TransferManager transferManager;

    // UI Components
    private Panel peerListPanel;
    private Panel transferPanel;
    private Panel historyPanel;
    private Label statusLabel;
    private Label timeLabel;
    private Label peerCountLabel;
    private Label transferProgressLabel;
    private Label networkStatsLabel;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final String localIP;
    private final String localName;
    private final String downloadPath;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public TUIManager(UdpBroadcaster broadcaster, UdpListener listener, TcpServer tcpServer) {
        this.listener = listener;
        this.broadcaster = broadcaster;
        this.transferManager = new TransferManager();
        this.transferManager.addListener(this);

        this.downloadPath = System.getProperty("user.home") + "/Downloads/FileTransfer";
        new File(downloadPath).mkdirs();

        String ip, name;
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
            name = System.getProperty("user.name");
        } catch (Exception e) {
            ip = "127.0.0.1";
            name = "Unknown";
        }
        this.localIP = ip;
        this.localName = name;
    }

    public TransferManager getTransferManager()  { return transferManager; }
    public TransferCallback getTransferCallback() { return this; }
    public String getDownloadPath()               { return downloadPath; }

    // ═══════════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════════

    public void start() throws IOException {
        terminal = new DefaultTerminalFactory().createTerminal();
        screen = new TerminalScreen(terminal);
        screen.startScreen();

        gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLACK));
        buildMainWindow();
        startRefreshThread();
        gui.addWindowAndWait(mainWindow);
        screen.stopScreen();
    }

    public void shutdown() {
        running.set(false);
        transferManager.removeListener(this);
        try {
            if (screen != null) screen.stopScreen();
        } catch (IllegalStateException | IOException ignored) {}
    }

    // ═══════════════════════════════════════════════════════════════
    // Window construction
    // ═══════════════════════════════════════════════════════════════

    private void buildMainWindow() {
        mainWindow = new BasicWindow();
        mainWindow.setHints(java.util.Arrays.asList(Window.Hint.CENTERED, Window.Hint.FIT_TERMINAL_WINDOW));

        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));

        // Header
        root.addComponent(buildHeader());

        // Local info bar
        root.addComponent(buildInfoPanel().withBorder(Borders.singleLine(" Local ")));

        // Content: peers (left) + transfers (right)
        Panel content = new Panel(new LinearLayout(Direction.HORIZONTAL));
        content.addComponent(buildPeerSection());
        content.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        content.addComponent(buildTransferSection());
        root.addComponent(content);

        // Buttons
        root.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        root.addComponent(buildButtons());

        // Status bar
        root.addComponent(buildStatusBar().withBorder(Borders.singleLine("Status")));

        // Keybinding hints
        Label help = new Label(" [Tab] Navigate  [Enter] Select  [R] Refresh  [S] Send  [Q/Esc] Exit ");
        help.setForegroundColor(TextColor.ANSI.WHITE);
        root.addComponent(help);

        mainWindow.setComponent(root);
        refreshPeerList();
        updateTransferHistory();

        // Global keyboard shortcuts
        mainWindow.addWindowListener(new WindowListenerAdapter() {
            @Override
            public void onUnhandledInput(Window basePane, com.googlecode.lanterna.input.KeyStroke ks, AtomicBoolean handled) {
                if (ks.getKeyType() == com.googlecode.lanterna.input.KeyType.Escape) {
                    exit();
                } else if (ks.getKeyType() == com.googlecode.lanterna.input.KeyType.Character) {
                    switch (Character.toLowerCase(ks.getCharacter())) {
                        case 'q' -> exit();
                        case 'r' -> refreshPeerList();
                        case 's' -> showSendFlow();
                        case 'h' -> showHelp();
                    }
                }
            }
        });
    }

    // ─── Header ──────────────────────────────────────────────────

    private Panel buildHeader() {
        Panel p = new Panel(new LinearLayout(Direction.VERTICAL));
        String[] art = {
            "╔═══════════════════════════════════════════════════════════════════╗",
            "║   File Transfer  ─  P2P LAN Share                               ║",
            "╚═══════════════════════════════════════════════════════════════════╝"
        };
        for (String line : art) {
            Label l = new Label(line);
            l.setForegroundColor(TextColor.ANSI.CYAN_BRIGHT);
            p.addComponent(l);
        }
        return p;
    }

    // ─── Local info ──────────────────────────────────────────────

    private Panel buildInfoPanel() {
        Panel p = new Panel(new GridLayout(4));

        p.addComponent(new Label(" User:"));
        Label user = new Label(localName);
        user.setForegroundColor(TextColor.ANSI.GREEN_BRIGHT);
        p.addComponent(user);

        p.addComponent(new Label(" IP:"));
        Label ip = new Label(localIP);
        ip.setForegroundColor(TextColor.ANSI.YELLOW);
        p.addComponent(ip);

        p.addComponent(new Label(" Time:"));
        timeLabel = new Label(LocalDateTime.now().format(TIME_FMT));
        timeLabel.setForegroundColor(TextColor.ANSI.WHITE);
        p.addComponent(timeLabel);

        p.addComponent(new Label(" Stats:"));
        networkStatsLabel = new Label("^ 0 B  v 0 B");
        networkStatsLabel.setForegroundColor(TextColor.ANSI.MAGENTA);
        p.addComponent(networkStatsLabel);

        return p;
    }

    // ─── Peer list ───────────────────────────────────────────────

    private Panel buildPeerSection() {
        Panel p = new Panel(new LinearLayout(Direction.VERTICAL));

        peerCountLabel = new Label(" Discovered Peers: 0");
        peerCountLabel.setForegroundColor(TextColor.ANSI.YELLOW);
        p.addComponent(peerCountLabel);

        peerListPanel = new Panel(new LinearLayout(Direction.VERTICAL));
        peerListPanel.setPreferredSize(new TerminalSize(44, 12));
        p.addComponent(peerListPanel.withBorder(Borders.doubleLine(" Online Peers ")));

        return p;
    }

    // ─── Transfer section ────────────────────────────────────────

    private Panel buildTransferSection() {
        Panel p = new Panel(new LinearLayout(Direction.VERTICAL));

        // Current transfer
        transferPanel = new Panel(new LinearLayout(Direction.VERTICAL));
        transferPanel.setPreferredSize(new TerminalSize(35, 5));
        transferProgressLabel = new Label("No active transfer");
        transferProgressLabel.setForegroundColor(TextColor.ANSI.WHITE);
        transferPanel.addComponent(transferProgressLabel);
        p.addComponent(transferPanel.withBorder(Borders.singleLine(" Active Transfer ")));

        // History
        historyPanel = new Panel(new LinearLayout(Direction.VERTICAL));
        historyPanel.setPreferredSize(new TerminalSize(35, 6));
        p.addComponent(historyPanel.withBorder(Borders.singleLine(" Recent Transfers ")));

        return p;
    }

    // ─── Buttons ─────────────────────────────────────────────────

    private Panel buildButtons() {
        Panel p = new Panel(new LinearLayout(Direction.HORIZONTAL));

        p.addComponent(new Button(" Refresh [R]", this::refreshPeerList));
        p.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        p.addComponent(new Button(" Send [S]", this::showSendFlow));
        p.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        p.addComponent(new Button(" Settings", this::showSettings));
        p.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        p.addComponent(new Button(" Help [H]", this::showHelp));
        p.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        p.addComponent(new Button(" Exit [Q]", this::exit));

        return p;
    }

    // ─── Status bar ──────────────────────────────────────────────

    private Panel buildStatusBar() {
        Panel p = new Panel(new LinearLayout(Direction.HORIZONTAL));
        statusLabel = new Label(" Ready - Scanning for peers...");
        statusLabel.setForegroundColor(TextColor.ANSI.GREEN_BRIGHT);
        p.addComponent(statusLabel);
        return p;
    }

    // ═══════════════════════════════════════════════════════════════
    // Refresh logic
    // ═══════════════════════════════════════════════════════════════

    private void refreshPeerList() {
        peerListPanel.removeAllComponents();

        if (listener == null) {
            addLabel(peerListPanel, "  Peer manager not ready", TextColor.ANSI.RED);
            return;
        }

        List<Peer> peers = listener.getPeerManager().getAlivePeers().stream()
                .filter(p -> !p.getPeerID().equals(broadcaster.getPeerID()))
                .collect(Collectors.toList());

        peerCountLabel.setText(" Discovered Peers: " + peers.size());
        timeLabel.setText(LocalDateTime.now().format(TIME_FMT));
        updateNetworkStats();

        if (peers.isEmpty()) {
            addLabel(peerListPanel, "  Scanning for peers...", TextColor.ANSI.YELLOW);
            addLabel(peerListPanel, "", TextColor.ANSI.WHITE);
            addLabel(peerListPanel, "  Make sure other devices are", TextColor.ANSI.WHITE);
            addLabel(peerListPanel, "  running this app on the", TextColor.ANSI.WHITE);
            addLabel(peerListPanel, "  same network.", TextColor.ANSI.WHITE);
        } else {
            for (int i = 0; i < peers.size(); i++) {
                Peer peer = peers.get(i);
                Panel row = new Panel(new LinearLayout(Direction.HORIZONTAL));

                Label num = new Label(String.format(" %d.", i + 1));
                num.setForegroundColor(TextColor.ANSI.WHITE);
                row.addComponent(num);

                Label name = new Label(String.format(" %-14s", FormatUtils.truncate(peer.getPeerName(), 14)));
                name.setForegroundColor(TextColor.ANSI.GREEN_BRIGHT);
                row.addComponent(name);

                Label ip = new Label(peer.getIPAddress());
                ip.setForegroundColor(TextColor.ANSI.CYAN);
                row.addComponent(ip);

                row.addComponent(new EmptySpace(new TerminalSize(1, 1)));

                final Peer target = peer;
                row.addComponent(new Button("Send", () -> showFileBrowserForPeer(target)));

                peerListPanel.addComponent(row);
            }
        }

        setStatus(" Ready - " + peers.size() + " peer(s) online");
    }

    private void updateNetworkStats() {
        TransferManager.TransferStats s = transferManager.getStats();
        networkStatsLabel.setText("^ " + s.getFormattedSent() + "  v " + s.getFormattedReceived());
    }

    private void updateTransferHistory() {
        historyPanel.removeAllComponents();
        List<TransferInfo> recent = transferManager.getRecentTransfers(5);
        if (recent.isEmpty()) {
            addLabel(historyPanel, " No transfers yet", TextColor.ANSI.WHITE);
        } else {
            for (TransferInfo info : recent) {
                Label l = new Label(" " + info.getSummary());
                l.setForegroundColor(switch (info.getStatus()) {
                    case COMPLETED   -> TextColor.ANSI.GREEN;
                    case FAILED      -> TextColor.ANSI.RED;
                    case CANCELLED   -> TextColor.ANSI.YELLOW;
                    case IN_PROGRESS -> TextColor.ANSI.CYAN;
                });
                historyPanel.addComponent(l);
            }
        }
    }

    private void updateCurrentTransfer() {
        transferPanel.removeAllComponents();
        TransferInfo cur = transferManager.getCurrentTransfer();
        if (cur == null || cur.getStatus() != TransferInfo.Status.IN_PROGRESS) {
            transferProgressLabel = new Label(" No active transfer");
            transferProgressLabel.setForegroundColor(TextColor.ANSI.WHITE);
            transferPanel.addComponent(transferProgressLabel);
        } else {
            Label fileLabel = new Label(" " + cur.getDirection().getSymbol() + " " + FormatUtils.truncate(cur.getFileName(), 25));
            fileLabel.setForegroundColor(TextColor.ANSI.CYAN);
            transferPanel.addComponent(fileLabel);

            Label bar = new Label(" " + cur.getProgressBar(20));
            bar.setForegroundColor(TextColor.ANSI.GREEN_BRIGHT);
            transferPanel.addComponent(bar);

            Label speed = new Label(" Speed: " + cur.getFormattedSpeed());
            speed.setForegroundColor(TextColor.ANSI.YELLOW);
            transferPanel.addComponent(speed);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Send flow
    // ═══════════════════════════════════════════════════════════════

    private void showSendFlow() {
        if (listener == null) {
            showError("Peer manager not ready");
            return;
        }
        if (transferManager.hasActiveTransfer()) {
            showError("A transfer is already in progress.\nPlease wait for it to complete.");
            return;
        }

        List<Peer> peers = listener.getPeerManager().getAlivePeers().stream()
                .filter(p -> !p.getPeerID().equals(broadcaster.getPeerID()))
                .collect(Collectors.toList());
        if (peers.isEmpty()) {
            showError("No peers available.\n\nMake sure other devices are running\nthis application on the same network.");
            return;
        }

        Peer target = new PeerSelectionDialog(peers).getSelectedPeer(gui);
        if (target != null) {
            showFileBrowserForPeer(target);
        }
    }

    private void showFileBrowserForPeer(Peer peer) {
        File file = new FileBrowserDialog().getSelectedFile(gui);
        if (file != null) {
            sendFileToPeer(peer, file);
        } else {
            setStatus(" File selection cancelled");
        }
    }

    private void sendFileToPeer(Peer peer, File file) {
        setStatus(" Connecting to " + peer.getPeerName() + "...");

        transferManager.startTransfer(
                file.getName(), file.length(),
                peer.getPeerName(), peer.getIPAddress(),
                TransferInfo.Direction.SENDING);
        updateCurrentTransfer();

        new Thread(() -> {
            try {
                Socket socket = new Socket(peer.getIPAddress(), peer.getTcpPort());
                PeerConnection conn = new PeerConnection(socket, this,
                        broadcaster.getPeerID(), localName);

                Thread connThread = new Thread(conn, "PeerConn-Send");
                connThread.setDaemon(true);
                connThread.start();

                conn.sendFileRequest(file);

                String size = FormatUtils.formatFileSize(file.length());
                invokeLater(() -> setStatus(" Sending: " + file.getName() + " (" + size + ") -> " + peer.getPeerName()));

            } catch (Exception e) {
                transferManager.failTransfer(e.getMessage());
                invokeLater(() -> {
                    showError("Connection failed!\n\nPeer: " + peer.getPeerName() + "\nError: " + e.getMessage());
                    setStatus(" Connection failed to " + peer.getPeerName());
                    updateCurrentTransfer();
                    updateTransferHistory();
                });
            }
        }, "Send-Init").start();
    }

    // ═══════════════════════════════════════════════════════════════
    // Dialogs
    // ═══════════════════════════════════════════════════════════════

    private void showSettings() {
        String msg = "Current Settings\n\n"
                + "Download Path:\n  " + downloadPath + "\n\n"
                + "Local IP: " + localIP + "\n"
                + "User Name: " + localName + "\n\n"
                + "Network Ports:\n"
                + "  UDP Discovery: 50000\n"
                + "  TCP Transfer:  50001\n";
        MessageDialog.showMessageDialog(gui, " Settings", msg, MessageDialogButton.OK);
    }

    private void showHelp() {
        String msg = "File Transfer Help\n\n"
                + "--- Keyboard Shortcuts ---\n"
                + "  [R]     Refresh peer list\n"
                + "  [S]     Send a file\n"
                + "  [H]     This help screen\n"
                + "  [Q/Esc] Exit application\n"
                + "  [Tab]   Navigate elements\n"
                + "  [Enter] Select / activate\n\n"
                + "--- How to Use ---\n"
                + "1. Wait for peers to appear\n"
                + "2. Click [Send] next to a peer\n"
                + "   or press [S] to pick a peer\n"
                + "3. Choose a file to send\n"
                + "4. The receiver will be prompted\n"
                + "5. Transfer begins automatically\n\n"
                + "Files are saved to:\n  " + downloadPath;
        MessageDialog.showMessageDialog(gui, " Help", msg, MessageDialogButton.OK);
    }

    // ═══════════════════════════════════════════════════════════════
    // Utility
    // ═══════════════════════════════════════════════════════════════

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    private void showError(String msg) {
        MessageDialog.showMessageDialog(gui, " Error", msg, MessageDialogButton.OK);
    }

    private void addLabel(Panel panel, String text, TextColor color) {
        Label l = new Label(text);
        l.setForegroundColor(color);
        panel.addComponent(l);
    }

    private void invokeLater(Runnable r) {
        if (gui != null && gui.getGUIThread() != null) {
            gui.getGUIThread().invokeLater(r);
        }
    }

    private void exit() {
        running.set(false);
        mainWindow.close();
    }

    private void startRefreshThread() {
        Thread t = new Thread(() -> {
            while (running.get()) {
                try {
                    Thread.sleep(2000);
                    invokeLater(() -> {
                        refreshPeerList();
                        updateCurrentTransfer();
                    });
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "TUI-Refresh");
        t.setDaemon(true);
        t.start();
    }

    // ═══════════════════════════════════════════════════════════════
    // TransferCallback implementation
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void onProgress(long bytesTransferred, long totalBytes, String fileName) {
        transferManager.updateProgress(bytesTransferred);
        invokeLater(this::updateCurrentTransfer);
    }

    @Override
    public void onTransferStart(String fileName, long totalBytes, boolean isSending) {
        invokeLater(() -> {
            String dir = isSending ? " Sending" : " Receiving";
            setStatus(dir + ": " + fileName + " (" + FormatUtils.formatFileSize(totalBytes) + ")");
            updateCurrentTransfer();
        });
    }

    @Override
    public void onTransferComplete(String fileName, String checksum) {
        transferManager.completeTransfer(checksum);
        invokeLater(() -> {
            setStatus(" Transfer complete: " + fileName);
            updateCurrentTransfer();
            updateTransferHistory();
            updateNetworkStats();
            MessageDialog.showMessageDialog(gui, " Transfer Complete",
                    "File: " + fileName + "\nChecksum: " + checksum.substring(0, Math.min(8, checksum.length())) + "...",
                    MessageDialogButton.OK);
        });
    }

    @Override
    public void onTransferError(String fileName, String error) {
        transferManager.failTransfer(error);
        invokeLater(() -> {
            setStatus(" Transfer failed: " + fileName);
            updateCurrentTransfer();
            updateTransferHistory();
            showError("Transfer Failed\n\nFile: " + fileName + "\nError: " + error);
        });
    }

    @Override
    public boolean onTransferRequest(String peerName, String peerIP, String fileName, long fileSize) {
        // Use CountDownLatch instead of wait/notify to avoid potential deadlock
        final boolean[] result = {false};
        final CountDownLatch latch = new CountDownLatch(1);

        invokeLater(() -> {
            try {
                String msg = String.format(
                        "Incoming File\n\n"
                        + "From: %s (%s)\n"
                        + "File: %s\n"
                        + "Size: %s\n\n"
                        + "Accept this file?",
                        peerName, peerIP, fileName, FormatUtils.formatFileSize(fileSize));

                MessageDialogButton answer = MessageDialog.showMessageDialog(
                        gui, " Incoming Transfer", msg,
                        MessageDialogButton.Yes, MessageDialogButton.No);

                result[0] = (answer == MessageDialogButton.Yes);
                if (result[0]) {
                    transferManager.startTransfer(fileName, fileSize, peerName, peerIP,
                            TransferInfo.Direction.RECEIVING);
                    updateCurrentTransfer();
                }
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            return false;
        }
        return result[0];
    }

    // ═══════════════════════════════════════════════════════════════
    // TransferManagerListener implementation
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void onTransferListChanged() {
        invokeLater(() -> {
            updateTransferHistory();
            updateNetworkStats();
        });
    }

    @Override
    public void onActiveTransferProgress() {
        invokeLater(this::updateCurrentTransfer);
    }
}
