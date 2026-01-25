package FileTransfer.core.discovery;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


import FileTransfer.core.protocol.Protocol;

public class UdpBroadcaster implements Runnable {

    public volatile boolean running = true;

    @Override
    public void run() {
        // Broadcasting logic to be implemented
        try (DatagramSocket socket = new DatagramSocket()){
            socket.setBroadcast(true);

            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");

            String peerID = InetAddress.getLocalHost().getHostAddress();
            String peerName = System.getProperty("user.name");

            while (running && !Thread.currentThread().isInterrupted()) {
                String message = Protocol.buildDiscover(peerID, peerName, Protocol.TCP_PORT);
                
                byte[] buffer = message.getBytes();
                
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, Protocol.UDP_PORT);

                socket.send(packet);

                // System.out.println("Broadcasted DISCOVER message: " + message.trim());

                Thread.sleep(Protocol.DISCOVER_INTERVAL);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
    }

}