package tr.broadcast;

import tr.Data;
import tr.StateMachine;

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
    final ConcurrentLinkedQueue<Broadcast> sQueue;
    final ConcurrentLinkedQueue<Broadcast> rQueue;
    BroadcastReceiver brdReceiver;
    BroadcastSender brdSender;
    StateMachine sm;
    Timer mTimer;
    //если приходит SS1, то сохраняем адрес лидера, который вызвал этот процесс, чтобы ответить на этот SS1 только один раз
    InetAddress ssSa;

    public BroadcastManager(int rPort, int sPort, StateMachine stateMachine) throws SocketException, UnknownHostException {
        NULL_ADDRESS = InetAddress.getByAddress(new byte[]{0,0,0,0});
        this.sm = stateMachine;
        sQueue = new ConcurrentLinkedQueue<>();
        rQueue = new ConcurrentLinkedQueue<>();
        brdReceiver = new BroadcastReceiver(rPort, rQueue);
        brdSender = new BroadcastSender(sPort, sQueue);
        mTimer = new Timer();
        ssSa = sm.myAddrs;
    }

    public void sendBroadcast(Broadcast brd) {
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
        Broadcast brdToSend = new Broadcast(NULL_ADDRESS, sm.myAddrs, FC.CT, 0, new Data(""));
        sendBroadcast(brdToSend);
    }

    public void sendSS() {
        Broadcast brdToSend = new Broadcast(sm.nextStation, sm.myAddrs, FC.SS, 0, new Data(""));
        sendBroadcast(brdToSend);
    }

    public void sendSS2ByLeader() {
        Broadcast brdToSend = new Broadcast(sm.myAddrs, sm.myAddrs, FC.SS2, 0, new Data(""));
        sendBroadcast(brdToSend);
        sm.ss2Mode = true;
        sm.ss2Buffer = new ArrayList<>();
        SS2HandleTask ss2ht = new SS2HandleTask();
        mTimer.schedule(ss2ht, sm.delay);
    }

    public void sendSSByLeader() {
        Broadcast brdToSend = new Broadcast(sm.nextStation, sm.myAddrs, FC.SS, 0, new Data(""));
        sendBroadcast(brdToSend);
        sm.ssMode = true;
        sm.ssBuffer = new ArrayList<>();
        SSHandleTask ssht = new SSHandleTask();
        mTimer.schedule(ssht, sm.delay);
    }

    public void sendSS2(InetAddress da) {
        Broadcast brdToSend = new Broadcast(da, sm.myAddrs, FC.SS2, 0, new Data(""));
        sendBroadcast(brdToSend);
    }

    private void onClaimTokenReceive(Broadcast brd) {
        if (!sm.claimTokenMode) {
            sendClaimToken();
            sm.claimTokenBuffer = new ArrayList<>();
            sm.claimTokenBuffer.add(brd.sa);
            sm.claimTokenMode = true;
            ClaimTokenHandleTask cthr = new ClaimTokenHandleTask();
            mTimer.schedule(cthr, sm.delay);
        } else {
            sm.claimTokenBuffer.add(brd.sa);
        }
    }

    private void onBecomeLeader() {
        sm.imLeader = true;
        sendSS2ByLeader();
    }

    private void onSolicitSuccessorReceive(Broadcast brd) {
        if (!ssSa.equals(brd.da)) {
            ssSa = brd.sa;
        } else {
            //Если кто-то левый(не лидер) отвечает на SS1 лидера, то мы игнорируем
            return;
        }
        if (brd.sa != sm.myAddrs) {
            if (between(brd.sa, brd.da)) {
                sendSS();
            }
        }
    }

    private boolean between(InetAddress sa, InetAddress da) {
        ArrayList<InetAddress> ls = new ArrayList<>();
        ls.add(sa); ls.add(da); ls.add(sm.myAddrs);
        ls.sort(new InetAddrsComparator());
        int myI = 0, saI = 0, daI = 0;
        for (int i = 0; i < ls.size(); i++) {
            if (ls.get(i).equals(sa)) {
                saI = i;
            } else if (ls.get(i).equals(da)) {
                daI = i;
            } else if (ls.get(i).equals(sm.myAddrs)) {
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

    private void onSolicitSuccessor2Receive(Broadcast brd) {
        if (brd.sa != sm.myAddrs && brd.sa == brd.da) {
            sendSS2(brd.sa);
        }
        if (sm.imLeader && sm.ss2Mode) {
            sm.ss2Buffer.add(brd.sa);
        }
    }

    private void onTokenReceive(Broadcast brd) {

    }

    private void handleReceivedClaimTokens() {
        sm.claimTokenMode = false;
        ArrayList<InetAddress> ias = sm.claimTokenBuffer;
        ias.sort(new InetAddrsComparator());
        if (sm.myAddrs == ias.get(ias.size() - 1)) {
            onBecomeLeader();
        }
    }

    private void handleReceivedSS2() {
        sm.ss2Mode = false;
        ArrayList<InetAddress> ss2 = sm.ss2Buffer;
        ss2.add(sm.myAddrs);
        ss2.sort(new InetAddrsComparator());
        for (int i = 0; i < ss2.size(); i++) {
            if (ss2.get(i).equals(sm.myAddrs)) {
                if (i != 0) {
                    sm.nextStation = ss2.get(i - 1);
                    break;
                } else {
                    sm.nextStation = ss2.get(ss2.size() - 1);
                    break;
                }
            }
        }
    }

    private void handleReceivedSS() {
        sm.ssMode = false;
        ArrayList<InetAddress> ss = sm.ssBuffer;
        if (ss.size() == 0) {
            return;
        }
        ss.add(sm.myAddrs);
        ss.sort(new InetAddrsComparator());
        for (int i = 0; i < ss.size(); i++) {
            if (ss.get(i).equals(sm.myAddrs)) {
                if (i != 0) {
                    sm.nextStation = ss.get(i - 1);
                    break;
                } else {
                    sm.nextStation = ss.get(ss.size() - 1);
                    break;
                }
            }
        }
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

        private void handleBroadcast(Broadcast brd) {
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
        @Override
        public void run() {
            handleReceivedSS2();
        }
    }

    class SSHandleTask extends TimerTask {
        @Override
        public void run() {
            handleReceivedSS();
        }
    }
}
