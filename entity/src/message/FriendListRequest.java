package message;

public class FriendListRequest extends AbstractMessage {
    private String senderId;
    public FriendListRequest(byte[] cipherTimeStamp, String senderId) {
        super(Type.FRIEND_LIST_REQUEST, cipherTimeStamp);
        this.senderId = senderId;
    }

    public String getSenderId() {
        return senderId;
    }
}
