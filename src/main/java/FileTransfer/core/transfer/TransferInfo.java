package FileTransfer.core.transfer;

import FileTransfer.core.util.FormatUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Information about a file transfer (completed or in progress).
 */
public class TransferInfo {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    public enum Status {
        IN_PROGRESS("↻"),
        COMPLETED("✓"),
        FAILED("✗"),
        CANCELLED("⊘");
        
        private final String symbol;
        Status(String symbol) { this.symbol = symbol; }
        public String getSymbol() { return symbol; }
    }
    
    public enum Direction {
        SENDING("↑"),
        RECEIVING("↓");
        
        private final String symbol;
        Direction(String symbol) { this.symbol = symbol; }
        public String getSymbol() { return symbol; }
    }
    
    private final String fileName;
    private final long fileSize;
    private final String peerName;
    private final String peerIP;
    private final Direction direction;
    private final LocalDateTime startTime;
    
    private Status status;
    private long bytesTransferred;
    private LocalDateTime endTime;
    private String errorMessage;
    private String checksum;
    
    public TransferInfo(String fileName, long fileSize, String peerName, String peerIP, Direction direction) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.peerName = peerName;
        this.peerIP = peerIP;
        this.direction = direction;
        this.startTime = LocalDateTime.now();
        this.status = Status.IN_PROGRESS;
        this.bytesTransferred = 0;
    }
    
    public void updateProgress(long bytesTransferred) {
        this.bytesTransferred = bytesTransferred;
    }
    
    public void complete(String checksum) {
        this.status = Status.COMPLETED;
        this.checksum = checksum;
        this.endTime = LocalDateTime.now();
        this.bytesTransferred = fileSize;
    }
    
    public void fail(String errorMessage) {
        this.status = Status.FAILED;
        this.errorMessage = errorMessage;
        this.endTime = LocalDateTime.now();
    }
    
    public void cancel() {
        this.status = Status.CANCELLED;
        this.endTime = LocalDateTime.now();
    }
    
    public double getProgressPercent() {
        if (fileSize == 0) return 100.0;
        return (bytesTransferred * 100.0) / fileSize;
    }
    
    public String getProgressBar(int width) {
        double percent = getProgressPercent();
        int filled = (int) (width * percent / 100);
        int empty = width - filled;
        
        StringBuilder bar = new StringBuilder();
        bar.append("│");
        for (int i = 0; i < filled; i++) bar.append("█");
        for (int i = 0; i < empty; i++) bar.append("░");
        bar.append("│");
        bar.append(String.format(" %5.1f%%", percent));
        
        return bar.toString();
    }
    
    public String getFormattedSize() {
        return FormatUtils.formatFileSize(fileSize);
    }
    
    public String getFormattedSpeed() {
        if (startTime == null || bytesTransferred == 0) return "0 B/s";
        
        long elapsedMs = java.time.Duration.between(startTime, 
            endTime != null ? endTime : LocalDateTime.now()).toMillis();
        
        if (elapsedMs == 0) return "∞";
        
        double bytesPerSecond = (bytesTransferred * 1000.0) / elapsedMs;
        return FormatUtils.formatFileSize((long) bytesPerSecond) + "/s";
    }
    
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(status.getSymbol()).append(" ");
        sb.append(direction.getSymbol()).append(" ");
        sb.append(FormatUtils.truncate(fileName, 20)).append(" ");
        sb.append("(").append(getFormattedSize()).append(") ");
        
        if (direction == Direction.SENDING) {
            sb.append("→ ").append(peerName);
        } else {
            sb.append("← ").append(peerName);
        }
        
        sb.append(" [").append(startTime.format(TIME_FORMAT)).append("]");
        
        return sb.toString();
    }
    

    
    // Getters
    public String getFileName() { return fileName; }
    public long getFileSize() { return fileSize; }
    public String getPeerName() { return peerName; }
    public String getPeerIP() { return peerIP; }
    public Direction getDirection() { return direction; }
    public Status getStatus() { return status; }
    public long getBytesTransferred() { return bytesTransferred; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public String getErrorMessage() { return errorMessage; }
    public String getChecksum() { return checksum; }
}
