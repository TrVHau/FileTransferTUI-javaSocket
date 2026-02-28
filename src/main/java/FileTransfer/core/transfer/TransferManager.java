package FileTransfer.core.transfer;

import FileTransfer.core.util.FormatUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages all file transfers (current and history).
 */
public class TransferManager {
    private static final int MAX_HISTORY = 50;
    
    private final List<TransferInfo> transfers = new CopyOnWriteArrayList<>();
    private volatile TransferInfo currentTransfer;
    private final List<TransferManagerListener> listeners = new CopyOnWriteArrayList<>();
    
    /**
     * Interface for listening to transfer manager events
     */
    public interface TransferManagerListener {
        void onTransferListChanged();
        void onActiveTransferProgress();
    }
    
    public void addListener(TransferManagerListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(TransferManagerListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Start tracking a new transfer
     */
    public TransferInfo startTransfer(String fileName, long fileSize, String peerName, 
                                       String peerIP, TransferInfo.Direction direction) {
        TransferInfo info = new TransferInfo(fileName, fileSize, peerName, peerIP, direction);
        transfers.add(0, info); // Add to front
        currentTransfer = info;
        
        // Trim history if needed
        while (transfers.size() > MAX_HISTORY) {
            transfers.remove(transfers.size() - 1);
        }
        
        notifyListeners();
        return info;
    }
    
    /**
     * Update progress of current transfer
     */
    public void updateProgress(long bytesTransferred) {
        if (currentTransfer != null) {
            currentTransfer.updateProgress(bytesTransferred);
            notifyProgressListeners();
        }
    }
    
    /**
     * Complete current transfer
     */
    public void completeTransfer(String checksum) {
        if (currentTransfer != null) {
            currentTransfer.complete(checksum);
            currentTransfer = null;
            notifyListeners();
        }
    }
    
    /**
     * Fail current transfer
     */
    public void failTransfer(String error) {
        if (currentTransfer != null) {
            currentTransfer.fail(error);
            currentTransfer = null;
            notifyListeners();
        }
    }
    
    /**
     * Cancel current transfer
     */
    public void cancelTransfer() {
        if (currentTransfer != null) {
            currentTransfer.cancel();
            currentTransfer = null;
            notifyListeners();
        }
    }
    
    /**
     * Get current active transfer
     */
    public TransferInfo getCurrentTransfer() {
        return currentTransfer;
    }
    
    /**
     * Check if there's an active transfer
     */
    public boolean hasActiveTransfer() {
        return currentTransfer != null && currentTransfer.getStatus() == TransferInfo.Status.IN_PROGRESS;
    }
    
    /**
     * Get all transfers (newest first)
     */
    public List<TransferInfo> getAllTransfers() {
        return Collections.unmodifiableList(transfers);
    }
    
    /**
     * Get recent transfers (limited count)
     */
    public List<TransferInfo> getRecentTransfers(int count) {
        int size = Math.min(count, transfers.size());
        return new ArrayList<>(transfers.subList(0, size));
    }
    
    /**
     * Get transfer statistics
     */
    public TransferStats getStats() {
        long totalSent = 0;
        long totalReceived = 0;
        int completedCount = 0;
        int failedCount = 0;
        
        for (TransferInfo info : transfers) {
            if (info.getStatus() == TransferInfo.Status.COMPLETED) {
                completedCount++;
                if (info.getDirection() == TransferInfo.Direction.SENDING) {
                    totalSent += info.getFileSize();
                } else {
                    totalReceived += info.getFileSize();
                }
            } else if (info.getStatus() == TransferInfo.Status.FAILED) {
                failedCount++;
            }
        }
        
        return new TransferStats(totalSent, totalReceived, completedCount, failedCount);
    }
    
    /**
     * Clear transfer history
     */
    public void clearHistory() {
        transfers.clear();
        if (currentTransfer != null) {
            transfers.add(currentTransfer);
        }
        notifyListeners();
    }
    
    private void notifyListeners() {
        for (TransferManagerListener listener : listeners) {
            try {
                listener.onTransferListChanged();
            } catch (Exception e) {
                // Ignore listener errors
            }
        }
    }
    
    private void notifyProgressListeners() {
        for (TransferManagerListener listener : listeners) {
            try {
                listener.onActiveTransferProgress();
            } catch (Exception e) {
                // Ignore listener errors
            }
        }
    }
    
    /**
     * Transfer statistics
     */
    public static class TransferStats {
        public final long totalBytesSent;
        public final long totalBytesReceived;
        public final int completedTransfers;
        public final int failedTransfers;
        
        public TransferStats(long sent, long received, int completed, int failed) {
            this.totalBytesSent = sent;
            this.totalBytesReceived = received;
            this.completedTransfers = completed;
            this.failedTransfers = failed;
        }
        
        public String getFormattedSent() {
            return FormatUtils.formatFileSize(totalBytesSent);
        }

        public String getFormattedReceived() {
            return FormatUtils.formatFileSize(totalBytesReceived);
        }
    }
}
