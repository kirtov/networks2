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
    public int networkActivityTime = 4000;
    public InetAddress successorAddrs, myAddrs;
    public long lastBroadcast;
    public boolean imLeader;

    public StateMachine(int brPort, int tcpPort) throws SocketException {
        this.broadcastPort = brPort;
        this.tcpPort = tcpPort;
        successorAddrs = null;
        imLeader = false;
        NetworkInterface nw;
        nw = NetworkInterface.getByName("wlan0");
        myAddrs = nw.getInterfaceAddresses().get(0).getAddress();
    }
}
