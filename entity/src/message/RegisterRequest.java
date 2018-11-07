package message;

import java.security.Key;

public class RegisterRequest extends AbstractMessage {
    private String senderID;
    private Key senderPublicKey;
    public RegisterRequest(byte[] cipherTimeStamp, String senderID, Key senderPublicKey) {
        super(Type.REGISTER_REQUEST, cipherTimeStamp);
        this.senderID = senderID;
        this.senderPublicKey = senderPublicKey;
    }

    public String getSenderID() {
        return senderID;
    }

    public Key getSenderPublicKey() {
        return senderPublicKey;
    }
}
