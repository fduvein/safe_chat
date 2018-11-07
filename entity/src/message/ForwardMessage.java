package message;

public class ForwardMessage extends AbstractMessage {
    private String senderID;
    private String receiverID;
    private Datagram datagram;
    public ForwardMessage(byte[] cipherTimeStamp, String senderID, String receiverID, Datagram datagram) {
        super(Type.FORWARD_MESSAGE, cipherTimeStamp);
        this.senderID = senderID;
        this.receiverID = receiverID;
        this.datagram = datagram;
    }

    public String getSenderID() {
        return senderID;
    }

    public String getReceiverID() {
        return receiverID;
    }

    public Datagram getDatagram() {
        return datagram;
    }
}
