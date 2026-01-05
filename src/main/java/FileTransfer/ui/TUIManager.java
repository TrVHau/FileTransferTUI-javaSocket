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
    private AtomicBoolean running = new AtomicBoolean(true);
    private String localIP;
    
    public TUIManager(UdpBroadcaster broadcaster, UdpListener listener, TcpServer tcpServer) {
        this.listener = listener;
        try {
            this.localIP = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            this.localIP = "127.0.0.1";
        }
    }
    
    public void start() throws IOException {
        // Create terminal
        terminal = new DefaultTerminalFactory().createTerminal();
        screen = new TerminalScreen(terminal);
        screen.startScreen();
        
        // Create GUI
        gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLUE));
        
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
        
        // Title
        Label titleLabel = new Label("=== P2P File Transfer ===");
        titleLabel.setForegroundColor(TextColor.ANSI.CYAN_BRIGHT);
        mainPanel.addComponent(titleLabel);
        mainPanel.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        
        // Peer list section
        mainPanel.addComponent(new Label("Online Peers:"));
        peerListPanel = new Panel();
        peerListPanel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
        peerListPanel.setPreferredSize(new TerminalSize(60, 10));
        mainPanel.addComponent(peerListPanel.withBorder(Borders.singleLine()));
        
        mainPanel.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        
        // Buttons
        Panel buttonPanel = new Panel();
        buttonPanel.setLayoutManager(new LinearLayout(Direction.HORIZONTAL));
        
        Button refreshButton = new Button("Refresh", this::refreshPeerList);
        Button sendButton = new Button("Send File", this::showFileBrowser);
        Button exitButton = new Button("Exit", this::exit);
        
        buttonPanel.addComponent(refreshButton);
        buttonPanel.addComponent(new EmptySpace(new TerminalSize(2, 1)));
        buttonPanel.addComponent(sendButton);
        buttonPanel.addComponent(new EmptySpace(new TerminalSize(2, 1)));
        buttonPanel.addComponent(exitButton);
        
        mainPanel.addComponent(buttonPanel);
        
        mainPanel.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        
        // Status bar
        statusLabel = new Label("Ready");
        statusLabel.setForegroundColor(TextColor.ANSI.GREEN);
        mainPanel.addComponent(statusLabel);
        
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
            peerListPanel.addComponent(new Label("Peer manager not initialized"));
            return;
        }
        
        // Clean up timed-out peers
        listener.peerManager.deleteTimedOutPeers();
        
        // Get peers and filter out self
        List<Peer> allPeers = listener.peerManager.getAlivePeers();
        List<Peer> peers = allPeers.stream()
            .filter(peer -> !peer.getIPAddress().equals(localIP))
            .collect(Collectors.toList());
        
        if (peers.isEmpty()) {
            Label emptyLabel = new Label("No peers discovered yet...");
            emptyLabel.setForegroundColor(TextColor.ANSI.YELLOW);
            peerListPanel.addComponent(emptyLabel);
        } else {
            for (int i = 0; i < peers.size(); i++) {
                Peer peer = peers.get(i);
                String peerInfo = String.format("%d. %s @ %s", 
                    i + 1, 
                    peer.getPeerName(),
                    peer.getIPAddress());
                
                Label peerLabel = new Label(peerInfo);
                peerLabel.setForegroundColor(TextColor.ANSI.GREEN_BRIGHT);
                
                // Make it clickable
                final int index = i;
                Button selectButton = new Button("Select", () -> selectPeer(peers.get(index)));
                
                Panel peerPanel = new Panel();
                peerPanel.setLayoutManager(new LinearLayout(Direction.HORIZONTAL));
                peerPanel.addComponent(peerLabel);
                peerPanel.addComponent(new EmptySpace(new TerminalSize(2, 1)));
                peerPanel.addComponent(selectButton);
                
                peerListPanel.addComponent(peerPanel);
            }
        }
        
        updateStatus("Peers: " + peers.size());
    }
    
    private void selectPeer(Peer peer) {
        updateStatus("Selected: " + peer.getPeerName());
        // Show file browser to send file to this peer
        showFileBrowserForPeer(peer);
    }
    
    private void showFileBrowser() {
        if (listener == null || listener.peerManager == null) {
            showError("Peer manager not initialized");
            return;
        }
        
        List<Peer> peers = listener.peerManager.getAlivePeers();
        if (peers.isEmpty()) {
            showError("No peers available. Wait for discovery...");
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
        }
    }
    
    private void sendFileToPeer(Peer peer, File file) {
        updateStatus("Connecting to " + peer.getPeerName() + "...");
        
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
                
                gui.getGUIThread().invokeLater(() -> {
                    updateStatus("File request sent to " + peer.getPeerName());
                });
                
            } catch (Exception e) {
                gui.getGUIThread().invokeLater(() -> {
                    showError("Failed to connect: " + e.getMessage());
                    updateStatus("Ready");
                });
            }
        }).start();
    }
    
    private void startRefreshThread() {
        Thread refreshThread = new Thread(() -> {
            while (running.get()) {
                try {
                    Thread.sleep(5000); // Refresh every 5 seconds
                    gui.getGUIThread().invokeLater(this::refreshPeerList);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        refreshThread.setDaemon(true);
        refreshThread.start();
    }
    
    private void updateStatus(String message) {
        statusLabel.setText("Status: " + message);
    }
    
    private void showError(String message) {
        MessageDialog.showMessageDialog(gui, "Error", message, MessageDialogButton.OK);
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
