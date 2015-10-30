package tr;

import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Created by ks.kochetov on 16.10.2015.
 */
public class Main {
    public static void main(String[] args) {
        int broadcastPort = 1234;
        int tcpPort = 12000;
        try {
            StateMachine stateMachine = new StateMachine(broadcastPort, tcpPort);
            SuperManager superManager = new SuperManager(stateMachine);
            superManager.init();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }


    }

}
