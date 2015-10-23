package tr.utils;

import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Created by ks.kochetov on 20.10.2015.
 */
public interface BroadcastResult {
    void onResult(ArrayList<InetAddress> resultBuffer);
}
