package tr.tcp;

import tr.broadcast.Message;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by ss.menshov on 21.10.2015.
 */
public class TCPSender extends Thread {
    ServerSocket serverSocket;
    final ConcurrentLinkedQueue<Message> sQueue;
    int portToSend;
    InetAddress curAddr;

    public TCPSender(ConcurrentLinkedQueue<Message> sQueue, int portToSend) {
        this.sQueue = sQueue;
        this.portToSend = portToSend;
        this.curAddr = null;
        try {
            serverSocket = new ServerSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                    if (curAddr == null || curAddr.equals(message.getDestinationAddress())) {
                        serverSocket.bind(new InetSocketAddress(message.getDestinationAddress(), portToSend));
                        curAddr = message.getDestinationAddress();
                    }
                    Socket socket = serverSocket.accept();
                    OutputStream out = socket.getOutputStream();
                    System.out.println("SENDED TCP " + message.toString());
                    out.write(message.getBytes());
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
