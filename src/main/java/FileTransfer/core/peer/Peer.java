package FileTransfer.core.peer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import FileTransfer.core.protocol.Protocol;

/**
 * Represents a discovered peer on the network.
 */
public class Peer {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final String peerID;
    private String peerName;
    private String ipAddress;
    private int tcpPort;
    private LocalDateTime lastSeen;

    public Peer(String peerID, String peerName, String ipAddress, int tcpPort) {
        this.peerID = peerID;
        this.peerName = peerName;
        this.ipAddress = ipAddress;
        this.tcpPort = tcpPort;
        this.lastSeen = LocalDateTime.now();
    }

    public String getPeerID()   { return peerID; }
    public String getPeerName() { return peerName; }
    public String getIPAddress(){ return ipAddress; }
    public int getTcpPort()     { return tcpPort; }
    public LocalDateTime getLastSeen() { return lastSeen; }

    /** Update mutable fields when a new discovery packet arrives. */
    public void update(String peerName, String ipAddress, int tcpPort) {
        this.peerName = peerName;
        this.ipAddress = ipAddress;
        this.tcpPort = tcpPort;
        this.lastSeen = LocalDateTime.now();
    }

    public long getSecondsSinceLastSeen() {
        return Duration.between(lastSeen, LocalDateTime.now()).getSeconds();
    }

    public String getFormattedLastSeen() {
        return lastSeen.format(TIME_FORMAT);
    }

    @Override
    public String toString() {
        return String.format("%s (%s:%d)", peerName, ipAddress, tcpPort);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Peer other = (Peer) obj;
        return peerID != null && peerID.equals(other.peerID);
    }

    @Override
    public int hashCode() {
        return peerID != null ? peerID.hashCode() : 0;
    }
}
