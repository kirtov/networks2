package tr.tcp;

import tr.broadcast.Message;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by ss.menshov on 21.10.2015.
 */
public class TCPReceiver extends Thread {
    private static final int SIZE = 1024;
    final ConcurrentLinkedQueue<Message> rQueue;
    private ServerSocket serverSocket;
    private int portToReceive;

    public TCPReceiver(ConcurrentLinkedQueue<Message> rQueue, int portToReceive) {
        this.rQueue = rQueue;
        this.portToReceive = portToReceive;
        try {
            serverSocket = new ServerSocket(portToReceive);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        byte[] data = new byte[SIZE];
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                InputStream i = socket.getInputStream();
                i.read(data);
                Message rMessage = new Message(data);
                System.out.println("RECEIVED TCP " + rMessage.toString() + " DATA = " + rMessage.data.data);
                rQueue.add(new Message(data));
                synchronized (rQueue) {
                    rQueue.notify();
                }
                i.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
