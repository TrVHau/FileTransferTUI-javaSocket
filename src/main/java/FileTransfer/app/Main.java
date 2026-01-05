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
    private static Thread broadcasterThread;
    private static Thread listenerThread;
    private static Thread tcpThread;
    
    public static void main(String[] args) {
        // Shutdown hook for cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdown();
        }));
        
        try {
            // Start services silently
            listener = new UdpListener();
            listenerThread = new Thread(listener, "UDP-Listener");
            listenerThread.setDaemon(true);
            listenerThread.start();
            
            broadcaster = new UdpBroadcaster();
            broadcasterThread = new Thread(broadcaster, "UDP-Broadcaster");
            broadcasterThread.setDaemon(true);
            broadcasterThread.start();
            
            tcpServer = new TcpServer(10);
            tcpThread = new Thread(tcpServer, "TCP-Server");
            tcpThread.setDaemon(true);
            tcpThread.start();
            
            // Wait for services to start
            Thread.sleep(1000);
            
            // Start TUI
            tuiManager = new TUIManager(broadcaster, listener, tcpServer);
            tuiManager.start();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            shutdown();
        }
    }
    
    private static void shutdown() {
        if (tuiManager != null) {
            tuiManager.shutdown();
        }
        if (broadcaster != null) {
            broadcaster.stop();
        }
        if (listener != null) {
            listener.stop();
        }
        if (tcpServer != null) {
            tcpServer.stop();
        }
        
        // Wait for threads to finish with longer timeout
        try {
            if (broadcasterThread != null && broadcasterThread.isAlive()) {
                broadcasterThread.join(1000);
                if (broadcasterThread.isAlive()) {
                    broadcasterThread.interrupt();
                }
            }
            if (listenerThread != null && listenerThread.isAlive()) {
                listenerThread.join(1000);
                if (listenerThread.isAlive()) {
                    listenerThread.interrupt();
                }
            }
            if (tcpThread != null && tcpThread.isAlive()) {
                tcpThread.join(1000);
                if (tcpThread.isAlive()) {
                    tcpThread.interrupt();
                }
            }
        } catch (InterruptedException e) {
            // Ignore
        }
    }
}
