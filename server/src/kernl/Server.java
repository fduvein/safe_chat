package kernl;

import key.MyAESKey;
import key.MyRSAKey;
import message.InvalidMessageException;
import message.Message;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Key;
import java.util.ArrayList;
import java.util.Base64;

/**
 * Created by mso on 16-5-18.
 */
public class Server {
    private final int PORT = 8000;
    public static final String USER_PUBLIC_KEY_FILE_PREFIX = "server/res/user_key/";
    private final String PUBLIC_KEY_FILE = "server/res/kpubS.key";
    private final String PRIVATE_KEY_FILE = "server/res/kpriS.key";
    private final String USER_DATA_FILE = "server/res/users.xml";
    private final long MAX_TIME_DIFF = 1000*5;
    private final int STREAM_SEGMENT_LENGTH = 128;
    private final int MESSAGE_SEGMENT_LENGTH = 117;

    private Key kpubS;
    private Key kpriS;

    private ArrayList<User> userList;
    private ArrayList<HandleAClient> onlineList;
    private UserDataXML userDataXML;

    public static void main(String[] args) {
        new Server().start();
    }

    public Server() {
        init();
    }

    public void start() {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                HandleAClient task = new HandleAClient(socket);
                new Thread(task).start();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private void init() {
        // get key pair of the server
        ServerKey serverKey = new ServerKey(PUBLIC_KEY_FILE, PRIVATE_KEY_FILE);
        kpubS = serverKey.getPublic();
        kpriS = serverKey.getPrivate();

        // get threadUser data
        userDataXML = new UserDataXML(USER_DATA_FILE);
        userList = userDataXML.getUserList();

        // init online list
        onlineList = new ArrayList<>();
    }



    public Key getKpubS() {
        return kpubS;
    }

    public Key getKpriS() {
        return kpriS;
    }

    private class HandleAClient implements Runnable {
        private Socket socket;
        private Key kcs;
        private User threadUser;
        public HandleAClient(Socket socket) {
            this.socket = socket;
        }

        public User getThreadUser() {
            return threadUser;
        }

        public void setThreadUser(User threadUser) {
            this.threadUser = threadUser;
        }

        public void run() {
            InputStream inputFromClient = null;
            OutputStream outputToClient = null;
            Message message = null;
            try {
                inputFromClient = socket.getInputStream();
                outputToClient = socket.getOutputStream();
                int messageLength = new DataInputStream(inputFromClient).readInt();
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                for (int i = 0; i < messageLength; i += STREAM_SEGMENT_LENGTH) {
                    byte[] encryptedBytes = new byte[STREAM_SEGMENT_LENGTH];
                    inputFromClient.read(encryptedBytes);
                    byte[] bytes = new byte[0];
                    try {
                        bytes = MyRSAKey.decrypt(encryptedBytes, kpriS);
                    } catch (Exception e) {
                        // can not decrypt message error
                        // TODO
                    }
                    byteArrayOutputStream.write(bytes);
                }
                byte[] messageBytes = byteArrayOutputStream.toByteArray();
                try {
                    message = Message.readObject(messageBytes);
                } catch (ClassNotFoundException e) {
                    // can not deserialize message error
                    // TODO
                }
            } catch (IOException e) {
                // socket error
                // TODO
            }

            switch (message.getType()) {
                case REGISTER: {
                    // get necessary info
                    String senderID = message.getSenderID();
                    byte[] encryptedTimeStamp = message.getEncryptedTimeStamp();
                    Key kpubC = message.getSenderPubKey();
                    // check info integrity
                    if (encryptedTimeStamp.length == 0 || kpubC == null || senderID == "") {
                        // message miss info
                        // TODO
                    }
                    try {
                        // check time stamp
                        byte[] timeStampBytes = new byte[0];
                        try {
                            timeStampBytes = MyRSAKey.decrypt(encryptedTimeStamp, kpubC);
                        } catch (Exception e) {
                            // can not decrypt time stamp error
                            System.err.println("can not decrypt time stamp error");
                            throw new InvalidMessageException();
                        }
                        long timeStamp = 0;
                        try {
                            timeStamp = Long.parseLong(new String(timeStampBytes));
                        } catch (NumberFormatException e) {
                            // time stamp format exception
                            System.err.println("time stamp format exception");
                            throw new InvalidMessageException();
                        }
                        long timeDiff = System.currentTimeMillis() - timeStamp;
                        if (timeDiff < 0 || timeDiff > MAX_TIME_DIFF) {
                            // time stamp out of date exception
                            System.err.println("time stamp out of date exc");
                            throw new InvalidMessageException();
                        }
                        // check whether id exist in threadUser list
                        boolean exist = false;
                        for (User u: userList) {
                            if (u.getID().equals(senderID)) {
                                exist = true;
                            }
                        }
                        if (exist) {
                            // threadUser id already exist error
                            System.err.println("threadUser id already exist");
                            throw new InvalidMessageException();
                        } else {
                            // create the threadUser and add it into userList
                            UserKey userKey = new UserKey(kpubC);
                            User user = new User(senderID, userKey);
                            userList.add(user);
                            userDataXML.updateXml(userList);
                            userKey.saveKeyTo(USER_PUBLIC_KEY_FILE_PREFIX + senderID + ".key");
                            // tell the client success
                            Message reply = new Message(Message.Type.SUCCESS);
                            sendRSAMessage(outputToClient, reply, kpubC);
                        }
                    } catch (InvalidMessageException e) {
                        Message reply = new Message(Message.Type.FAILED);
                        sendRSAMessage(outputToClient, reply, kpubC);
                    }
                    break;
                }
                case LOGIN: {
                    // get necessary info
                    String senderID = message.getSenderID();
                    byte[] encryptedTimeStamp = message.getEncryptedTimeStamp();
                    // check info integrity
                    if (encryptedTimeStamp.length == 0 || senderID == "") {
                        // message miss info
                        System.err.println("message miss info");
                        // TODO
                    }

                    // check whether id exist in threadUser list
                    for (User user: userList) {
                        if (user.getID().equals(message.getSenderID())) {
                            // get threadUser public key
                            Key kpubC = user.getKpubC().getPublic();
                            //System.out.print(Base64.getEncoder().encodeToString(kpubC.getEncoded()));
                            try {
                                // check time stamp
                                byte[] timeStampBytes = new byte[0];
                                try {
                                    timeStampBytes = MyRSAKey.decrypt(encryptedTimeStamp, kpubC);
                                } catch (Exception e) {
                                    // can not decrypt time stamp error
                                    System.err.println("can not decrypt login time stamp");
                                    throw new InvalidMessageException();
                                }
                                long timeStamp = Long.parseLong(new String(timeStampBytes));
                                long timeDiff = System.currentTimeMillis() - timeStamp;
                                if (timeDiff < 0 || timeDiff > MAX_TIME_DIFF) {
                                    // time stamp out of date error
                                    System.err.println("time stamp out of date");
                                    throw new InvalidMessageException();
                                }
                                // generate AES key
                                kcs = MyAESKey.geneKey();
                                // send it to client
                                Message reply = new Message(Message.Type.SUCCESS);
                                byte[] content = Base64.getEncoder().encode(kcs.getEncoded());
                                reply.setContent(content);
                                sendRSAMessage(outputToClient, reply, kpubC);
                                // set threadUser online
                                threadUser = user;
                                onlineList.add(this);
                                OnlineClient onlineClient = new OnlineClient(socket, kcs);
                                new Thread(onlineClient).start();

                            } catch (InvalidMessageException e) {
                                Message reply = new Message(Message.Type.FAILED);
                                sendRSAMessage(outputToClient, reply, kpubC);
                            }
                        }
                    }
                }
                default: {

                }
            }


        }

        public void sendRSAMessage(OutputStream outputToClient, Message message, Key key) {
            // add time stamp
            String timeStampStr = System.currentTimeMillis()+"";
            try {
                message.setEncryptedTimeStamp(MyRSAKey.encrypt(timeStampStr.getBytes(), kpriS));
            } catch (Exception e) {
                // can not encrypt error
                // TODO
            }
            // encrypt the message
            byte[] messageBytes = new byte[0];
            try {
                messageBytes = Message.writeObject(message);
            } catch (IOException e) {
                // can not serialize error
                // TODO
            }
            int segmentNum = messageBytes.length/MESSAGE_SEGMENT_LENGTH;
            int remainder = messageBytes.length%MESSAGE_SEGMENT_LENGTH;
            int replyLength;
            if (remainder == 0) {
                replyLength = segmentNum*STREAM_SEGMENT_LENGTH;
            } else {
                replyLength = (segmentNum+1)*STREAM_SEGMENT_LENGTH;
            }
            try {
                new DataOutputStream(outputToClient).writeInt(replyLength);
            } catch (IOException e) {
                // socket error
                // TODO
            }
            for (int i = 0; i < segmentNum; i++) {
                byte[] bytes = new byte[MESSAGE_SEGMENT_LENGTH];
                for (int j = 0; j < bytes.length; j++) {
                    bytes[j] = messageBytes[i*MESSAGE_SEGMENT_LENGTH + j];
                }
                byte[] encryptBytes = new byte[0];
                try {
                    encryptBytes = MyRSAKey.encrypt(bytes, key);
                } catch (Exception e) {
                    // can not encrypt error
                    // TODO
                }
                try {
                    outputToClient.write(encryptBytes);
                } catch (IOException e) {
                    // socket error
                    // TODO
                }
            }
            if (remainder != 0) {
                byte[] bytes = new byte[remainder];
                for (int k = 0; k < remainder; k++) {
                    bytes[k] = messageBytes[(segmentNum)*MESSAGE_SEGMENT_LENGTH+k];
                }
                byte[] encryptBytes = new byte[0];
                try {
                    encryptBytes = MyRSAKey.encrypt(bytes, key);
                } catch (Exception e) {
                    // can not encrypt error
                    // TODO

                }
                try {
                    outputToClient.write(encryptBytes);
                } catch (IOException e) {
                    // socket error
                    // TODO
                }
            }
            try {
                outputToClient.write(messageBytes);
                outputToClient.flush();
            } catch (IOException e) {
                // socket error
                // TODO
            }
        }
    }

    private class OnlineClient implements Runnable {
        private Socket socket;
        private Key kcs;

        public OnlineClient(Socket socket, Key kcs) {
            this.socket = socket;
        }

        @Override
        public void run() {
            InputStream inputFromClient = null;
            OutputStream outputToClient = null;
            try {
                inputFromClient = socket.getInputStream();
                outputToClient = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (true) {
                Message subMessage;
                try {
                    int messageLength = new DataInputStream(inputFromClient).readInt();
                    byte[] encryptedMessageBytes = new byte[messageLength];
                    inputFromClient.read(encryptedMessageBytes);
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
                            String receiverID = subMessage.getReceiverID();
                            if (receiverID == "") {
                                // message miss info
                                System.err.print("sub message miss info");
                                throw new InvalidMessageException();
                            }
                            // check whether receiver id is online
                            boolean online = false;
                            for (HandleAClient thread: onlineList) {
                                if (thread.getThreadUser().getID().equals(receiverID)) {
                                    online = true;
                                    System.out.println("This user is online");
                                }
                            }
                            if (!online) {
                                System.err.println("threadUser does not online");
                                throw new InvalidMessageException();
                            }
                            break;
                        }
                        case FRIEND_LIST: {
                            break;
                        }
                        case QUERY: {
                            break;
                        }
                        case FORWARD: {
                            break;
                        }
                        default: {

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
}
