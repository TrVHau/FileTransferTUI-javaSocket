package FileTransfer.core.transfer;

/**
 * Callback interface for tracking file transfer progress and events.
 */
public interface TransferCallback {
    /**
     * Called when transfer progress updates
     * @param bytesTransferred bytes transferred so far
     * @param totalBytes total bytes to transfer
     * @param fileName name of the file being transferred
     */
    void onProgress(long bytesTransferred, long totalBytes, String fileName);
    
    /**
     * Called when transfer starts
     * @param fileName name of the file
     * @param totalBytes total size in bytes
     * @param isSending true if sending, false if receiving
     */
    void onTransferStart(String fileName, long totalBytes, boolean isSending);
    
    /**
     * Called when transfer completes successfully
     * @param fileName name of the file
     * @param checksum MD5 checksum of the file
     */
    void onTransferComplete(String fileName, String checksum);
    
    /**
     * Called when transfer fails or is cancelled
     * @param fileName name of the file
     * @param error error message
     */
    void onTransferError(String fileName, String error);
    
    /**
     * Called when a file transfer request is received
     * @param peerName name of the peer requesting to send
     * @param peerIP IP address of the peer
     * @param fileName name of the file
     * @param fileSize size of the file in bytes
     * @return true to accept, false to reject
     */
    boolean onTransferRequest(String peerName, String peerIP, String fileName, long fileSize);
}
