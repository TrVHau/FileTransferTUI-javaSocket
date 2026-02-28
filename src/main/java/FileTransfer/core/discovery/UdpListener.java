package FileTransfer.core.discovery;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import FileTransfer.core.network.MessageParser;
import FileTransfer.core.network.MessageParser.ParsedMessage;
import FileTransfer.core.peer.PeerManager;
import FileTransfer.core.protocol.MessageType;
import FileTransfer.core.protocol.Protocol;

public class UdpListener implements Runnable {
    private final PeerManager peerManager = new PeerManager();
    private volatile boolean running = true;
    private DatagramSocket socket;

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(Protocol.UDP_PORT);
            socket.setSoTimeout(200);

            byte[] buffer = new byte[2048];

            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    handlePacket(message, packet.getAddress());
                } catch (java.net.SocketTimeoutException e) {
                    // continue to check running flag
                }
            }
        } catch (Exception e) {
            // socket bind failure or similar – nothing to do
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    private void handlePacket(String rawMessage, InetAddress ipAddress) {
        try {
            ParsedMessage msg = MessageParser.parse(rawMessage);
            if (msg.type == MessageType.DISCOVER && msg.fields.length >= 3) {
                String peerID   = msg.fields[0];
                String peerName = msg.fields[1];
                int tcpPort;
                try {
                    tcpPort = Integer.parseInt(msg.fields[2]);
                } catch (NumberFormatException e) {
                    tcpPort = Protocol.TCP_PORT;
                }
                peerManager.addOrUpdatePeer(peerID, peerName, ipAddress.getHostAddress(), tcpPort);
            }
        } catch (Exception e) {
            // Ignore malformed messages
        }
    }

    public PeerManager getPeerManager() { return peerManager; }

    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
