package kernl;

import java.security.Key;
import java.util.ArrayList;

/**
 * Created by mso on 16-5-21.
 */
public class User {
    private UserKey kpubC;
    private String ID;
    private ArrayList<String> friendsIDList;


    public User(String ID, UserKey kpubC) {
        this.kpubC = kpubC;
        this.ID = ID;
        this.friendsIDList = new ArrayList<>();
    }

    public UserKey getKpubC() {
        return kpubC;
    }

    public void setKpubC(UserKey kpubC) {
        this.kpubC = kpubC;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public ArrayList<String> getFriendsIDList() {
        return friendsIDList;
    }

    public void setFriendsIDList(ArrayList<String> friendsIDList) {
        this.friendsIDList = friendsIDList;
    }
}
