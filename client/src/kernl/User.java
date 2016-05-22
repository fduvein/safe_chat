package kernl;

import key.MyRSAKey;

import java.security.Key;
import java.security.KeyPair;
import java.util.ArrayList;

/**
 * Created by mso on 16-5-19.
 */
public class User {
    private String userID;
    private Key kpubC;
    private Key kpriC;
    private ArrayList<Friend> friendList;

    public User(String userID) {
        this.userID = userID;
        friendList = new ArrayList<>();
    }

    public void geneKeyPair() {
        // generate client public-private key
        KeyPair keyPair = MyRSAKey.geneKeyPair();
        kpubC = keyPair.getPublic();
        kpriC = keyPair.getPrivate();
    }

    public Key getKpubC() {
        return kpubC;
    }

    public Key getKpriC() {
        return kpriC;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public void setKpubC(Key kpubC) {
        this.kpubC = kpubC;
    }

    public void setKpriC(Key kpriC) {
        this.kpriC = kpriC;
    }

    public ArrayList<Friend> getFriendList() {
        return friendList;
    }

    public void setFriendList(ArrayList<Friend> friendList) {
        this.friendList = friendList;
    }
    public void addFriendIntoList(String id,Key k){
        friendList.add(new Friend(id,k));
    }

    @Override
    public String toString() {
        return userID;
    }
}
