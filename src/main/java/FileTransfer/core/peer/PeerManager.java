package FileTransfer.core.peer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import FileTransfer.core.protocol.Protocol;

public class PeerManager {
    private Map<String, Peer> peers= new ConcurrentHashMap<>();

    // add or update peer
    public void addOrUpdatePeer(String peerID, String peerName, String ipAddress) {
        Peer peer = peers.get(peerID);

        if (peer == null) {
            peer = new Peer(peerID, peerName, ipAddress);
            peers.put(peerID, peer);
            // System.out.println("Added new peer: " + peerName + " (" + peerID + ") at " + ipAddress);
        } else {
            peer.setPeerName(peerName);
            peer.setIPAddress(ipAddress);
            peer.setLastSeen(java.time.LocalDateTime.now());
        }
    }

    // get alive peers
    public List<Peer> getAlivePeers() {
        List<Peer> alivePeers = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Peer peer : peers.values()) {
            if (isAlive(peer, now)) {
                alivePeers.add(peer);
            }
        }
        return alivePeers;
    }

    // delete timed-out peers
    public void deleteTimedOutPeers() {
        LocalDateTime now = LocalDateTime.now();
        peers.values().removeIf(peer -> !isAlive(peer, now));
    }   
private boolean isAlive(Peer peer, LocalDateTime now) {
    return peer.getLastSeen()
            .plusNanos(Protocol.DISCOVER_TIMEOUT * 1_000_000L)
            .isAfter(now);
}
}