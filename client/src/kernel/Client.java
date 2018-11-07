package kernel;

import dao.ServerDao;
import dao.UserDao;
import encryptor.AESEncryptor;
import encryptor.RSAEncryptor;
import encryptor.Util;
import message.*;
import ui.MainFrame;
import user.ClientUser;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

import static java.lang.Thread.currentThread;

public class Client {
    private final String HOST = "localhost";
    private final int PORT = 8000;
    private Socket socket;


    public Client() {
        try {
            ServerDao.init();
            UserDao.init();
            socket = new Socket(HOST, PORT);
            new Thread(new ListenToServer(this, socket.getInputStream())).start();
        } catch (IOException e) {
            System.err.println("connection is not available");
            e.printStackTrace();
        }
    }

    public void sendMessage(AbstractMessage message, Key key, Datagram.MessageEncryptType messageEncryptType) {
        byte[] cipherMessageBytes;
        if (messageEncryptType == Datagram.MessageEncryptType.RSA) {
            cipherMessageBytes = RSAEncryptor.encryptMessage(message, key);
        } else {
            cipherMessageBytes = AESEncryptor.encryptMessage(message, key);
        }
        Datagram datagram = new Datagram(cipherMessageBytes, messageEncryptType);
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(datagram);
            objectOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void register(String userId) {
        KeyPair keyPair = RSAEncryptor.geneRSAKeyPair();
        ClientUser user = new ClientUser(userId, keyPair.getPrivate());
        UserDao.saveUserTemp(user);
        AbstractMessage message = new RegisterRequest(RSAEncryptor.getCipherTimeStamp(keyPair.getPrivate()), userId, keyPair.getPublic());
        sendMessage(message, ServerDao.getPublicKey(), Datagram.MessageEncryptType.RSA);
    }

    public void login(String userId, String privateKeyPath) {
        Key privateKey = UserDao.getPrivateKeyWithPath(privateKeyPath);
        AbstractMessage message = new LoginRequest(RSAEncryptor.getCipherTimeStamp(privateKey), userId);
        ClientUser temp = new ClientUser(userId, privateKey);
        UserDao.saveUserTemp(temp);
        sendMessage(message, ServerDao.getPublicKey(), Datagram.MessageEncryptType.RSA);
        UserDao.setLoginSendFlag(true);
        synchronized (this) {
            try {
                this.wait(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (UserDao.getLoginSendFlag()) {
            MainFrame.panel1.login.l4.setText("Login fail");
        }
    }

    public void friend(String friendId) {
        if (ServerDao.getKcs() != null && UserDao.getLoginUser() != null) {
            AbstractMessage message = new FriendRequest(AESEncryptor.getCipherTimeStamp(ServerDao.getKcs()), UserDao.getLoginUser().getId(), friendId);
            sendMessage(message, ServerDao.getKcs(), Datagram.MessageEncryptType.AES);
        } else {
            System.err.println("not login yet");
        }
    }

    public void acceptFriend(String askerId) {
        if (ServerDao.getKcs() != null && UserDao.getLoginUser() != null) {
            AbstractMessage message = new AcceptFriendResponse(AESEncryptor.getCipherTimeStamp(ServerDao.getKcs()), UserDao.getLoginUser().getId(), askerId);
            sendMessage(message, ServerDao.getKcs(), Datagram.MessageEncryptType.AES);
        } else {
            System.err.println("not login yet");
        }
    }

    public void rejectFriend(String askerId) {
        if (ServerDao.getKcs() != null && UserDao.getLoginUser() != null) {
            AbstractMessage message = new RejectFriendResponse(AESEncryptor.getCipherTimeStamp(ServerDao.getKcs()), UserDao.getLoginUser().getId(), askerId);
            sendMessage(message, ServerDao.getKcs(), Datagram.MessageEncryptType.AES);
        } else {
            System.err.println("not login yet");
        }
    }

    public void sendSessionKey(String receiverId) {
        if (ServerDao.getKcs() != null && UserDao.getLoginUser() != null) {
            Key sessionKey = AESEncryptor.geneAESKey();
            AbstractMessage subMessage = new SendSessionKeyRequest(RSAEncryptor.getCipherTimeStamp(UserDao.getLoginUser().getPrivateKey()), sessionKey);
            byte[] cipherMessageBytes;
            UserDao.getLoginUser().getFriendWithId(receiverId).setSessionKey(sessionKey);
            // cipherMessageBytes = RSAEncryptor.encryptMessage(subMessage, ServerDao.getPublicKey());
            cipherMessageBytes = RSAEncryptor.encryptMessage(subMessage, UserDao.getLoginUser().getFriendWithId(receiverId).getPublicKey());
            Datagram datagram = new Datagram(cipherMessageBytes, Datagram.MessageEncryptType.RSA);
            AbstractMessage message = new ForwardMessage(AESEncryptor.getCipherTimeStamp(ServerDao.getKcs()), UserDao.getLoginUser().getId(), receiverId, datagram);
            sendMessage(message, ServerDao.getKcs(), Datagram.MessageEncryptType.AES);
        } else {
            System.err.println("not login yet");
        }
    }

    public void acceptSessionKey(String receiverId) {
        if (ServerDao.getKcs() != null && UserDao.getLoginUser() != null) {
            Key sessionKey = AESEncryptor.geneAESKey();
            AbstractMessage subMessage = new AcceptSessionKeyResponse(RSAEncryptor.getCipherTimeStamp(UserDao.getLoginUser().getPrivateKey()));
            byte[] cipherMessageBytes;
            // cipherMessageBytes = RSAEncryptor.encryptMessage(subMessage, ServerDao.getPublicKey());
            cipherMessageBytes = RSAEncryptor.encryptMessage(subMessage, UserDao.getLoginUser().getFriendWithId(receiverId).getPublicKey());
            Datagram datagram = new Datagram(cipherMessageBytes, Datagram.MessageEncryptType.RSA);
            AbstractMessage message = new ForwardMessage(AESEncryptor.getCipherTimeStamp(ServerDao.getKcs()), UserDao.getLoginUser().getId(), receiverId, datagram);
            sendMessage(message, ServerDao.getKcs(), Datagram.MessageEncryptType.AES);
        } else {
            System.err.println("not login yet");
        }
    }


    public void chat(String receiverId, String content) {
        if (ServerDao.getKcs() != null && UserDao.getLoginUser() != null) {
            Key sessionKey;
            while (UserDao.getLoginUser().getFriendWithId(receiverId).getSessionKey() == null) {
                sendSessionKey(receiverId);
                // System.out.print("aaa");
                synchronized (this) {
                    try {
                        this.wait(1000 * 2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            sessionKey = UserDao.getLoginUser().getFriendWithId(receiverId).getSessionKey();
            AbstractMessage subMessage = new ChatMessage(AESEncryptor.getCipherTimeStamp(sessionKey), content);
            byte[] cipherMessageBytes;
            cipherMessageBytes = AESEncryptor.encryptMessage(subMessage, sessionKey);
            Datagram datagram = new Datagram(cipherMessageBytes, Datagram.MessageEncryptType.AES);
            AbstractMessage message = new ForwardMessage(AESEncryptor.getCipherTimeStamp(ServerDao.getKcs()), UserDao.getLoginUser().getId(), receiverId, datagram);
            sendMessage(message, ServerDao.getKcs(), Datagram.MessageEncryptType.AES);
            MainFrame.panel3.addSendMessage(receiverId, content);
        } else {
            //System.err.println("not login yet");
            MainFrame.panel3.l4.setText("not login yet");
        }
    }

    public void sendFile(String receiverId, File f) throws NoSuchAlgorithmException, InvalidKeyException {
        if (ServerDao.getKcs() != null && UserDao.getLoginUser() != null) {
            Key sessionKey;
            while (UserDao.getLoginUser().getFriendWithId(receiverId).getSessionKey() == null) {
                sendSessionKey(receiverId);
                synchronized (this) {
                    try {
                        this.wait(1000 * 2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            sessionKey = UserDao.getLoginUser().getFriendWithId(receiverId).getSessionKey();
            String path = f.getPath();
            int a = path.lastIndexOf('.');
            if (a == -1)
                path = "";
            path = path.substring(a, path.length());
            byte[] fi = Util.file2Byte(f);
            if (fi == null) {
                return;
            } else {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(sessionKey);
                byte[] macCode = mac.doFinal(fi);
                AbstractMessage subMessage = new FileMessage(AESEncryptor.getCipherTimeStamp(sessionKey), fi, macCode, path);
                byte[] cipherMessageBytes;
                cipherMessageBytes = AESEncryptor.encryptMessage(subMessage, sessionKey);
                Datagram datagram = new Datagram(cipherMessageBytes, Datagram.MessageEncryptType.AES);
                AbstractMessage message = new ForwardMessage(AESEncryptor.getCipherTimeStamp(ServerDao.getKcs()), UserDao.getLoginUser().getId(), receiverId, datagram);
                sendMessage(message, ServerDao.getKcs(), Datagram.MessageEncryptType.AES);

                // MainFrame.panel3.addSendMessage(receiverId, content);
                MainFrame.panel3.l4.setText("Send File Success");
            }
        } else {
            // System.err.println("not login yet");
            MainFrame.panel3.l4.setText("not login yet");
        }
    }

    public void buy() {
        if (ServerDao.getKcs() != null && UserDao.getLoginUser() != null) {
            AbstractMessage message = new BuyMessage(AESEncryptor.getCipherTimeStamp(ServerDao.getKcs()), UserDao.getLoginUser().getId());
            sendMessage(message, ServerDao.getKcs(), Datagram.MessageEncryptType.AES);
        } else {
            System.err.println("not login yet");
        }
    }

    public void requestFileKey(String i, File selectedFile) {
        if (ServerDao.getKcs() != null && UserDao.getLoginUser() != null) {
            byte[] a = Util.file2Byte(selectedFile);
            if (a.length > 8) {
                byte[] b = new byte[8];
                for (int in = 0; in < 8; in++) {
                    b[in] = a[in];
                }
                String res = new String(b);
                //  System.out.print(res);
                AbstractMessage message = new FileKeyRequest(AESEncryptor.getCipherTimeStamp(ServerDao.getKcs()), i, res);
                sendMessage(message, ServerDao.getKcs(), Datagram.MessageEncryptType.AES);
            } else {
                MainFrame.panel4.l4.setText("Wrong File");
            }
        } else {
            System.err.println("not login yet");
        }
    }

    public void close() {
        if (ServerDao.getKcs() != null) {
            AbstractMessage message = new UserClose(AESEncryptor.getCipherTimeStamp(ServerDao.getKcs()));
            sendMessage(message, ServerDao.getKcs(), Datagram.MessageEncryptType.AES);
        }

    }

//    public void friendListRequest() {
//        if (ServerDao.getKcs() != null && UserDao.getLoginUser() != null) {
//            AbstractMessage message = new FriendListRequest(AESEncryptor.getCipherTimeStamp(ServerDao.getKcs()), UserDao.getLoginUser().getId());
//            sendMessage(message, ServerDao.getKcs(), Datagram.MessageEncryptType.AES);
//        } else {
//            System.err.println("not login yet");
//        }
//    }

}