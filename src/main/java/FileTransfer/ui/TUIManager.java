package FileTransfer.ui;

import FileTransfer.core.discovery.UdpBroadcaster;
import FileTransfer.core.discovery.UdpListener;
import FileTransfer.core.network.PeerConnection;
import FileTransfer.core.network.TcpServer;
import FileTransfer.core.peer.Peer;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class TUIManager {
    private Terminal terminal;
    private Screen screen;
    private MultiWindowTextGUI gui;
    private BasicWindow mainWindow;
    
    private UdpListener listener;
    private Panel peerListPanel;
    private Label statusLabel;
    private Label timeLabel;
    private Label peerCountLabel;
    private AtomicBoolean running = new AtomicBoolean(true);
    private String localIP;
    private String localName;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    public TUIManager(UdpBroadcaster broadcaster, UdpListener listener, TcpServer tcpServer) {
        this.listener = listener;
        try {
            this.localIP = InetAddress.getLocalHost().getHostAddress();
            this.localName = System.getProperty("user.name");
        } catch (Exception e) {
            this.localIP = "127.0.0.1";
            this.localName = "Unknown";
        }
    }
    
    public void start() throws IOException {
        // Create terminal
        terminal = new DefaultTerminalFactory().createTerminal();
        screen = new TerminalScreen(terminal);
        screen.startScreen();
        
        // Create GUI
        gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLACK));
        
        // Create main window
        createMainWindow();
        
        // Start refresh thread
        startRefreshThread();
        
        // Process GUI (blocking)
        gui.addWindowAndWait(mainWindow);
        
        // Cleanup when window closes
        screen.stopScreen();
    }
    
    private void createMainWindow() {
        mainWindow = new BasicWindow("LAN File Transfer - P2P");
        mainWindow.setHints(java.util.Arrays.asList(Window.Hint.CENTERED, Window.Hint.FIT_TERMINAL_WINDOW));
        
        Panel mainPanel = new Panel();
        mainPanel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
        
        // Header
        Panel headerPanel = new Panel();
        headerPanel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
        
        Label titleLabel = new Label("╔════════════════════════════════════════════╗");
        titleLabel.setForegroundColor(TextColor.ANSI.CYAN_BRIGHT);
        headerPanel.addComponent(titleLabel);
        
        Label titleText = new Label("║       P2P LAN File Transfer System         ║");
        titleText.setForegroundColor(TextColor.ANSI.CYAN_BRIGHT);
        headerPanel.addComponent(titleText);
        
        Label titleBottom = new Label("╚════════════════════════════════════════════╝");
        titleBottom.setForegroundColor(TextColor.ANSI.CYAN_BRIGHT);
        headerPanel.addComponent(titleBottom);
        
        mainPanel.addComponent(headerPanel);
        
        // Local info
        Panel infoPanel = new Panel();
        infoPanel.setLayoutManager(new LinearLayout(Direction.HORIZONTAL));
        
        Label localLabel = new Label("You: " + localName + " @ " + localIP);
        localLabel.setForegroundColor(TextColor.ANSI.GREEN);
        infoPanel.addComponent(localLabel);
        
        infoPanel.addComponent(new EmptySpace(new TerminalSize(5, 1)));
        
        timeLabel = new Label("Time: " + LocalDateTime.now().format(TIME_FORMAT));
        timeLabel.setForegroundColor(TextColor.ANSI.WHITE);
        infoPanel.addComponent(timeLabel);
        
        mainPanel.addComponent(infoPanel.withBorder(Borders.singleLine("Local Info")));
        
        mainPanel.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        
        // Peer count label
        peerCountLabel = new Label("Discovered Peers: 0");
        peerCountLabel.setForegroundColor(TextColor.ANSI.YELLOW);
        mainPanel.addComponent(peerCountLabel);
        
        // Peer list section
        peerListPanel = new Panel();
        peerListPanel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
        peerListPanel.setPreferredSize(new TerminalSize(60, 10));
        mainPanel.addComponent(peerListPanel.withBorder(Borders.doubleLine("Online Peers")));
        
        mainPanel.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        
        // Buttons with better styling
        Panel buttonPanel = new Panel();
        buttonPanel.setLayoutManager(new LinearLayout(Direction.HORIZONTAL));
        
        Button refreshButton = new Button("↻ Refresh", this::refreshPeerList);
        Button sendButton = new Button("📁 Send File", this::showFileBrowser);
        Button exitButton = new Button("✕ Exit", this::exit);
        
        buttonPanel.addComponent(refreshButton);
        buttonPanel.addComponent(new EmptySpace(new TerminalSize(2, 1)));
        buttonPanel.addComponent(sendButton);
        buttonPanel.addComponent(new EmptySpace(new TerminalSize(2, 1)));
        buttonPanel.addComponent(exitButton);
        
        mainPanel.addComponent(buttonPanel);
        
        mainPanel.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        
        // Status bar
        Panel statusPanel = new Panel();
        statusPanel.setLayoutManager(new LinearLayout(Direction.HORIZONTAL));
        
        statusLabel = new Label("Ready - Press Tab to navigate, Enter to select");
        statusLabel.setForegroundColor(TextColor.ANSI.GREEN_BRIGHT);
        statusPanel.addComponent(statusLabel);
        
        mainPanel.addComponent(statusPanel.withBorder(Borders.singleLine("Status")));
        
        // Help text
        Label helpLabel = new Label("[Tab] Navigate  [Enter] Select  [Esc] Exit");
        helpLabel.setForegroundColor(TextColor.ANSI.WHITE);
        mainPanel.addComponent(helpLabel);
        
        mainWindow.setComponent(mainPanel);
        
        // Initial peer list load
        refreshPeerList();
        
        // Handle window close
        mainWindow.addWindowListener(new WindowListenerAdapter() {
            @Override
            public void onUnhandledInput(Window basePane, com.googlecode.lanterna.input.KeyStroke keyStroke, AtomicBoolean hasBeenHandled) {
                // Allow ESC to close
                if (keyStroke.getKeyType() == com.googlecode.lanterna.input.KeyType.Escape) {
                    exit();
                }
            }
        });
    }
    
    private void refreshPeerList() {
        peerListPanel.removeAllComponents();
        
        if (listener == null || listener.peerManager == null) {
            peerListPanel.addComponent(new Label("⚠ Peer manager not initialized"));
            return;
        }
        
        // Clean up timed-out peers
        listener.peerManager.deleteTimedOutPeers();
        
        // Get peers and filter out self
        List<Peer> allPeers = listener.peerManager.getAlivePeers();
        List<Peer> peers = allPeers.stream()
            .filter(peer -> !peer.getIPAddress().equals(localIP))
            .collect(Collectors.toList());
        
        // Update peer count
        peerCountLabel.setText("Discovered Peers: " + peers.size());
        
        // Update time
        timeLabel.setText("Time: " + LocalDateTime.now().format(TIME_FORMAT));
        
        if (peers.isEmpty()) {
            Label emptyLabel = new Label("  Scanning for peers on the network...");
            emptyLabel.setForegroundColor(TextColor.ANSI.YELLOW);
            peerListPanel.addComponent(emptyLabel);
            
            Label hintLabel = new Label("  (Other devices must be running this app)");
            hintLabel.setForegroundColor(TextColor.ANSI.WHITE);
            peerListPanel.addComponent(hintLabel);
        } else {
            for (int i = 0; i < peers.size(); i++) {
                Peer peer = peers.get(i);
                
                Panel peerPanel = new Panel();
                peerPanel.setLayoutManager(new LinearLayout(Direction.HORIZONTAL));
                
                // Peer info with number
                String peerInfo = String.format("  %d. %-15s @ %-15s", 
                    i + 1, 
                    truncate(peer.getPeerName(), 15),
                    peer.getIPAddress());
                
                Label peerLabel = new Label(peerInfo);
                peerLabel.setForegroundColor(TextColor.ANSI.GREEN_BRIGHT);
                
                // Select button
                final Peer selectedPeer = peer;
                Button selectButton = new Button("Send →", () -> selectPeer(selectedPeer));
                
                peerPanel.addComponent(peerLabel);
                peerPanel.addComponent(new EmptySpace(new TerminalSize(2, 1)));
                peerPanel.addComponent(selectButton);
                
                peerListPanel.addComponent(peerPanel);
            }
        }
        
        updateStatus("Ready - " + peers.size() + " peer(s) online");
    }
    
    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen - 2) + ".." : str;
    }
    
    private void selectPeer(Peer peer) {
        updateStatus("Selected: " + peer.getPeerName() + " - Choose a file to send");
        // Show file browser to send file to this peer
        showFileBrowserForPeer(peer);
    }
    
    private void showFileBrowser() {
        if (listener == null || listener.peerManager == null) {
            showError("Peer manager not initialized");
            return;
        }
        
        // Filter out self from peers list
        List<Peer> peers = listener.peerManager.getAlivePeers().stream()
            .filter(peer -> !peer.getIPAddress().equals(localIP))
            .collect(Collectors.toList());
            
        if (peers.isEmpty()) {
            showError("No peers available.\n\nMake sure other devices are running this application on the same network.");
            return;
        }
        
        // Show peer selection dialog
        PeerSelectionDialog dialog = new PeerSelectionDialog(peers);
        Peer selectedPeer = dialog.getSelectedPeer(gui);
        
        if (selectedPeer != null) {
            showFileBrowserForPeer(selectedPeer);
        }
    }
    
    private void showFileBrowserForPeer(Peer peer) {
        FileBrowserDialog fileBrowser = new FileBrowserDialog();
        File selectedFile = fileBrowser.getSelectedFile(gui);
        
        if (selectedFile != null) {
            sendFileToPeer(peer, selectedFile);
        } else {
            updateStatus("File selection cancelled");
        }
    }
    
    private void sendFileToPeer(Peer peer, File file) {
        updateStatus("⏳ Connecting to " + peer.getPeerName() + "...");
        
        // Connect to peer and send file in background thread
        new Thread(() -> {
            try {
                Socket socket = new Socket(peer.getIPAddress(), 50001);
                PeerConnection connection = new PeerConnection(socket);
                
                // Start connection thread
                Thread connThread = new Thread(connection);
                connThread.start();
                
                // Wait a bit for HELLO handshake
                Thread.sleep(500);
                
                // Send file request
                connection.sendFileRequest(file);
                
                String fileSize = formatFileSize(file.length());
                gui.getGUIThread().invokeLater(() -> {
                    updateStatus("✓ Sent: " + file.getName() + " (" + fileSize + ") → " + peer.getPeerName());
                    showInfo("File Transfer", 
                        "File request sent successfully!\n\n" +
                        "File: " + file.getName() + "\n" +
                        "Size: " + fileSize + "\n" +
                        "To: " + peer.getPeerName());
                });
                
            } catch (Exception e) {
                gui.getGUIThread().invokeLater(() -> {
                    showError("Connection failed!\n\n" + 
                        "Peer: " + peer.getPeerName() + "\n" +
                        "Error: " + e.getMessage());
                    updateStatus("✗ Connection failed to " + peer.getPeerName());
                });
            }
        }).start();
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    private void startRefreshThread() {
        Thread refreshThread = new Thread(() -> {
            while (running.get()) {
                try {
                    Thread.sleep(3000); // Refresh every 3 seconds
                    if (gui != null && gui.getGUIThread() != null) {
                        gui.getGUIThread().invokeLater(this::refreshPeerList);
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        refreshThread.setDaemon(true);
        refreshThread.setName("TUI-Refresh");
        refreshThread.start();
    }
    
    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
    
    private void showError(String message) {
        MessageDialog.showMessageDialog(gui, "⚠ Error", message, MessageDialogButton.OK);
    }
    
    private void showInfo(String title, String message) {
        MessageDialog.showMessageDialog(gui, title, message, MessageDialogButton.OK);
    }
    
    private void exit() {
        running.set(false);
        mainWindow.close();
    }
    
    public void shutdown() {
        running.set(false);
        try {
            if (screen != null) {
                screen.stopScreen();
            }
        } catch (IllegalStateException | IOException e) {
            // Ignore - screen may already be closed
        }
    }
}
