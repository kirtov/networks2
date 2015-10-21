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


    /**
     * Обрабатывает полученные данные и возвращает измененный объект
     */
    public Data update() {
        String newData = new String(data);
        if (newData.length() == 0) {
            newData = "1";
        } else {
            int lastNum = Integer.parseInt(newData.substring(newData.length() - 1));
            lastNum++;
            newData = newData + lastNum;
        }
        return new Data(newData);
    }
}
