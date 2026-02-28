package FileTransfer.core.network;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import FileTransfer.core.protocol.Protocol;
import FileTransfer.core.transfer.TransferCallback;

public class TcpServer implements Runnable {

    private final ExecutorService threadPool;
    private volatile boolean running = true;
    private ServerSocket serverSocket;
    private TransferCallback callback;
    private String downloadPath = System.getProperty("user.home") + "/Downloads/FileTransfer";
    private String localPeerId = "unknown";
    private String localPeerName = "unknown";
    
    public TcpServer(int maxConnections) {
        this.threadPool = Executors.newFixedThreadPool(maxConnections);
    }
    
    public void setCallback(TransferCallback callback) {
        this.callback = callback;
    }
    
    public void setDownloadPath(String path) {
        this.downloadPath = path;
    }
    
    public void setLocalIdentity(String peerId, String peerName) {
        this.localPeerId = peerId;
        this.localPeerName = peerName;
    }
    
    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(Protocol.TCP_PORT);
            serverSocket.setSoTimeout(200); // 200ms timeout for faster shutdown

            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    PeerConnection connection = new PeerConnection(clientSocket, callback, localPeerId, localPeerName);
                    connection.setDownloadPath(downloadPath);
                    threadPool.submit(connection);
                } catch (java.net.SocketTimeoutException e) {
                    // Timeout, continue loop to check running flag
                    continue;
                }
            }
            
        } catch (Exception e) {
            if (running && !Thread.currentThread().isInterrupted()) {
                // Error handling
            }
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (Exception e) {}
            }
        }
    }
    
    public void stop() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (Exception e) {}
        }
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdownNow();
        }
    }
}
