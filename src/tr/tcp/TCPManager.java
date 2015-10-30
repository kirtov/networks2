package tr.tcp;

import tr.broadcast.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by ss.menshov on 20.10.2015.
 */
public class TCPManager {
    final ConcurrentLinkedQueue<Message> sQueue;
    final ConcurrentLinkedQueue<Message> rQueue;
    TCPReceiver tcpReceiver;
    TCPSender tcpSender;

    public TCPManager(ConcurrentLinkedQueue<Message> rQueue, int port) {
        this.sQueue = new ConcurrentLinkedQueue<>();
        this.rQueue = rQueue;
        tcpReceiver = new TCPReceiver(rQueue, port);
        tcpSender = new TCPSender(sQueue, rQueue, port);
    }

    public void startListening() {
        tcpReceiver.start();
        tcpSender.start();
    }

    public void sendMessage(Message message) {
        if (sQueue.size() == 0) {
            sQueue.add(message);
            synchronized (sQueue) {
                sQueue.notify();
            }
        } else {
            sQueue.add(message);
        }
    }
}



