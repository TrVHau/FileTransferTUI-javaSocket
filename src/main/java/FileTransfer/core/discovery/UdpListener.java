package FileTransfer.core.discovery;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import FileTransfer.core.network.*;
import FileTransfer.core.network.MessageParser.*;
import FileTransfer.core.peer.PeerManager;
import FileTransfer.core.protocol.MessageType;
import FileTransfer.core.protocol.Protocol;

public class UdpListener implements Runnable {
    public final PeerManager peerManager = new PeerManager();
    private volatile boolean running = true;
    private DatagramSocket socket;

    @Override
    public void run() {
        try{
            socket = new DatagramSocket(Protocol.UDP_PORT);
            socket.setSoTimeout(200); // 200ms timeout for faster shutdown
            // System.out.println("UDP Listener started on port " + Protocol.UDP_PORT);

            byte[] buffer = new byte[1024];

            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength());
                    handlePacket(message, packet.getAddress());
                } catch (java.net.SocketTimeoutException e) {
                    // Timeout, continue loop to check running flag
                    continue;
                }
            }
        }
        catch (Exception e) {
            if (running && !Thread.currentThread().isInterrupted()) {
                // System.out.println("UDP Listener encountered an error:"+ e.getMessage());
            }
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    private void handlePacket(String rawmessage, InetAddress ipAddress) {
        // Handle incoming UDP packet
        try {
            ParsedMessage msg = MessageParser.parse(rawmessage);
            if(msg.type == MessageType.DISCOVER){
                String peerID = msg.fields[0];
                String peerName = msg.fields[1];
                peerManager.addOrUpdatePeer(peerID, peerName, ipAddress.getHostAddress());

                // System.out.println("Discovered peer: " + peerName + " (" + peerID + ") at " + ipAddress.getHostAddress());
            }

        } catch (Exception e) {
            // ignore malformed messages
        }
    }
    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

}
