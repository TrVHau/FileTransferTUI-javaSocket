package FileTransfer.core.peer;

import java.time.LocalDateTime;

public class Peer {
    private String PeerID;
    private String PeerName;
    private String ipAddress;
    private LocalDateTime lastSeen;
    public Peer(String peerID, String peerName, String ipAddress) {
        PeerID = peerID;
        PeerName = peerName;
        this.ipAddress = ipAddress;
        this.lastSeen = LocalDateTime.now();
    }
    public String getPeerID() {
        return PeerID;  
    }
    public String getPeerName() {
        return PeerName;
    }
    public String getIPAddress() {
        return ipAddress;
    }
    public LocalDateTime getLastSeen() {
        return lastSeen;
    }
    public void setIPAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }public void setPeerID(String peerID) {
        PeerID = peerID;
    }public void setPeerName(String peerName) {
        PeerName = peerName;
    }
}
