package tr;

/**
 * Created by ks.kochetov on 16.10.2015.
 */
public class Data {
    public String data;

    public Data(String data) {
        this.data = data;
    }

    public int getLen() {
        return data.length();
    }

    public byte[] getBytes() {
        return data.getBytes();
    }


    public Data update() {
        String newData = new String(data);
        int intData;
        try {
            intData = Integer.parseInt(newData);
        } catch(Exception e) {
            intData = 1;
        }
        intData++;
        String nextData = Integer.toString(intData);
        return new Data(nextData);
    }
}
