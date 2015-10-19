package tr;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;

/**
 * Created by ks.kochetov on 16.10.2015.
 */
public class StateMachine {
    public int delay = 2000;
    public ArrayList<InetAddress> claimTokenBuffer, ssBuffer, ss2Buffer;
    public boolean claimTokenMode, ss2Mode, ssMode;
    public InetAddress nextStation, prevStation, myAddrs;
    public boolean hasToken;
    public String token;
    public long lastBroadcast;
    public boolean imLeader;


    public StateMachine() throws SocketException {
        nextStation = null;
        prevStation = null;
        hasToken = false;
        token = "";
        NetworkInterface nw;
        nw = NetworkInterface.getByName("wlan0");
        myAddrs = nw.getInterfaceAddresses().get(0).getAddress();
        claimTokenMode = false;
        ss2Mode = false;
        ssMode = false;
    }

}
