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
    ArrayList<InetAddress> ssBuffer, ss2Buffer;
    BroadcastReceiver brdReceiver;
    BroadcastSender brdSender;
    StateMachine mStateMachine;
    Timer mTimer;
    //если приходит SS1, то сохраняем адрес лидера, который вызвал этот процесс, чтобы ответить на этот SS1 только один раз
    InetAddress ssSa;

    public BroadcastManager(StateMachine stateMachine) throws SocketException, UnknownHostException {
        NULL_ADDRESS = InetAddress.getByAddress(new byte[]{0,0,0,0});
        this.mStateMachine = stateMachine;
        sQueue = new ConcurrentLinkedQueue<>();
        rQueue = new ConcurrentLinkedQueue<>();
        brdReceiver = new BroadcastReceiver(stateMachine.broadcastPort, rQueue);
        brdSender = new BroadcastSender(stateMachine.broadcastPort, sQueue);
        mTimer = new Timer();
        ssSa = mStateMachine.myAddrs;
    }

    public void sendBroadcast(Message brd) {
        synchronized (sQueue) {
            sQueue.add(brd);
            sQueue.notify();
        }
    }

    public void startBroadcasting() {
        brdReceiver.run();
        brdSender.run();
        BroadcastHandler handler = new BroadcastHandler();
        Thread t = new Thread(handler);
        t.start();
    }

    private void sendClaimToken() {
        Message brdToSend = new Message(NULL_ADDRESS, mStateMachine.myAddrs, FC.CT, 0, new Data(""));
        sendBroadcast(brdToSend);
    }

    public void sendSS() {
        Message brdToSend = new Message(mStateMachine.nextStation, mStateMachine.myAddrs, FC.SS, 0, new Data(""));
        sendBroadcast(brdToSend);
    }

    public void sendSS2ByLeader(BroadcastResult bResult) {
        Message brdToSend = new Message(mStateMachine.myAddrs, mStateMachine.myAddrs, FC.SS2, 0, new Data(""));
        sendBroadcast(brdToSend);
        ss2Buffer = new ArrayList<>();
        SS2HandleTask ss2ht = new SS2HandleTask(bResult);
        mTimer.schedule(ss2ht, mStateMachine.delay);
    }

    public void sendSSByLeader(BroadcastResult bResult) {
        Message brdToSend = new Message(mStateMachine.nextStation, mStateMachine.myAddrs, FC.SS, 0, new Data(""));
        sendBroadcast(brdToSend);
        ssBuffer = new ArrayList<>();
        SSHandleTask ssht = new SSHandleTask(bResult);
        mTimer.schedule(ssht, mStateMachine.delay);
    }

    public void sendSS2(InetAddress da) {
        Message brdToSend = new Message(da, mStateMachine.myAddrs, FC.SS2, 0, new Data(""));
        sendBroadcast(brdToSend);
    }

    //TODO: перенести в SM
    private void onClaimTokenReceive(Message brd) {
        if (!mStateMachine.claimTokenMode) {
            sendClaimToken();
            mStateMachine.claimTokenBuffer = new ArrayList<>();
            mStateMachine.claimTokenBuffer.add(brd.sa);
            mStateMachine.claimTokenMode = true;
            ClaimTokenHandleTask cthr = new ClaimTokenHandleTask();
            mTimer.schedule(cthr, mStateMachine.delay);
        } else {
            mStateMachine.claimTokenBuffer.add(brd.sa);
        }
    }

    private void onSolicitSuccessorReceive(Message brd) {
        if (!ssSa.equals(brd.da)) {
            ssSa = brd.sa;
        } else {
            //Если кто-то левый(не лидер) отвечает на SS1 лидера, то мы игнорируем
            return;
        }
        if (brd.sa != mStateMachine.myAddrs) {
            if (between(brd.sa, brd.da)) {
                sendSS();
            }
        }
    }

    private boolean between(InetAddress sa, InetAddress da) {
        ArrayList<InetAddress> ls = new ArrayList<>();
        ls.add(sa); ls.add(da); ls.add(mStateMachine.myAddrs);
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
        if (brd.sa != mStateMachine.myAddrs && brd.sa == brd.da) {
            sendSS2(brd.sa);
        }
        if (mStateMachine.imLeader && mStateMachine.ss2Mode) {
            ss2Buffer.add(brd.sa);
        }
    }

    private void onTokenReceive(Message brd) {

    }

    private void handleReceivedClaimTokens() {
        mStateMachine.claimTokenMode = false;
        ArrayList<InetAddress> ias = mStateMachine.claimTokenBuffer;
        ias.sort(new InetAddrsComparator());
        if (mStateMachine.myAddrs == ias.get(ias.size() - 1)) {
            onBecomeLeader();
        }
    }

    private void handleReceivedSS2(BroadcastResult bRes) {
        bRes.onResult(ss2Buffer);
    }

    private void handleReceivedSS(BroadcastResult bRes) {
        bRes.onResult(ssBuffer);
    }

    class BroadcastHandler implements Runnable {
        @Override
        public void run() {
            while (true) {
                if (!rQueue.isEmpty()) {
                    handleBroadcast(rQueue.poll());
                } else {
                    try {
                        rQueue.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private void handleBroadcast(Message brd) {
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
                case T:
                    onTokenReceive(brd);
            }
        }
    }

    class ClaimTokenHandleTask extends TimerTask {
        @Override
        public void run() {
            handleReceivedClaimTokens();
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

    private class SSHandleTask extends TimerTask {
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
