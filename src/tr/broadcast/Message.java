package tr.broadcast;

import tr.Data;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by kk1 on 15.10.2015.
 */
public class Message {
    InetAddress da, sa;
    byte fc;
    public ControlEventByte eFc;
    int len;
    public Data data;

    private String errorDa = "Invalid destination address ";
    private String errorSa = "Invalid source address ";
    private String createDa = "create da = ";
    private String createSa = "create sa = ";


    public Message(InetAddress da, InetAddress sa, byte fc, Data data) {
        this.da = da;
        this.sa = sa;
        this.fc = fc;

        this.eFc = byteToFC(fc);

        this.len = data.getLen();
        this.data = data;
    }

    public Message(InetAddress da, InetAddress sa, ControlEventByte ffc, Data data) {
        this.da = da;
        this.sa = sa;
        this.eFc = ffc;

        if (eFc == ControlEventByte.CT) fc = 0;
        else if (eFc == ControlEventByte.SS) fc = 1;
        else if (eFc == ControlEventByte.SS2) fc = 2;
        else fc = 3;

        this.len = data.getLen();
        this.data = data;
    }

    private ControlEventByte byteToFC(byte b) {
        ControlEventByte eFc;
        if (fc == 0) eFc = ControlEventByte.CT;
        else if (fc == 1) eFc = ControlEventByte.SS;
        else if (fc == 2) eFc = ControlEventByte.SS2;
        else eFc = ControlEventByte.T;
        return eFc;
    }

    public Message(byte[] data) {
        fc = data[0];
        eFc = byteToFC(fc);
        try {
            da = InetAddress.getByAddress(Arrays.copyOfRange(data, 1, 5));
            sa = InetAddress.getByAddress(Arrays.copyOfRange(data, 5, 9));
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.out.println("ERROR WHILE PARSING ADRESSES");
        }

        len = getLength(Arrays.copyOfRange(data, 9, 13));
        if (len > 0) {
            this.data = new Data(new String(Arrays.copyOfRange(data, 13, 13 + len)));
        } else {
            this.data = new Data("");
        }
    }

    private int getLength(byte[] data) {
        return ((data[0] << 24) | (data[1] << 16) | (data[2] << 8) | (data[3]));
    }

    public byte[] getBytes() {
        int minLen = 1 + 4 + 4 + 4;
        byte[] bytes = new byte[minLen + data.getLen()];
        bytes[0] = fc;
        System.arraycopy(da.getAddress(), 0, bytes, 1, 4);
        System.arraycopy(sa.getAddress(), 0, bytes, 5, 4);
        System.arraycopy(ByteBuffer.allocate(4).putInt(len).array(), 0, bytes, 9, 4);
        System.arraycopy(data.getBytes(), 0, bytes, 13, data.getLen());
        return bytes;
    }

    public InetAddress getDestinationAddress() {
        return da;
    }

    public InetAddress getSourceAddress() {
        return sa;
    }

    public void setDestinationAddress(InetAddress da) {
        this.da = da;
    }

    public boolean isToken() {
        return eFc == ControlEventByte.T;
    }

    public boolean dataIsNull() {
        return data.getLen() == 0;
    }

    @Override
    public String toString() {
        return "FC = " + eFc.toString() + ", DA = " + da.toString() + " , SA = " + sa.toString();
    }
}