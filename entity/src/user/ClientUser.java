package user;

import java.security.Key;
import java.util.ArrayList;

public class ClientUser extends User {
    private Key privateKey;
    private ArrayList<Friend> friendList;
    public ClientUser(String id, Key privateKey) {
        super(id);
        this.privateKey = privateKey;
        friendList = new ArrayList<>();
    }

    public Key getPrivateKey() {
        return privateKey;
    }

    public void addFriend(Friend friend) {
        for(int i=0;i<friendList.size();i++){
            if(friendList.get(i).getId().equals(friend.getId())){
                return;
            }
        }
        friendList.add(friend);
    }

    public ArrayList<Friend> getFriendList() {
        return friendList;
    }

    public Friend getFriendWithId(String friendId) {
        for (Friend f: friendList) {
            if (f.getId().equals(friendId)) {
                return f;
            }
        }
        return null;
    }
}
