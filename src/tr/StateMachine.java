package tr;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

/**
 * Created by ks.kochetov on 16.10.2015.
 */
public class StateMachine {
    public int broadcastPort;
    public int tcpPort;
    public int broadcastWaitingTime = 2000;
    public int networkActivityTime = 6000;
    public InetAddress successorAddrs, myAddrs;
    public long lastBroadcast;
    public boolean imLeader;
    public Data lastData;

    public StateMachine(int brPort, int tcpPort) throws SocketException {
        this.broadcastPort = brPort;
        this.tcpPort = tcpPort;
        successorAddrs = null;
        imLeader = false;
        NetworkInterface nw;
        nw = NetworkInterface.getByName("wlan0");
        myAddrs = nw.getInterfaceAddresses().get(1).getAddress();
        //myAddrs = nw.getInterfaceAddresses().get(0).getAddress();
        lastData = new Data("1");
        lastData = lastData.update();
    }
}
