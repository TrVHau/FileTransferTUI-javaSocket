package FileTransfer.core.network;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import FileTransfer.core.protocol.Protocol;

public class TcpServer implements Runnable {

    private final ExecutorService threadPool;
    private volatile boolean running = true;
    private ServerSocket serverSocket;
    
    public TcpServer(int maxConnections) {
        this.threadPool = Executors.newFixedThreadPool(maxConnections);
    }
    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(Protocol.TCP_PORT);
            serverSocket.setSoTimeout(200); // 200ms timeout for faster shutdown
            // System.out.println("TCP Server started on port " + Protocol.TCP_PORT);

            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    // System.out.println("Accepted connection from " + clientSocket.getInetAddress().getHostAddress());
                    PeerConnection connection = new PeerConnection(clientSocket);
                    threadPool.submit(connection);
                } catch (java.net.SocketTimeoutException e) {
                    // Timeout, continue loop to check running flag
                    continue;
                }
            }
            
        } catch (Exception e) {
            if (running && !Thread.currentThread().isInterrupted()) {
                // System.out.println("TCP Server encountered an error: " + e.getMessage());
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
        // System.out.println("TCP Server stopped");
    }
}
