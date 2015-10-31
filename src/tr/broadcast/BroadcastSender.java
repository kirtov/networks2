package tr.broadcast;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by ks.kochetov on 16.10.2015.
 */
public class BroadcastSender extends Thread {
    int port;
    InetAddress addrs;
    private DatagramSocket socket;
    private ConcurrentLinkedQueue<Message> queue;

    public BroadcastSender(int port, ConcurrentLinkedQueue<Message> q) throws SocketException {
        this.port = port;
        this.queue = q;
        socket = new DatagramSocket();
        NetworkInterface nw;
        nw = NetworkInterface.getByName("wlan0");
        addrs = nw.getInterfaceAddresses().get(0).getBroadcast();
    }

    public void sendBroadcast(Message brd) {
        byte[] messageBytes = brd.getBytes();
        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, addrs, port);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            if (!queue.isEmpty()) {
                Message brdtoSend = queue.poll();
//                System.out.println("SENDED BROADCAST " + brdtoSend.toString());
                sendBroadcast(brdtoSend);
            } else {
                try {
                    synchronized (queue) {
                        queue.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
