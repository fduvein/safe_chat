package dao;

import user.ClientUser;
import user.Friend;

import java.io.*;
import java.security.Key;
import java.util.HashMap;

public class UserDao {
    private static final String USER_KEY_PATH_PREFIX = "client/res/userKey/";

    private static ClientUser tempUser;

    private static ClientUser loginUser;
    private static boolean flag = false;

    public static void init() {
        tempUser = null;
    }

    public static HashMap<Friend, Key> friendSessionKeyHashmap = new HashMap<>();

    public static void addSessionKey(String friendId, Key sessionKey) {
        Friend friend = loginUser.getFriendWithId(friendId);
        friendSessionKeyHashmap.put(friend, sessionKey);
    }

    public static Key getSessionKeyWithFriendId(String friendId) {
        Friend friend = loginUser.getFriendWithId(friendId);
        return friend.getSessionKey();
    }

    public static Key getPrivateKeyWithPath(String privateKeyPath) {
        Key privateKey = null;
        try {
            File privateKeyFile = new File(privateKeyPath);
            if (privateKeyFile.exists()) {
                // read client private key from file
                ObjectInputStream privateKeyInputStream = new ObjectInputStream(new FileInputStream(privateKeyFile));
                privateKey = (Key) privateKeyInputStream.readObject();
                privateKeyInputStream.close();
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("can not read private key of user");
            e.printStackTrace();
        }
        return privateKey;
    }

    public static void storeUserPrivateKey(ClientUser user) {
        try {
            File keyFile = new File(USER_KEY_PATH_PREFIX + user.getId() + ".key");
            if (!keyFile.exists()) {
                keyFile.createNewFile();
            }
            ObjectOutputStream privateKeyOutputStream = new ObjectOutputStream(new FileOutputStream(keyFile));
            privateKeyOutputStream.writeObject(user.getPrivateKey());
            privateKeyOutputStream.flush();
            privateKeyOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveUserTemp(ClientUser user) {
        tempUser = user;
    }

    public static ClientUser getTempUser() {
        return tempUser;
    }

    public static void storeUserTemp() {
        if (tempUser != null) {
            storeUserPrivateKey(tempUser);
            tempUser = null;
        }
    }

    public static void removeTempUser() {
        tempUser = null;
    }

    public static ClientUser copyUserTemp() {
        return new ClientUser(tempUser.getId(), tempUser.getPrivateKey());
    }

    public static ClientUser getLoginUser() {
        return loginUser;
    }

    public static void setLoginUser(ClientUser loginUser) {
        UserDao.loginUser = loginUser;
    }

    public static void setLoginSendFlag(boolean loginSendFlag) {
        flag = loginSendFlag;
    }
    public static  boolean getLoginSendFlag(){
        return flag;
    }
}
