package message;

public class AskFriendListRequest extends AbstractMessage {
    private String senderID;
    public AskFriendListRequest(byte[] cipherTimeStamp, String senderID) {
        super(Type.ASK_FRIEND_LIST_REQUEST, cipherTimeStamp);
        this.senderID = senderID;
    }

    public String getSenderID() {
        return senderID;
    }
}
