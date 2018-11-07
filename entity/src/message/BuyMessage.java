package message;

/**
 * Created by Administrator on 2016/5/24.
 */
public class BuyMessage extends AbstractMessage{
    private String senderID;
    public BuyMessage(byte[] cipherTimeStamp, String senderID) {
        super(Type.BUY_FILE_MESSAGE, cipherTimeStamp);
        this.senderID = senderID;
    }
    public String getSenderID() {
        return senderID;
    }
}
