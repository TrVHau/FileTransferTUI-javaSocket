package FileTransfer.core.discovery;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import FileTransfer.core.protocol.Protocol;

public class UdpBroadcaster implements Runnable {

    private volatile boolean running = true;
    private final String peerID;
    
    public UdpBroadcaster() {
        // Generate a unique peer ID combining hostname and a random suffix
        String id;
        try {
            id = InetAddress.getLocalHost().getHostName() + "-" + 
                 Long.toHexString(System.currentTimeMillis() & 0xFFFFFF);
        } catch (Exception e) {
            id = "peer-" + Long.toHexString(System.currentTimeMillis() & 0xFFFFFF);
        }
        this.peerID = id;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);

            String peerName = System.getProperty("user.name");

            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    String message = Protocol.buildDiscover(peerID, peerName, Protocol.TCP_PORT);
                    byte[] buffer = message.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    
                    // Send to general broadcast address
                    InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
                    DatagramPacket packet = new DatagramPacket(
                        buffer, buffer.length, broadcastAddress, Protocol.UDP_PORT);
                    socket.send(packet);
                    
                    // Also try to send to subnet-specific broadcast addresses
                    // for better compatibility across different network configurations
                    sendToSubnetBroadcasts(socket, buffer);
                    
                } catch (Exception e) {
                    // Ignore individual send failures, keep running
                }

                Thread.sleep(Protocol.DISCOVER_INTERVAL);
            }

        } catch (Exception e) {
            if (running) {
                // Log error if we're still supposed to be running
            }
        }
    }
    
    /**
     * Send broadcast to all subnet-specific broadcast addresses
     * for better LAN discovery
     */
    private void sendToSubnetBroadcasts(DatagramSocket socket, byte[] buffer) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                
                for (var addr : iface.getInterfaceAddresses()) {
                    InetAddress broadcast = addr.getBroadcast();
                    if (broadcast != null) {
                        try {
                            DatagramPacket packet = new DatagramPacket(
                                buffer, buffer.length, broadcast, Protocol.UDP_PORT);
                            socket.send(packet);
                        } catch (Exception e) {
                            // Ignore individual interface failures
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    public void stop() {
        running = false;
    }
    
    public String getPeerID() {
        return peerID;
    }
}