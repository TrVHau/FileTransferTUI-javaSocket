package FileTransfer.core.peer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import FileTransfer.core.protocol.Protocol;

public class PeerManager {
    private final Map<String, Peer> peers = new ConcurrentHashMap<>();

    /** Add a new peer or update an existing one from a discovery packet. */
    public void addOrUpdatePeer(String peerID, String peerName, String ipAddress, int tcpPort) {
        Peer peer = peers.get(peerID);
        if (peer == null) {
            peers.put(peerID, new Peer(peerID, peerName, ipAddress, tcpPort));
        } else {
            peer.update(peerName, ipAddress, tcpPort);
        }
    }

    /** Return peers whose last discovery was within the timeout window. */
    public List<Peer> getAlivePeers() {
        cleanUp();
        return new ArrayList<>(peers.values());
    }

    /** Remove peers that haven't been seen within the timeout window. */
    public void cleanUp() {
        LocalDateTime cutoff = LocalDateTime.now()
                .minusNanos(Protocol.DISCOVER_TIMEOUT * 1_000_000L);
        peers.values().removeIf(p -> p.getLastSeen().isBefore(cutoff));
    }
}