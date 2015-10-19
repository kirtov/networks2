package tr.broadcast;

import tr.Data;

import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * Created by kk1 on 15.10.2015.
 */
public class Broadcast {

    InetAddress da,sa;
    byte fc;
    FC eFc;
    int len;
    Data data;

    public Broadcast(InetAddress da, InetAddress sa, byte fc, int len, Data data) {
        this.da = da;
        this.sa = sa;
        this.fc = fc;

        if (fc == 0) eFc = FC.CT;
        else if (fc == 1) eFc = FC.SS;
        else if (fc == 2) eFc = FC.SS2;
        else eFc = FC.T;

        this.len = len;
        this.data = data;
    }

    public Broadcast(InetAddress da, InetAddress sa, FC ffc, int len, Data data) {
        this.da = da;
        this.sa = sa;
        this.eFc = ffc;

        if (eFc == FC.CT) fc = 0;
        else if (eFc == FC.SS) fc = 1;
        else if (eFc == FC.SS2) fc = 2;
        else fc = 3;

        this.len = len;
        this.data = data;
    }

    public byte[] getBytes() {
        int minLen = 1+4+4+4;
        byte[] bytes = new byte[minLen + data.getLen()];
        bytes[0] = fc;
        System.arraycopy(da.getAddress(), 0, bytes, 1, 4);
        System.arraycopy(sa.getAddress(), 0, bytes, 5, 4);
        System.arraycopy(ByteBuffer.allocate(4).putInt(len).array(), 0, bytes, 9, 4);
        System.arraycopy(data.getBytes(), 0, bytes, 13, data.getLen());
        return bytes;
    }

}