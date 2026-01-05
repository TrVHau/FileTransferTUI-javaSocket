package FileTransfer.core.transfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileSender {
    private static final int BUFFER_SIZE = 8192; // 8KB chunks
    
    private final File file;
    private final OutputStream outputStream;
    private long totalBytes;
    private long sentBytes;
    
    public FileSender(File file, OutputStream outputStream) {
        this.file = file;
        this.outputStream = outputStream;
        this.totalBytes = file.length();
        this.sentBytes = 0;
    }
    
    /**
     * Send the file through the output stream
     * @return checksum (MD5) of the sent file
     * @throws IOException if any I/O error occurs
     */
    public String sendFile() throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            MessageDigest md = MessageDigest.getInstance("MD5");
            
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                md.update(buffer, 0, bytesRead);
                sentBytes += bytesRead;
                
                // Print progress
                printProgress();
            }
            
            outputStream.flush();
            
            // Calculate checksum
            byte[] digest = md.digest();
            return bytesToHex(digest);
            
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("MD5 algorithm not available", e);
        }
    }
    
    /**
     * Get progress percentage
     */
    public double getProgress() {
        if (totalBytes == 0) return 100.0;
        return (sentBytes * 100.0) / totalBytes;
    }
    
    private void printProgress() {
        if (totalBytes > 0) {
            double progress = getProgress();
            // System.out.printf("\rSending: %.2f%% (%d/%d bytes)", progress, sentBytes, totalBytes);
            
            if (sentBytes >= totalBytes) {
                // System.out.println(); // New line when complete
            }
        }
    }
    
    /**
     * Convert byte array to hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    public long getTotalBytes() {
        return totalBytes;
    }
    
    public long getSentBytes() {
        return sentBytes;
    }
}
