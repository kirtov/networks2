package tr;

import tr.broadcast.Message;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

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
        List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        for (NetworkInterface i : interfaces) {
            if (i.getHardwareAddress() != null) {
                List<InterfaceAddress> add = i.getInterfaceAddresses();
                for (InterfaceAddress a : add) {
                    if (a.getBroadcast() != null) {
                        myAddrs = a.getAddress();
                    }
                }
            }
        }
        lastData = new Data("1");
        lastData = lastData.update();
    }
}
