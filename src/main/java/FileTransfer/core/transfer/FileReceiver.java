package FileTransfer.core.transfer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileReceiver {
    private static final int BUFFER_SIZE = 8192; // 8KB chunks
    
    private final File outputFile;
    private final InputStream inputStream;
    private final long expectedBytes;
    private long receivedBytes;
    
    public FileReceiver(File outputFile, InputStream inputStream, long expectedBytes) {
        this.outputFile = outputFile;
        this.inputStream = inputStream;
        this.expectedBytes = expectedBytes;
        this.receivedBytes = 0;
    }
    
    /**
     * Receive the file from the input stream
     * @return checksum (MD5) of the received file
     * @throws IOException if any I/O error occurs
     */
    public String receiveFile() throws IOException {
        // Create parent directories if they don't exist
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            MessageDigest md = MessageDigest.getInstance("MD5");
            
            long remaining = expectedBytes;
            
            while (remaining > 0) {
                int toRead = (int) Math.min(BUFFER_SIZE, remaining);
                int bytesRead = inputStream.read(buffer, 0, toRead);
                
                if (bytesRead == -1) {
                    throw new IOException("Unexpected end of stream. Expected " + 
                        expectedBytes + " bytes, received " + receivedBytes + " bytes");
                }
                
                fos.write(buffer, 0, bytesRead);
                md.update(buffer, 0, bytesRead);
                receivedBytes += bytesRead;
                remaining -= bytesRead;
                
                // Print progress
                printProgress();
            }
            
            fos.flush();
            
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
        if (expectedBytes == 0) return 100.0;
        return (receivedBytes * 100.0) / expectedBytes;
    }
    
    private void printProgress() {
        if (expectedBytes > 0) {
            double progress = getProgress();
            // System.out.printf("\rReceiving: %.2f%% (%d/%d bytes)", progress, receivedBytes, expectedBytes);
            
            if (receivedBytes >= expectedBytes) {
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
    
    /**
     * Verify checksum matches expected
     */
    public static boolean verifyChecksum(String calculated, String expected) {
        return calculated != null && calculated.equalsIgnoreCase(expected);
    }
    
    public long getExpectedBytes() {
        return expectedBytes;
    }
    
    public long getReceivedBytes() {
        return receivedBytes;
    }
}
