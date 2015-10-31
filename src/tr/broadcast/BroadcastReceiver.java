package tr.broadcast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by ks.kochetov on 16.10.2015.
 */
public class BroadcastReceiver extends Thread {
    int port;
    int packetLen;
    private DatagramSocket socket;
    private final ConcurrentLinkedQueue<Message> queue;

    public BroadcastReceiver(int port, ConcurrentLinkedQueue<Message> q) throws SocketException {
        this.queue = q;
        this.port = port;
        socket = new DatagramSocket(port);
        setPacketLen(512);
    }

    public void setPacketLen(int length) {
        this.packetLen = length;
    }

    @Override
    public void run() {
        while (true) {
            DatagramPacket packet = new DatagramPacket(new byte[packetLen], packetLen);
            try {
                socket.receive(packet);
                Message rMessage = new Message(packet.getData());
//                System.out.println("RECEIVED BROADCAST " + rMessage.toString());
                queue.add(rMessage);
                synchronized (queue) {
                    queue.notify();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
