package message;

/**
 * Created by Administrator on 2016/5/24.
 */
public class BuyResponse extends AbstractMessage {

    private String rand;
    private byte[] file;
    private byte[] mac;
    private String fileType;
    // TODO
    public BuyResponse(byte[] cipherTimeStamp, String a, byte[] f, byte[] m,String fi) {
        super(Type.BUY_FILE_RESPONSE, cipherTimeStamp);
        rand = a;
        file = f;
        mac = m;
        fileType=fi;
    }

    public byte[] getFile() {
        return file;
    }
    public byte[] getMac() {
        return mac;
    }
    public String getRand() {
        return rand;
    }
    public String getFileType(){
        return fileType;
    }
}
