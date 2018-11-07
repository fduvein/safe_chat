package kernel;

import dao.ServerDao;
import dao.UserDao;
import encryptor.AESEncryptor;
import encryptor.RSAEncryptor;
import encryptor.Util;
import message.*;
import sun.applet.Main;
import ui.MainFrame;
import user.ClientUser;
import user.Friend;

import javax.crypto.Mac;
import javax.swing.*;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class ListenToServer implements Runnable {
    private InputStream fromServer;
    private Client client;
    private final String FILE_SAVE_PATH = "client/res/download/";

    public ListenToServer(Client client, InputStream fromServer) {
        this.fromServer = fromServer;
        this.client = client;
    }

    public void run() {
        while (true) {
            try {
                ObjectInputStream objectInputStream = new ObjectInputStream(fromServer);
                Datagram datagram = (Datagram) objectInputStream.readObject();
                new Thread(new HandleADatagram(datagram)).start();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public class HandleADatagram implements Runnable {
        private Datagram datagram;

        public HandleADatagram(Datagram datagram) {
            this.datagram = datagram;
        }

        @Override
        public void run() {
            AbstractMessage message = null;
            boolean timeStampValid = false;

            try {
                if (datagram.getMessageEncryptType() == Datagram.MessageEncryptType.RSA) {
                    //  System.out.println(UserDao.getTempUser().getPrivateKey());
                    message = RSAEncryptor.decryptMessage(datagram.getCipherMessageBytes(), UserDao.getTempUser().getPrivateKey());
                    // check time stamp
                    timeStampValid = RSAEncryptor.checkTimeStamp(message.getCipherTimeStamp(), ServerDao.getPublicKey());

                } else {
                    //System.out.println(ServerDao.getKcs());
                    message = AESEncryptor.decryptMessage(datagram.getCipherMessageBytes(), ServerDao.getKcs());
                    timeStampValid = AESEncryptor.checkTimeStamp(message.getCipherTimeStamp(), ServerDao.getKcs());
                }
            } catch (Exception e) {
                System.err.println("can not decrypt message");
                e.printStackTrace();
            }
            if (timeStampValid) {
                switch (message.getType()) {
                    case ACCEPT_FRIEND_RESPONSE: {
                        String senderId = ((AcceptFriendResponse) message).getSenderID();
                        String receiverId = ((AcceptFriendResponse) message).getReceiverID();
                        //  System.out.println(senderId + " and " + receiverId + " now is friends");
                        JOptionPane.showMessageDialog(null, senderId + " and " + receiverId + " now is friends");
                        break;
                    }
                    case ACCEPT_LOGIN_RESPONSE: {
                        // save kcs
                        ServerDao.setKcs(((AcceptLoginResponse) message).getKcs());
                        UserDao.setLoginUser(UserDao.copyUserTemp());
                        //System.out.println(UserDao.getTempUser().getId() + " login success");
                        MainFrame.panel1.login.l4.setText(UserDao.getTempUser().getId() + " login success");
                        MainFrame.it2.setEnabled(true);
                        MainFrame.it3.setEnabled(true);
                        MainFrame.it4.setEnabled(true);
                        MainFrame.card.show(MainFrame.s,"chat");
                        UserDao.removeTempUser();
                        UserDao.setLoginSendFlag(false);
                        // set login user
                        break;
                    }
                    case ACCEPT_REGISTER_RESPONSE: {
                        UserDao.storeUserTemp();
                        String userId = ((AcceptRegisterResponse) message).getReceiverId();
                        //System.out.println(userId + " register success");
                        MainFrame.panel1.register.l4.setText(userId + " register success" + "\n" + "Key is at res/user/" + userId + ".key");
                        break;
                    }
                    case BUY_FILE_RESPONSE: {
                        String id = ((BuyResponse) message).getRand();
                        byte[] file = ((BuyResponse) message).getFile();
                        byte[] macCode = ((BuyResponse) message).getMac();
                        String fileType=((BuyResponse) message).getFileType();
                        if (Util.checkMac(ServerDao.getKcs(), file, macCode)) {
                            String path = FILE_SAVE_PATH + UserDao.getLoginUser().getId()+fileType;
                            MainFrame.panel4.l4.setText("The file is at " + path + "\n" + "Your Record number is " + id);
                            Util.byte2File(file, path);
                        } else {
                            MainFrame.panel4.l4.setText("The file may be modified by others");
                        }
                        break;
                    }
                    case FILE_KEY_FAIL:{
                        MainFrame.panel4.l4.setText("No result found , check your RecordID or your input file");
                        break;
                    }
                    case FILE_KEY_RESPONSE: {
                        Key key = ((FileKeyResponse) message).getKey();
                        File file = MainFrame.panel4.getFile();
                        if (file != null) {
                            String p = file.getPath() + ".plain";
                            byte[] content = Util.deleteHeader(Util.file2Byte(file), 8);
                            try {
                                byte[] result = AESEncryptor.decrypt(content, key);
                                Util.byte2File(result, p);
                                MainFrame.panel4.l4.setText("Decrpty Success ! The file is at"+p );
                            } catch (Exception e) {
                                MainFrame.panel4.l4.setText("Decrpty error");
                            }
                        } else {
                            MainFrame.panel4.l4.setText("Wrong File");
                        }
                        break;
                    }
                    case FORWARD_MESSAGE: {
                        String senderId = ((ForwardMessage) message).getSenderID();
                        Datagram datagram1 = ((ForwardMessage) message).getDatagram();
                        // System.out.println("aa");
                        AbstractMessage subMessage = null;
                        boolean subTimeStampValid = false;
                        try {
                            if (datagram1.getMessageEncryptType() == Datagram.MessageEncryptType.RSA) {
                                subMessage = RSAEncryptor.decryptMessage(datagram1.getCipherMessageBytes(), UserDao.getLoginUser().getPrivateKey());
                                // check time stamp
                                subTimeStampValid = RSAEncryptor.checkTimeStamp(subMessage.getCipherTimeStamp(), UserDao.getLoginUser().getFriendWithId(senderId).getPublicKey());
                                // System.out.println(subTimeStampValid);
                            } else {
                                subMessage = AESEncryptor.decryptMessage(datagram1.getCipherMessageBytes(), UserDao.getSessionKeyWithFriendId(senderId));
                                subTimeStampValid = AESEncryptor.checkTimeStamp(subMessage.getCipherTimeStamp(), UserDao.getSessionKeyWithFriendId(senderId));
                            }
                        } catch (Exception e) {
                            System.err.println("can not decrypt message");
                            e.printStackTrace();
                        }

                        if (subTimeStampValid) {
                            switch (subMessage.getType()) {
                                case SEND_SESSION_KEY_REQUEST: {
                                    String b = senderId;
                                    UserDao.getLoginUser().getFriendWithId(senderId).setSessionKey(((SendSessionKeyRequest) subMessage).getSessionKey());
                                    client.acceptSessionKey(senderId);
                                    break;
                                }
                                case ACCEPT_SESSION_KEY_RESPONSE: {
                                    break;
                                }
                                case CHAT_MESSAGE: {
                                    MainFrame.panel3.addReceiveMessage(senderId, ((ChatMessage) subMessage).getContent());
                                    break;
                                }
                                case FILE_MESSAGE: {
                                    byte[] fi = ((FileMessage) subMessage).getFile();
                                    byte[] macCode1 = ((FileMessage) subMessage).getMac();
                                    String fileType=((FileMessage) subMessage).getFileType();
                                    String path = FILE_SAVE_PATH + senderId + Util.generateString(10)+fileType;
                                    Util.byte2File(fi, path);
                                    boolean flag = Util.checkMac(UserDao.getSessionKeyWithFriendId(senderId), fi, macCode1);
                                    if (flag) {
                                        MainFrame.panel3.l4.setText(senderId + " Send you a file " + path);
                                    } else {
                                        MainFrame.panel3.l4.setText("File sent by " + senderId + " may be modified by others");
                                    }
                                    break;
                                }
                            }
                        }
                        break;
                    }
                    case FRIEND_LIST_RESPONSE: {
                        ArrayList<Friend> a = ((FriendListResponse) message).getFriendList();
                        for (int i = 0; i < a.size(); i++) {
                            UserDao.getLoginUser().addFriend(a.get(i));
                            MainFrame.panel3.addFriend(a.get(i));
                        }
                        MainFrame.panel3.loadFriend();
                        break;
                    }
                    case FRIEND_REQUEST: {
                        String askerId = ((FriendRequest) message).getSenderID();
                        //  System.out.print(askerId + " ask to make friends with you\nWould you agree?<y/n>\n>");
                        int i = JOptionPane.showOptionDialog(null, askerId + " ask to make friends with you Would you agree?", "Invitation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
                        if (i == 0) {
                            client.acceptFriend(askerId);
                        } else {
                            client.rejectFriend(askerId);
                        }
                        break;
                    }
                    case REJECT_FRIEND_RESPONSE: {
                        String senderId = ((RejectFriendResponse) message).getSenderID();
                        //System.out.print(senderId + " reject to be friends to you");
                        JOptionPane.showMessageDialog(null, senderId + " reject to be friends to you");
                        break;
                    }
                    case REJECT_LOGIN_RESPONSE: {
                        //System.out.println(UserDao.getTempUser().getId() + " login failed");
                        MainFrame.panel1.login.l4.setText(UserDao.getTempUser().getId() + " login failed");
                        UserDao.removeTempUser();
                        break;
                    }
                    case REJECT_REGISTER_RESPONSE: {
                        UserDao.removeTempUser();
                        String userId = ((RejectRegisterResponse) message).getReceiverId();
                        // System.out.println(userId + " register failed");
                        MainFrame.panel1.register.l4.setText(userId + " register failed");
                        break;
                    }
                    case REJECT_SESSION_KEY_RESPONSE: {
                        break;
                    }

                    case USER_NOT_EXIST_RESPONSE: {
                        String userId = ((UserNotExistResponse) message).getReceiverId();
                        MainFrame.panel2.l4.setText(userId + " does not exist");
                        break;

                    }
                    case USER_OFFLINE_RESPONSE: {
                        String userId = ((UserOfflineResponse) message).getReceiverId();
                        MainFrame.panel2.l4.setText(userId + " is off line");
                        MainFrame.panel3.l4.setText(userId + " is off line");
                        break;
                    }
                }
            }
        }
    }


}
