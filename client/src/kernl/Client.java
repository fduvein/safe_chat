package kernl;

import key.MyAESKey;
import key.MyRSAKey;
import message.InvalidMessageException;
import message.Message;
import ui.ChatPanel;

import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.security.Key;
import java.util.Base64;

/**
 * Created by mso on 16-5-18.
 */
public class Client {
    private final String HOST = "localhost";
    private final int PORT = 8000;
    private final String SERVER_PUBLIC_KEY_FILE = "client/res/kpubS.key";
    private final long MAX_TIME_DIFF = 1000 * 5;
    private final int STREAM_SEGMENT_LENGTH = 128;
    private final int MESSAGE_SEGMENT_LENGTH = 117;
    private Key kpubS;

    private Socket socket;

    private User user;
    private ChatPanel chatPanel;
    private Key kcs;


    public static void main(String[] args) {
        //System.out.print(new Client().register("tx"));
//        System.out.print(new Client().login("freemso", new File("client/res/kpri_freemso.key")));

        while (true) {

        }
    }

    public Client(ChatPanel p1) {
        chatPanel = p1;
        try {
            // read server public key
            File publicKeyFile = new File(SERVER_PUBLIC_KEY_FILE);
            if (publicKeyFile.exists()) {
                ObjectInputStream publicKeyInputStream = new ObjectInputStream(new FileInputStream(publicKeyFile));
                kpubS = (Key) publicKeyInputStream.readObject();
                publicKeyInputStream.close();
            }
            // construct socket with server
            socket = new Socket(HOST, PORT);
        } catch (IOException e) {
            // tell user connection not available
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public String register(String userID) {
        User user = new User(userID);
        user.geneKeyPair();
        Message message = new Message(Message.Type.REGISTER);
        message.setSenderID(userID);
        // add user public key
        message.setSenderPubKey(user.getKpubC());
        // send message to server through secure channel
        sendRSAMessage(message, user.getKpriC());
        // wait server to response and get the reply from server
        try {
            InputStream fromServer = socket.getInputStream();
            int messageLength = new DataInputStream(fromServer).readInt();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            for (int i = 0; i < messageLength; i += STREAM_SEGMENT_LENGTH) {
                byte[] encryptBytes = new byte[STREAM_SEGMENT_LENGTH];
                fromServer.read(encryptBytes);
                byte[] bytes = new byte[0];
                try {
                    bytes = MyRSAKey.decrypt(encryptBytes, user.getKpriC());
                } catch (Exception e) {
                    // can not decrypt error
                    System.err.println("can not decrypt message");
                    throw new InvalidMessageException();
                }
                byteArrayOutputStream.write(bytes);
            }
            byte[] messageBytes = byteArrayOutputStream.toByteArray();
            Message reply;
            try {
                reply = Message.readObject(messageBytes);
            } catch (ClassNotFoundException e) {
                // can not deserialize error
                System.err.println("can not deserialize");
                throw new InvalidMessageException();
            }
            // get necessary info
            byte[] encryptTimeStamp = reply.getEncryptedTimeStamp();
            if (encryptTimeStamp.length == 0) {
                System.err.println("message info miss");
                throw new InvalidMessageException();
            }
            // check time stamp
            byte[] replyTimeStampBytes = new byte[0];
            try {
                replyTimeStampBytes = MyRSAKey.decrypt(encryptTimeStamp, kpubS);
            } catch (Exception e) {
                // can not decrypt time stamp
                System.err.println("can not decrypt time stamp");
                throw new InvalidMessageException();
            }
            long replyTimeStamp = Long.parseLong(new String(replyTimeStampBytes));
            long timeDiff = System.currentTimeMillis() - replyTimeStamp;
            if (timeDiff < 0 || timeDiff > MAX_TIME_DIFF) {
                System.err.println("time stamp out of date");
                throw new InvalidMessageException();
            }
            if (reply.getType() == Message.Type.SUCCESS) {
                // store user private key in local
                ObjectOutputStream privateKeyOutputStream = new ObjectOutputStream(new FileOutputStream("client/res/kpri_" + userID + ".key"));
                privateKeyOutputStream.writeObject(user.getKpriC());
                privateKeyOutputStream.close();
                return "register success" + "\n" + "Your password is at client/res/kpri_" + userID + ".key";
            } else if (reply.getType() == Message.Type.FAILED) {
                // register failed
                return "register fail";
            }
            return "register fail";
        } catch (IOException | InvalidMessageException | NumberFormatException e) {
            // socket error
            // invalid message
            // invalid message
            return "Socket error";
        }

    }

    public String login(String userID, File kpriCFile) {
        ClientKey clientKey = new ClientKey(kpriCFile);
        Key kpriC = clientKey.getPrivateKey();
        Message message = new Message(Message.Type.LOGIN);
        message.setSenderID(userID);
        sendRSAMessage(message, kpriC);

        // wait server to response and get the reply from server
        try {
            InputStream fromServer = socket.getInputStream();
            int messageLength = new DataInputStream(fromServer).readInt();
            // System.out.println(messageLength);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            for (int i = 0; i < messageLength; i += STREAM_SEGMENT_LENGTH) {
                byte[] encryptBytes = new byte[STREAM_SEGMENT_LENGTH];
                fromServer.read(encryptBytes);
                byte[] bytes = new byte[0];
                try {
                    bytes = MyRSAKey.decrypt(encryptBytes, kpriC);
                } catch (Exception e) {
                    // can not decrypt error
                    System.err.println("can not decrypt login message");
                    throw new InvalidMessageException();
                }
                byteArrayOutputStream.write(bytes);
            }
            byte[] messageBytes = byteArrayOutputStream.toByteArray();
            // System.out.println(messageBytes.length);
            Message reply;
            try {
                reply = Message.readObject(messageBytes);
            } catch (ClassNotFoundException e) {
                // can not deserialize error
                System.err.println("can not deserialize error");
                throw new InvalidMessageException();
            }
            // get necessary info
            byte[] encryptTimeStamp = reply.getEncryptedTimeStamp();
            if (encryptTimeStamp.length == 0) {
                System.err.println("message miss info");
                throw new InvalidMessageException();
            }
            // check time stamp
            byte[] replyTimeStampBytes = new byte[0];
            try {
                replyTimeStampBytes = MyRSAKey.decrypt(encryptTimeStamp, kpubS);
            } catch (Exception e) {
                // can not decrypt time stamp
                System.err.println("can not decrypt time stamp");
                throw new InvalidMessageException();
            }
            long replyTimeStamp = Long.parseLong(new String(replyTimeStampBytes));
            long timeDiff = System.currentTimeMillis() - replyTimeStamp;
            if (timeDiff < 0 || timeDiff > MAX_TIME_DIFF) {
                System.err.println("time stamp out of date");
                throw new InvalidMessageException();
            }
            if (reply.getType() == Message.Type.SUCCESS) {

                // get KCS
                byte[] decodedKey = Base64.getDecoder().decode(reply.getContent());
                kcs = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
                user = new User(userID);
                user.setKpriC(kpriC);
                //getFriendList();
                ListenToServer listenToServer = new ListenToServer(socket, user, kcs);
                new Thread(listenToServer).start();
                // System.out.println("aaaaa");
                return "login success";
            } else if (reply.getType() == Message.Type.FAILED) {
                return "login fail";
            }
            return "login fail";
        } catch (IOException | InvalidMessageException | NumberFormatException e) {
            // socket error
            // invalid message
            // invalid message
            return "socket error";
        }
    }

    public void friending(String friendID) {
        Message message = new Message(Message.Type.FRIENDING);
        message.setSenderID(user.getUserID());
        message.setReceiverID(friendID);
        sendAESMessage(message);

    }

    public void yesToFriending(String askerID) {
        Message message = new Message(Message.Type.YES_TO_FRIENDING);
        message.setSenderID(user.getUserID());
        message.setReceiverID(askerID);
        sendAESMessage(message);
    }

    public void noToFriending(String askerID) {
        Message message = new Message(Message.Type.NO_TO_FRIENDING);
        message.setSenderID(user.getUserID());
        message.setReceiverID(askerID);
        sendAESMessage(message);
    }

    public void negoSessionKey(String receiverID) {
        // get receiver public key
        Key kpubR = null;
        for (Friend f : user.getFriendList()) {
            if (f.getId().equals(receiverID)) {
                if (f.getSessionKey() == null) {
                    return;
                } else {
                    kpubR = f.getPublicKey();
                }
            }
        }
        Message subMessage = new Message(Message.Type.NEGO_SESSION_KEY);
        // generate a session key
        Key kcc = MyAESKey.geneKey();
        subMessage.setSessionKey(kcc);
        // add time stamp
        String messageTimeStamp = System.currentTimeMillis() + "";
        try {
            subMessage.setEncryptedTimeStamp(MyRSAKey.encrypt(messageTimeStamp.getBytes(), user.getKpriC()));
        } catch (Exception e) {
            System.err.println("can not encrypt");
        }
        byte[] messageBytes = new byte[0];
        try {
            messageBytes = Message.writeObject(subMessage);
        } catch (IOException e) {
            System.err.println("can not serialize message");
        }
        // encrypt the message
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int segmentNum = messageBytes.length / MESSAGE_SEGMENT_LENGTH;
        int remainder = messageBytes.length % MESSAGE_SEGMENT_LENGTH;
        for (int i = 0; i < segmentNum; i++) {
            byte[] bytes = new byte[MESSAGE_SEGMENT_LENGTH];
            for (int j = 0; j < bytes.length; j++) {
                bytes[j] = messageBytes[i * MESSAGE_SEGMENT_LENGTH + j];
            }
            byte[] encryptBytes = new byte[0];
            try {
                encryptBytes = MyRSAKey.encrypt(bytes, kpubR);
            } catch (Exception e) {
                System.err.println("can not encrypt message");
            }
            try {
                byteArrayOutputStream.write(encryptBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (remainder != 0) {
            byte[] bytes = new byte[remainder];
            for (int k = 0; k < remainder; k++) {
                bytes[k] = messageBytes[(segmentNum) * MESSAGE_SEGMENT_LENGTH + k];
            }
            byte[] encryptBytes = new byte[0];
            try {
                encryptBytes = MyRSAKey.encrypt(bytes, kpubS);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                byteArrayOutputStream.write(encryptBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        byte[] content = byteArrayOutputStream.toByteArray();
        Message message = new Message(Message.Type.NEGO_SESSION_KEY);
        message.setSenderID(user.getUserID());
        message.setReceiverID(user.getUserID());
        message.setContent(content);
        sendAESMessage(message);
    }

    private void getFriendList() {
        Message message = new Message(Message.Type.FRIEND_LIST);
        message.setSenderID(user.getUserID());
        sendAESMessage(message);

    }

    public void sendRSAMessage(Message message, Key key) {
        try {
            //System.out.println("b");
            OutputStream toServer = socket.getOutputStream();
            // add time stamp
            String messageTimeStamp = System.currentTimeMillis() + "";
            message.setEncryptedTimeStamp(MyRSAKey.encrypt(messageTimeStamp.getBytes(), key));
            byte[] messageBytes = Message.writeObject(message);
            // encrypt the message
            int segmentNum = messageBytes.length / MESSAGE_SEGMENT_LENGTH;
            int remainder = messageBytes.length % MESSAGE_SEGMENT_LENGTH;
            int messageLength;
            if (remainder == 0) {
                messageLength = segmentNum * STREAM_SEGMENT_LENGTH;
            } else {
                messageLength = (segmentNum + 1) * STREAM_SEGMENT_LENGTH;
            }
            new DataOutputStream(toServer).writeInt(messageLength);
            for (int i = 0; i < segmentNum; i++) {
                byte[] bytes = new byte[MESSAGE_SEGMENT_LENGTH];
                for (int j = 0; j < bytes.length; j++) {
                    bytes[j] = messageBytes[i * MESSAGE_SEGMENT_LENGTH + j];
                }
                byte[] encryptBytes = MyRSAKey.encrypt(bytes, kpubS);
                toServer.write(encryptBytes);
            }
            if (remainder != 0) {
                byte[] bytes = new byte[remainder];
                for (int k = 0; k < remainder; k++) {
                    bytes[k] = messageBytes[(segmentNum) * MESSAGE_SEGMENT_LENGTH + k];
                }
                byte[] encryptBytes = MyRSAKey.encrypt(bytes, kpubS);
                toServer.write(encryptBytes);
            }
            toServer.flush();
        } catch (IOException e) {
            // socket error
            // TODO
        } catch (Exception e) {
            // can not encrypt error
            // TODO
        }
    }

    public void sendAESMessage(Message message) {
        try {

            OutputStream toServer = socket.getOutputStream();

            String timeStampStr = System.currentTimeMillis() + "";
            message.setEncryptedTimeStamp(MyAESKey.encrypt(timeStampStr.getBytes(), kcs));
            byte[] messageBytes = Message.writeObject(message);
            byte[] encryptedMessageBytes = MyAESKey.encrypt(messageBytes, kcs);
            int messageLength = encryptedMessageBytes.length;
           // System.out.println(messageLength);
            new DataOutputStream(toServer).writeInt(messageLength);
            toServer.write(encryptedMessageBytes);
            toServer.flush();
            toServer.close();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("socket error");
        } catch (Exception e) {
            System.err.println("can not encrypt");
        }

    }

    public class ListenToServer implements Runnable {
        private User user;
        private Key kcs;
        private Socket socket;
        private InputStream inputFromServer;

        public ListenToServer(Socket socket, User user, Key kcs) {
            this.user = user;
            this.socket = socket;
            this.kcs = kcs;
        }

        @Override
        public void run() {
            try {
                inputFromServer = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (true) {
                handleAMessage();
            }

        }

        public void handleAMessage() {
            Message subMessage;
            try {
                int messageLength = new DataInputStream(inputFromServer).readInt();
              //  System.out.println(messageLength);
                if (messageLength > 0) {
                    byte[] encryptedMessageBytes = new byte[messageLength];
                    inputFromServer.read(encryptedMessageBytes);
                    // decrypt using kcs
                    byte[] messageBytes;
                    if (kcs != null) {
                        try {
                            messageBytes = MyAESKey.decrypt(encryptedMessageBytes, kcs);
                        } catch (Exception e) {
                            // can not decrypt
                            System.err.println("can not decrypt aes");
                            throw new InvalidMessageException();
                        }
                    } else {
                        throw new InvalidMessageException();
                    }
                    try {
                        subMessage = Message.readObject(messageBytes);
                    } catch (ClassNotFoundException e) {
                        // can not deserialize message error
                        System.err.println("can not deserialize message");
                        throw new InvalidMessageException();
                    }

                    // check time stamp
                    byte[] subEncryptedTimeStamp = subMessage.getEncryptedTimeStamp();
                    if (subEncryptedTimeStamp.length == 0) {
                        // no time stamp
                        System.err.println("no time stamp");
                        throw new InvalidMessageException();
                    }
                    byte[] subTimeStampBytes = new byte[0];
                    try {
                        subTimeStampBytes = MyAESKey.decrypt(subEncryptedTimeStamp, kcs);
                    } catch (Exception e) {
                        // can not decrypt time stamp error
                        System.err.println("can not decrypt sub time stamp");
                        throw new InvalidMessageException();
                    }
                    long subTimeStamp = Long.parseLong(new String(subTimeStampBytes));
                    long subTimeDiff = System.currentTimeMillis() - subTimeStamp;
                    if (subTimeDiff < 0 || subTimeDiff > MAX_TIME_DIFF) {
                        // time stamp out of date error
                        System.err.println("sub time stamp out of date");
                        throw new InvalidMessageException();
                    }

                    switch (subMessage.getType()) {
                        case FRIENDING: {
                            String senderID = subMessage.getSenderID();
                            int answer = JOptionPane.showOptionDialog(null, senderID + "want to make friends with you, accept or not", "Friend Request", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
                            // send to server the answer
                            Message reply;
                            if (answer == 0) {
                                reply = new Message(Message.Type.NO_TO_FRIENDING);
                            } else {
                                reply = new Message(Message.Type.YES_TO_FRIENDING);
                            }
                            reply.setSenderID(user.getUserID());
                            reply.setReceiverID(senderID);
                            sendAESMessage(reply);
                            break;
                        }
                        case YES_TO_FRIENDING: {
                            String senderID = subMessage.getSenderID();
                            JOptionPane.showMessageDialog(null, "user " + senderID + "accept you friending request");
                            break;
                        }
                        case NO_TO_FRIENDING: {
                            String senderID = subMessage.getSenderID();
                            JOptionPane.showMessageDialog(null, "user " + senderID + "accept you friending request");
                            break;
                        }
                        case CHAT: {
                            break;
                        }
                        case NEGO_SESSION_KEY: {
                            break;
                        }
                        case FRIEND_LIST: {
                            for (int i = 0; i < subMessage.getFriendInfo().size(); i++) {
                                user.addFriendIntoList(subMessage.getFriendInfo().get(i).getID(), subMessage.getFriendInfo().get(i).getKey());
                            }
                            chatPanel.loadFriend(user.getFriendList());
                            break;
                        }
                        default: {
                        }
                    }
                }
            } catch (IOException e) {
                // socket error
            } catch (InvalidMessageException e) {
                System.err.println("online message error");

            }

        }
    }

}
