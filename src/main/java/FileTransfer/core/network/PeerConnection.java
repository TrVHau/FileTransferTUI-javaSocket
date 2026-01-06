package FileTransfer.core.network;

import FileTransfer.core.protocol.Protocol;
import FileTransfer.core.transfer.FileReceiver;
import FileTransfer.core.transfer.FileSender;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class PeerConnection implements Runnable {

    private final Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    
    // Session state according to protocol spec
    private enum State {
        IDLE,
        WAITING_FOR_RESPONSE,
        WAITING_FOR_START,
        SENDING,
        RECEIVING,
        COMPLETED,
        ERROR
    }
    
    private State currentState;
    @SuppressWarnings("unused")
    private String remotePeerId;
    @SuppressWarnings("unused")
    private String remoteDeviceName;
    private String pendingFilename;
    private long pendingFilesize;
    private File fileToSend; // Store file reference for sending
    
    public PeerConnection(Socket socket) {
        this.socket = socket;
        this.currentState = State.IDLE;
    }
    @Override
    public void run() {
        try {
            reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );
            writer = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream()), 
                true // auto-flush
            );

            // System.out.println("PeerConnection established with " + socket.getInetAddress().getHostAddress());

            // Send HELLO message
            sendHello();

            // Main message loop
            String line;
            while ((line = reader.readLine()) != null) {
                handleMessage(line);

                if (currentState == State.COMPLETED || currentState == State.ERROR) {
                    break;
                }
            }
            
        } catch (IOException e) {
            // System.err.println("PeerConnection error: " + e.getMessage());
            currentState = State.ERROR;
        } finally {
            cleanup();
        }
    }
    private void handleMessage(String message) {
        // System.out.println("Received: " + message.trim());
        
        try {
            MessageParser.ParsedMessage parsed = MessageParser.parse(message);
            
            switch (parsed.type) {
                case HELLO:
                    handleHello(parsed.fields);
                    break;
                    
                case SEND_REQUEST:
                    handleSendRequest(parsed.fields);
                    break;
                    
                case SEND_ACCEPT:
                    handleSendAccept();
                    break;
                    
                case SEND_REJECT:
                    handleSendReject();
                    break;
                    
                case START_SEND:
                    handleStartSend();
                    break;
                    
                case CANCEL:
                    handleCancel();
                    break;
                    
                case DONE:
                    handleDone();
                    break;
                    
                case ERROR:
                    handleError(parsed.fields);
                    break;
                    
                default:
                    // System.err.println("Unknown message type: " + parsed.type);
                    sendError("Unknown message type");
            }
        } catch (Exception e) {
            // System.err.println("Error parsing message: " + e.getMessage());
            sendError("Invalid message format");
        }
    }
    
    // ===== Message Handlers =====
    
    private void handleHello(String[] fields) {
        if (fields.length < 2) {
            sendError("Invalid HELLO format");
            return;
        }
        
        remotePeerId = fields[0];
        remoteDeviceName = fields[1];
        
        // System.out.println("HELLO from " + remoteDeviceName + " (" + remotePeerId + ")");
    }
    
    private void handleSendRequest(String[] fields) {
        if (fields.length < 2) {
            sendError("Invalid SEND_REQUEST format");
            return;
        }
        
        // Check if already busy
        if (currentState != State.IDLE) {
            // System.out.println("Busy, rejecting request");
            sendSendReject();
            return;
        }
        
        String filename = fields[0];
        long filesize = Long.parseLong(fields[1]);
        
        pendingFilename = filename;
        pendingFilesize = filesize;
        
        // System.out.println("\n=== File Transfer Request ===");
        // System.out.println("From: " + remoteDeviceName + " (" + socket.getInetAddress().getHostAddress() + ")");
        // System.out.println("File: " + filename);
        // System.out.println("Size: " + formatFileSize(filesize));
        // System.out.println("=============================");
        
        // For now, auto-accept
        boolean userAccepts = true;
        
        if (userAccepts) {
            sendSendAccept();
            currentState = State.WAITING_FOR_START;
        } else {
            sendSendReject();
            currentState = State.IDLE;
        }
    }
    
    private void handleSendAccept() {
        if (currentState != State.WAITING_FOR_RESPONSE) {
            // System.err.println("Unexpected SEND_ACCEPT in state: " + currentState);
            return;
        }
        
        // System.out.println("File transfer accepted by peer");
        
        // Send START_SEND then begin raw byte transfer
        sendStartSend();
        currentState = State.SENDING;
        
        // Send file data as raw bytes
        try {
            if (fileToSend == null || !fileToSend.exists()) {
                // System.err.println("File to send not found");
                sendCancel();
                currentState = State.ERROR;
                return;
            }
            
            FileSender sender = new FileSender(fileToSend, socket.getOutputStream());
            
            // System.out.println("Sending file: " + fileToSend.getName());
            @SuppressWarnings("unused")
            String checksum = sender.sendFile();
            
            // System.out.println("File sent successfully, checksum: " + checksum);
            
            // Wait for DONE from receiver
            // (will be handled in message loop)
            
        } catch (IOException e) {
            // System.err.println("Error sending file: " + e.getMessage());
            sendCancel();
            currentState = State.ERROR;
        }
    }
    
    private void handleSendReject() {
        // System.out.println("File transfer rejected by peer");
        currentState = State.IDLE;
        pendingFilename = null;
        pendingFilesize = 0;
    }
    
    private void handleStartSend() {
        if (currentState != State.WAITING_FOR_START) {
            // System.err.println("Unexpected START_SEND in state: " + currentState);
            return;
        }
        
        // System.out.println("Receiving file: " + pendingFilename);
        currentState = State.RECEIVING;
        
        try {
            // Receive raw byte stream
            File outputFile = new File("downloads/" + pendingFilename);
            FileReceiver receiver = new FileReceiver(
                outputFile, 
                socket.getInputStream(), 
                pendingFilesize
            );
            
            receiver.receiveFile();
            
            // Send DONE when complete
            sendDone();
            currentState = State.COMPLETED;
            
            // System.out.println("\nFile received successfully: " + outputFile.getAbsolutePath());
            
        } catch (IOException e) {
            // System.err.println("Error receiving file: " + e.getMessage());
            sendCancel();
            currentState = State.ERROR;
        }
    }
    
    private void handleCancel() {
        // System.out.println("Transfer cancelled by peer");
        currentState = State.IDLE;
        pendingFilename = null;
        pendingFilesize = 0;
    }
    
    private void handleDone() {
        // System.out.println("Transfer completed successfully");
        currentState = State.COMPLETED;
    }
    
    private void handleError(String[] fields) {
        @SuppressWarnings("unused")
        String errorMsg = fields.length > 0 ? fields[0] : "Unknown error";
        // System.err.println("Error from peer: " + errorMsg);
        currentState = State.ERROR;
    }
    
    // ===== Sender Methods =====
    
    private void sendHello() {
        try {
            String peerId = InetAddress.getLocalHost().getHostAddress();
            String deviceName = System.getProperty("user.name");
            writer.print(Protocol.buildHello(peerId, deviceName));
            writer.flush();
            // System.out.println("Sent HELLO");
        } catch (Exception e) {
            // System.err.println("Error sending HELLO: " + e.getMessage());
        }
    }
    
    public void sendFileRequest(File file) {
        if (currentState != State.IDLE) {
            // System.err.println("Cannot send file request in state: " + currentState);
            return;
        }
        
        if (!file.exists() || !file.isFile()) {
            // System.err.println("File does not exist: " + file.getPath());
            return;
        }
        
        fileToSend = file;
        pendingFilename = file.getName();
        pendingFilesize = file.length();
        
        writer.print(Protocol.buildSendRequest(file.getName(), file.length()));
        writer.flush();
        currentState = State.WAITING_FOR_RESPONSE;
        
        // System.out.println("Sent SEND_REQUEST for: " + file.getName());
    }
    
    private void sendSendAccept() {
        writer.print(Protocol.buildSendAccept());
        writer.flush();
        // System.out.println("Sent SEND_ACCEPT");
    }
    
    private void sendSendReject() {
        writer.print(Protocol.buildSendReject());
        writer.flush();
        // System.out.println("Sent SEND_REJECT");
    }
    
    private void sendStartSend() {
        writer.print(Protocol.buildStartSend());
        writer.flush();
        // System.out.println("Sent START_SEND");
    }
    
    public void sendCancel() {
        writer.print(Protocol.buildCancel());
        writer.flush();
        currentState = State.IDLE;
        // System.out.println("Sent CANCEL");
    }
    
    private void sendDone() {
        writer.print(Protocol.buildDone());
        writer.flush();
        // System.out.println("Sent DONE");
    }
    
    public void sendError(String errorMessage) {
        writer.print(Protocol.buildError(errorMessage));
        writer.flush();
        currentState = State.ERROR;
        // System.err.println("Sent ERROR: " + errorMessage);
    }
    
    // ===== Helper Methods =====
    
    @SuppressWarnings("unused")
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
     
    private void cleanup() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            // System.err.println("Error during cleanup: " + e.getMessage());
        }
        // System.out.println("PeerConnection closed with " + socket.getInetAddress().getHostAddress());
    }
}
