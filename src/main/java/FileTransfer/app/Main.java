package FileTransfer.app;

import FileTransfer.core.discovery.UdpBroadcaster;
import FileTransfer.core.discovery.UdpListener;
import FileTransfer.core.network.TcpServer;
import FileTransfer.ui.TUIManager;

public class Main {
    private static UdpBroadcaster broadcaster;
    private static UdpListener listener;
    private static TcpServer tcpServer;
    private static TUIManager tuiManager;

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(Main::shutdown));

        try {
            // Start UDP listener
            listener = new UdpListener();
            Thread listenerThread = new Thread(listener, "UDP-Listener");
            listenerThread.setDaemon(true);
            listenerThread.start();

            // Start UDP broadcaster
            broadcaster = new UdpBroadcaster();
            Thread broadcasterThread = new Thread(broadcaster, "UDP-Broadcaster");
            broadcasterThread.setDaemon(true);
            broadcasterThread.start();

            // Start TCP server
            tcpServer = new TcpServer(10);
            Thread tcpThread = new Thread(tcpServer, "TCP-Server");
            tcpThread.setDaemon(true);
            tcpThread.start();

            // Wait for network services to bind
            Thread.sleep(300);

            // Create TUI with all services ready
            tuiManager = new TUIManager(broadcaster, listener, tcpServer);

            // Wire TCP server callbacks and identity
            tcpServer.setCallback(tuiManager.getTransferCallback());
            tcpServer.setDownloadPath(tuiManager.getDownloadPath());
            tcpServer.setLocalIdentity(broadcaster.getPeerID(), System.getProperty("user.name"));

            // Start TUI (blocking)
            tuiManager.start();

        } catch (Exception e) {
            System.err.println("Fatal: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private static void shutdown() {
        if (tuiManager != null) tuiManager.shutdown();
        if (broadcaster != null) broadcaster.stop();
        if (listener != null) listener.stop();
        if (tcpServer != null) tcpServer.stop();
    }
}
