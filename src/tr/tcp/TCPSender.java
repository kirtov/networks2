package tr.tcp;

import tr.broadcast.ControlEventByte;
import tr.broadcast.Message;

import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by ss.menshov on 21.10.2015.
 */
public class TCPSender extends Thread {
    Socket socketToSend;
    final ConcurrentLinkedQueue<Message> sQueue;
    final ConcurrentLinkedQueue<Message> eventQueue;
    int portToSend;
    InetAddress curAddr;
    OutputStream out;

    public TCPSender(ConcurrentLinkedQueue<Message> sQueue, ConcurrentLinkedQueue<Message> eventQueue, int portToSend) {
        this.sQueue = sQueue;
        this.eventQueue = eventQueue;
        this.portToSend = portToSend;
        this.curAddr = null;
    }

    @Override
    public void run() {
        while (true) {
            if (sQueue.isEmpty()) {
                try {
                    synchronized (sQueue) {
                        sQueue.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                Message message = sQueue.poll();
                try {
                    if (curAddr == null || !curAddr.equals(message.getDestinationAddress()) || socketToSend.getChannel() == null) {
                        socketToSend = new Socket(message.getDestinationAddress(), portToSend);
                        curAddr = message.getDestinationAddress();
                        out = socketToSend.getOutputStream();
                    }
                    out.write(message.getBytes());
                    System.out.println("SENDED TCP " + message.toString() + " DATA = " + message.data.data);
                    message.eFc = ControlEventByte.TOKEN_SENDED;
                    eventQueue.add(message);
                    synchronized (eventQueue) {
                        eventQueue.notify();
                    }
                } catch (IOException e) {
                    message.eFc = ControlEventByte.TCP_EXC;
                    eventQueue.add(message);
                    synchronized (eventQueue) {
                        eventQueue.notify();
                    }
                }
            }
        }
    }
}
