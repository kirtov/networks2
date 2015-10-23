package tr;

import tr.broadcast.BroadcastManager;
import tr.broadcast.FrameControlByte;
import tr.broadcast.InetAddrsComparator;
import tr.broadcast.Message;
import tr.tcp.TCPManager;
import tr.utils.BroadcastResult;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by ks.kochetov on 20.10.2015.
 */
public class SuperManager {
    BroadcastManager bManager;
    TCPManager tcpManager;
    StateMachine mStateMachine;
    EventHandler tcpHandler;
    Timer mTimer;

    public SuperManager(StateMachine sm) throws SocketException, UnknownHostException {
        mStateMachine = sm;
        ConcurrentLinkedQueue<Message> eventQueue = new ConcurrentLinkedQueue<>();
        bManager = new BroadcastManager(eventQueue, sm);
        tcpManager = new TCPManager(eventQueue, sm.tcpPort);
        tcpHandler = new EventHandler(eventQueue);
    }

    public void init() {
        tcpHandler.run();
        tcpManager.startListening();
        bManager.startBroadcasting();
        mTimer = new Timer();
        mTimer.schedule(new NetworkActivityTimer(), mStateMachine.networkActivityTime, mStateMachine.networkActivityTime);
    }

    public void onTokenReceive(Message msg) {
        onBecomeLeader();
        Data dataToSend;
        if (!msg.dataIsNull()) {
            dataToSend = msg.data.update();
        } else {
            dataToSend = generateNewData();
        }
        Message messageToSend = new Message(null, mStateMachine.myAddrs, FrameControlByte.T, dataToSend);
        sendMessageToSuccessor(messageToSend);
    }

    private void sendMessageToSuccessor(Message messageToSend) {
        if (!mStateMachine.imLeader) return;
        if (mStateMachine.successorAddrs != null) {
            bManager.sendSSByLeader(new BroadcastResult() {
                @Override
                public void onResult(ArrayList<InetAddress> resultBuffer) {
                    InetAddress addressToSend = mStateMachine.successorAddrs;
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
                    messageToSend.setDestinationAddress(addressToSend);
                    if (mStateMachine.imLeader) {
                        tcpManager.sendMessage(messageToSend);
                    }
                    mStateMachine.imLeader = false;
                }
            });
        } else {
            bManager.sendSS2ByLeader(new BroadcastResult() {
                @Override
                public void onResult(ArrayList<InetAddress> resultBuffer) {
                    if (resultBuffer.size() == 0) {
                        //���� SS2, ���� ����-������ �� ������
                        sendMessageToSuccessor(messageToSend);
                    } else {
                        InetAddress addressToSend = null;
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
                        messageToSend.setDestinationAddress(addressToSend);
                        if (mStateMachine.imLeader) {
                            tcpManager.sendMessage(messageToSend);
                        }
                        mStateMachine.imLeader = false;
                    }
                }
            });
        }
    }

    /**
     * @param ctInitiator = null, ���� ��������� - ��
     */
    private void sendClaimToken(InetAddress ctInitiator) {
        mStateMachine.imLeader = false;
        bManager.sendClaimToken(new BroadcastResult() {
            @Override
            public void onResult(ArrayList<InetAddress> resultBuffer) {
                if (ctInitiator != null) {
                    resultBuffer.add(ctInitiator);
                }
                resultBuffer.sort(new InetAddrsComparator());
                if (mStateMachine.myAddrs == resultBuffer.get(resultBuffer.size() - 1)) {
                    onBecomeLeader();
                    sendMessageToSuccessor(new Message(null, mStateMachine.myAddrs, FrameControlByte.T, generateNewData()));
                }
            }
        });
    }

    private void onBecomeLeader() {
        mStateMachine.imLeader = true;
    }

    private Data generateNewData() {
        return new Data("").update();
    }

    class NetworkActivityTimer extends TimerTask {

        @Override
        public void run() {
            if (System.currentTimeMillis() - mStateMachine.lastBroadcast > mStateMachine.networkActivityTime && !mStateMachine.imLeader) {
                sendClaimToken(null);
            }
        }
    }

    class EventHandler implements Runnable {
        ConcurrentLinkedQueue<Message> rQueue;

        public EventHandler(ConcurrentLinkedQueue<Message> rQueue) {
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
            if (message.isToken()) {
                onTokenReceive(message);
            } else {
                sendClaimToken(message.getSourceAddress());
            }
        }
    }
}
