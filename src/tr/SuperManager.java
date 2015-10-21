package tr;

import tr.broadcast.BroadcastManager;
import tr.broadcast.Message;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by ks.kochetov on 20.10.2015.
 */
public class SuperManager {
    BroadcastManager bManager;
    StateMachine mStateMachine;
    TCPHandler tcpHandler;


    public SuperManager(StateMachine sm) throws SocketException, UnknownHostException {
        mStateMachine = sm;
        bManager = new BroadcastManager(sm);
        ConcurrentLinkedQueue<Message> tcpRQueue = new ConcurrentLinkedQueue<>();
        tcpHandler = new TCPHandler(tcpRQueue);
        tcpHandler.run();
    }


    public void onMessageReceive(Message msg) {
        Data dataToSend = msg.data.update();
        sendDataToSuccessor(dataToSend);
    }

    private void sendDataToSuccessor(Data dataToSend) {
        
    }








    class TCPHandler implements Runnable {
        ConcurrentLinkedQueue<Message> rQueue;

        public TCPHandler(ConcurrentLinkedQueue<Message> rQueue) {
            this.rQueue = rQueue;
        }

        @Override
        public void run() {
            while (true) {
                if (!rQueue.isEmpty()) {
                    onReceive(rQueue.poll());
                } else {
                    try {
                        rQueue.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private void onReceive(Message message) {
            onMessageReceive(message);
        }
    }
}
