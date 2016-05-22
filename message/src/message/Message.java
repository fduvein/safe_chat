package message;

import java.io.*;
import java.security.Key;
import java.util.ArrayList;

/**
 * Created by mso on 16-5-20.
 */
public class Message implements Serializable {
    private Type type;
    private String senderID;
    private String receiverID;
    private Key senderPubKey;
    private Key receiverPubKey;
    private byte[] encryptedTimeStamp;
    private byte[] content;
    private Key sessionKey;
    private Message subMessage;
    private ArrayList<FriendInfo> friendInfo=new ArrayList<>();

    private class  FriendInfo{
        String id;
        Key key;
        FriendInfo(String i,Key k){
            id=i;
            key=k;
        }
    }
    public void addFriendInfo(String a,Key b){
        friendInfo.add(new FriendInfo(a,b));
    }
    public Message(Type type) {
        this.type = type;
    }

    public static byte[] writeObject(Message message) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(message);
        byte[] bytes = bos.toByteArray();
        oos.flush();
        bos.close();
        oos.close();
        return bytes;
    }

    public static Message readObject(byte[] messageBytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(messageBytes);
        ObjectInputStream ois = new ObjectInputStream(bis);
        Message message = (Message) ois.readObject();
        bis.close();
        ois.close();
        return message;
    }


    public String getSenderID() {
        return senderID;
    }

    public void setSenderID(String senderID) {
        this.senderID = senderID;
    }

    public String getReceiverID() {
        return receiverID;
    }

    public void setReceiverID(String receiverID) {
        this.receiverID = receiverID;
    }

    public Key getSenderPubKey() {
        return senderPubKey;
    }

    public void setSenderPubKey(Key senderPubKey) {
        this.senderPubKey = senderPubKey;
    }

    public Key getReceiverPubKey() {
        return receiverPubKey;
    }

    public void setReceiverPubKey(Key receiverPubKey) {
        this.receiverPubKey = receiverPubKey;
    }

    public byte[] getEncryptedTimeStamp() {
        return encryptedTimeStamp;
    }

    public void setEncryptedTimeStamp(byte[] encryptedTimeStamp) {
        this.encryptedTimeStamp = encryptedTimeStamp;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public Type getType() {
        return type;
    }

    public Key getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(Key sessionKey) {
        this.sessionKey = sessionKey;
    }

    public Message getSubMessage() {
        return subMessage;
    }

    public void setSubMessage(Message subMessage) {
        this.subMessage = subMessage;
    }

    public enum Type {
        REGISTER, LOGIN, SUCCESS, FAILED, FRIENDING, YES_TO_FRIENDING, NO_TO_FRIENDING, NEGO_SESSION_KEY, FRIEND_LIST, QUERY, CHAT,FILE
    }

}
