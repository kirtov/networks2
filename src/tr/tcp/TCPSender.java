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
public class TCPSender implements Runnable {
    ServerSocket serverSocket;
    final ConcurrentLinkedQueue<Message> sQueue;
    int portToSend;

    public TCPSender(ConcurrentLinkedQueue<Message> sQueue, int portToSend) {
        this.sQueue = sQueue;
        this.portToSend = portToSend;
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
                    sQueue.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                Message message = sQueue.poll();
                try {
                    //TODO Maybe open coonection with target
                    serverSocket.bind(new InetSocketAddress(message.getDestinationAddress(), portToSend));
                    Socket socket = serverSocket.accept();
                    OutputStream out = socket.getOutputStream();
                    out.write(message.getBytes());
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
