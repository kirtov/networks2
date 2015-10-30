package tr.broadcast;

import tr.Data;
import tr.StateMachine;
import tr.utils.BroadcastResult;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by ks.kochetov on 16.10.2015.
 */
public class BroadcastManager {
    public static InetAddress NULL_ADDRESS;
    final ConcurrentLinkedQueue<Message> sQueue;
    final ConcurrentLinkedQueue<Message> rQueue;
    ArrayList<InetAddress> ssBuffer, ss2Buffer, ctBuffer;
    BroadcastReceiver brdReceiver;
    BroadcastSender brdSender;
    StateMachine mStateMachine;
    Timer mTimer;
    //если приходит SS1, то сохраняем адрес лидера, который вызвал этот процесс, чтобы ответить на этот SS1 только один раз
    InetAddress ssInitiatorAddrs;
    ConcurrentLinkedQueue eventQueue;
    boolean claimTokenMode = false;
    boolean ss2Mode = false;
    boolean ssMode = false;

    public BroadcastManager(ConcurrentLinkedQueue eventQueue, StateMachine stateMachine) throws SocketException, UnknownHostException {
        NULL_ADDRESS = InetAddress.getByAddress(new byte[]{0, 0, 0, 0});
        this.mStateMachine = stateMachine;
        this.eventQueue = eventQueue;
        sQueue = new ConcurrentLinkedQueue<>();
        rQueue = new ConcurrentLinkedQueue<>();
        brdReceiver = new BroadcastReceiver(stateMachine.broadcastPort, rQueue);
        brdSender = new BroadcastSender(stateMachine.broadcastPort, sQueue);
        mTimer = new Timer();
        ssInitiatorAddrs = mStateMachine.myAddrs;
    }

    public void sendBroadcast(Message brd) {
        synchronized (sQueue) {
            sQueue.add(brd);
            sQueue.notify();
        }
    }

    public void startBroadcasting() {
        brdReceiver.start();
        brdSender.start();
        BroadcastHandler handler = new BroadcastHandler();
        handler.start();
    }

    public void sendClaimToken(BroadcastResult bResult) {
        Message brdToSend = new Message(NULL_ADDRESS, mStateMachine.myAddrs, FrameControlByte.CT, new Data(""));
        ctBuffer = new ArrayList<>();
        sendBroadcast(brdToSend);
        claimTokenMode = true;
        ClaimTokenHandleTask ctTask = new ClaimTokenHandleTask(bResult);
        mTimer.schedule(ctTask, mStateMachine.broadcastWaitingTime);
    }

    public void sendSS() {
        Message brdToSend = new Message(mStateMachine.successorAddrs, mStateMachine.myAddrs, FrameControlByte.SS, new Data(""));
        sendBroadcast(brdToSend);
    }

    public void sendSS2ByLeader(BroadcastResult bResult) {
        Message brdToSend = new Message(mStateMachine.myAddrs, mStateMachine.myAddrs, FrameControlByte.SS2, new Data(""));
        ss2Buffer = new ArrayList<>();
        sendBroadcast(brdToSend);
        ss2Mode = true;
        SS2HandleTask ss2ht = new SS2HandleTask(bResult);
        mTimer.schedule(ss2ht, mStateMachine.broadcastWaitingTime);
    }

    public void sendSSByLeader(BroadcastResult bResult) {
        Message brdToSend = new Message(mStateMachine.successorAddrs, mStateMachine.myAddrs, FrameControlByte.SS, new Data(""));
        ssBuffer = new ArrayList<>();
        sendBroadcast(brdToSend);
        ssMode = true;
        SSHandleTask ssht = new SSHandleTask(bResult);
        mTimer.schedule(ssht, mStateMachine.broadcastWaitingTime);
    }

    public void sendSS2(InetAddress da) {
        Message brdToSend = new Message(da, mStateMachine.myAddrs, FrameControlByte.SS2, new Data(""));
        sendBroadcast(brdToSend);
    }

    private void onClaimTokenReceive(Message brd) {
        if (!claimTokenMode) {
            eventQueue.add(brd);
            synchronized (eventQueue) {
                eventQueue.notify();
            }
        } else {
            ctBuffer.add(brd.sa);
        }
    }

    private void onSolicitSuccessorReceive(Message brd) {
        ArrayList<InetAddress> adrs = new ArrayList<>();
        adrs.add(brd.da);
        adrs.add(brd.sa);
        adrs.sort(new InetAddrsComparator());
        if (adrs.get(0).equals(brd.da)) {
            if (!brd.sa.equals(mStateMachine.myAddrs) && !brd.da.equals(mStateMachine.myAddrs)) {
                if (between(brd.sa, brd.da)) {
                    sendSS();
                }
            } else if (ssMode && mStateMachine.imLeader && !brd.sa.equals(mStateMachine.myAddrs)) {
                ssBuffer.add(brd.sa);
            }
        }
    }

    private boolean between(InetAddress sa, InetAddress da) {
        ArrayList<InetAddress> ls = new ArrayList<>();
        ls.add(sa);
        ls.add(da);
        ls.add(mStateMachine.myAddrs);
        ls.sort(new InetAddrsComparator());
        int myI = 0, saI = 0, daI = 0;
        for (int i = 0; i < ls.size(); i++) {
            if (ls.get(i).equals(sa)) {
                saI = i;
            } else if (ls.get(i).equals(da)) {
                daI = i;
            } else if (ls.get(i).equals(mStateMachine.myAddrs)) {
                myI = i;
            }
        }

        // D<--I--S
        if (saI == 2 && daI == 0) {
            return true;
        }

        //  -I--S....D<-
        if (saI == 1 && daI == 2) {
            return true;
        }

        //  --S....D<--I-
        if (saI == 0 && daI == 1) {
            return true;
        }
        return false;
    }

    private void onSolicitSuccessor2Receive(Message brd) {
        if (!brd.sa.equals(mStateMachine.myAddrs) && brd.sa.equals(brd.da)) {
            sendSS2(brd.sa);
        }
        if (mStateMachine.imLeader && ss2Mode && !brd.sa.equals(mStateMachine.myAddrs)) {
            ss2Buffer.add(brd.sa);
        }
    }

    private void handleReceivedClaimTokens(BroadcastResult bResult) {
        claimTokenMode = false;
        bResult.onResult(ctBuffer);
    }

    private void handleReceivedSS2(BroadcastResult bRes) {
        ss2Mode = false;
        bRes.onResult(ss2Buffer);
    }

    private void handleReceivedSS(BroadcastResult bRes) {
        ssMode = false;
        bRes.onResult(ssBuffer);
    }

    class BroadcastHandler extends Thread {
        @Override
        public void run() {
            while (true) {
                if (!rQueue.isEmpty()) {
                    handleBroadcast(rQueue.poll());
                } else {
                    try {
                        synchronized (rQueue) {
                            rQueue.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private void handleBroadcast(Message brd) {
            mStateMachine.lastBroadcast = System.currentTimeMillis();
            switch (brd.eFc) {
                case CT:
                    onClaimTokenReceive(brd);
                    break;
                case SS:
                    onSolicitSuccessorReceive(brd);
                    break;
                case SS2:
                    onSolicitSuccessor2Receive(brd);
                    break;
            }
        }
    }

    class ClaimTokenHandleTask extends TimerTask {
        BroadcastResult bResult;

        public ClaimTokenHandleTask(BroadcastResult bResult) {
            this.bResult = bResult;
        }

        @Override
        public void run() {
            handleReceivedClaimTokens(bResult);
        }
    }

    class SS2HandleTask extends TimerTask {
        BroadcastResult bRes;

        public SS2HandleTask(BroadcastResult bRes) {
            this.bRes = bRes;
        }

        @Override
        public void run() {
            handleReceivedSS2(bRes);
        }
    }

    class SSHandleTask extends TimerTask {
        BroadcastResult bRes;

        public SSHandleTask(BroadcastResult bRes) {
            this.bRes = bRes;
        }

        @Override
        public void run() {
            handleReceivedSS(bRes);
        }
    }
}