package tr;

import tr.broadcast.BroadcastManager;
import tr.broadcast.InetAddrsComparator;
import tr.broadcast.Message;
import tr.tcp.TCPManager;
import tr.utils.BroadcastResult;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by ks.kochetov on 20.10.2015.
 */
public class SuperManager {
    BroadcastManager bManager;
    TCPManager tcpManager;
    StateMachine mStateMachine;
    TCPHandler tcpHandler;

    public SuperManager(StateMachine sm) throws SocketException, UnknownHostException {
        mStateMachine = sm;
        bManager = new BroadcastManager(sm);
        ConcurrentLinkedQueue<Message> tcpRQueue = new ConcurrentLinkedQueue<>();
        tcpManager = new TCPManager(tcpRQueue, sm.tcpPort);
        tcpHandler = new TCPHandler(tcpRQueue);
        tcpHandler.run();
    }

    public void onMessageReceive(Message msg) {
        Data dataToSend = msg.data.update();
        sendDataToSuccessor(dataToSend);
    }

    private void sendDataToSuccessor(Data dataToSend) {
        if (mStateMachine.nextStation != null) {
            bManager.sendSSByLeader(new BroadcastResult<ArrayList<InetAddress>>() {
                @Override
                public void onResult(ArrayList<InetAddress> resultBuffer) {
                    InetAddress addressToSend;
                    if (resultBuffer.size() == 0) {
                        return;
                    }
                    resultBuffer.add(mStateMachine.myAddrs);
                    resultBuffer.sort(new InetAddrsComparator());
                    for (int i = 0; i < resultBuffer.size(); i++) {
                        if (resultBuffer.get(i).equals(mStateMachine.myAddrs)) {
                            if (i != 0) {
                                addressToSend = resultBuffer.get(i - 1);
                                break;
                            } else {
                                addressToSend = resultBuffer.get(resultBuffer.size() - 1);
                                break;
                            }
                        }
                    }
                    //TCPManager.sendData(dataToSend, addressToSend)
                }
            });
        } else {
                bManager.sendSS2ByLeader(new BroadcastResult<ArrayList<InetAddress>>() {
                    @Override
                    public void onResult(ArrayList<InetAddress> resultBuffer) {
                        if (resultBuffer.size() == 0) {
                            //шлем SS2, пока кого-нибудь не найдем
                            sendDataToSuccessor(dataToSend);
                        } else {
                            InetAddress addressToSend;
                            resultBuffer.add(mStateMachine.myAddrs);
                            resultBuffer.sort(new InetAddrsComparator());
                            for (int i = 0; i < resultBuffer.size(); i++) {
                                if (resultBuffer.get(i).equals(mStateMachine.myAddrs)) {
                                    if (i != 0) {
                                        addressToSend = resultBuffer.get(i - 1);
                                        break;
                                    } else {
                                        addressToSend = resultBuffer.get(resultBuffer.size() - 1);
                                        break;
                                    }
                                }
                            }
                            //TCPManager.sendData(dataToSend, addressToSend)
                        }
                    }
                });
            }
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
