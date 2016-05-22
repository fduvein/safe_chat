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
    private final String PUBLIC_KEY_FILE = "server/res/kpubS.key";
    private final String PRIVATE_KEY_FILE = "server/res/kpriS.key";
    private final String USER_DATA_FILE = "server/res/users.xml";
    private final long MAX_TIME_DIFF = 1000*5;
    private final int STREAM_SEGMENT_LENGTH = 128;
    private final int MESSAGE_SEGMENT_LENGTH = 117;

    private Key kpubS;
    private Key kpriS;

    private ArrayList<User> userList;
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

        // get user data
        userDataXML = new UserDataXML(USER_DATA_FILE);
        userList = userDataXML.getUserList();
    }



    public Key getKpubS() {
        return kpubS;
    }

    public Key getKpriS() {
        return kpriS;
    }

    private class HandleAClient implements Runnable{
        private Socket socket;
        public HandleAClient(Socket socket) {
            this.socket = socket;
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
                            throw new InvalidMessageException();
                        }
                        long timeStamp = 0;
                        try {
                            timeStamp = Long.parseLong(new String(timeStampBytes));
                        } catch (NumberFormatException e) {
                            // time stamp format exception
                            throw new InvalidMessageException();
                        }
                        long timeDiff = System.currentTimeMillis() - timeStamp;
                        if (timeDiff < 0 || timeDiff > MAX_TIME_DIFF) {
                            // time stamp out of data exception
                            throw new InvalidMessageException();
                        }
                        // check whether id exist in user list
                        boolean exist = false;
                        for (User u: userList) {
                            if (u.getID().equals(senderID)) {
                                exist = true;
                            }
                        }
                        if (exist) {
                            // user id already exist error
                            throw new InvalidMessageException();
                        } else {
                            // create the user and add it into userList
                            User user = new User(senderID, kpubC);
                            userList.add(user);
                            userDataXML.updateXml(userList);
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
                        // TODO
                    }

                    // check whether id exist in user list
                    for (User u: userList) {
                        if (u.getID().equals(message.getSenderID())) {
                            // get user public key
                            Key kpubC = u.getKpubC();
                            try {
                                // check time stamp
                                byte[] encryptTimeStamp = message.getEncryptedTimeStamp();
                                byte[] timeStampBytes = new byte[0];
                                try {
                                    timeStampBytes = MyRSAKey.decrypt(encryptTimeStamp, kpubC);
                                } catch (Exception e) {
                                    // can not decrypt time stamp error
                                    throw new InvalidMessageException();
                                }
                                long timeStamp = Long.parseLong(new String(timeStampBytes));
                                long timeDiff = System.currentTimeMillis() - timeStamp;
                                if (timeDiff < 0 || timeDiff > MAX_TIME_DIFF) {
                                    // time stamp out of date error
                                    throw new InvalidMessageException();
                                }
                                // generate AES key
                                Key kcs = MyAESKey.geneKey();
                                Message reply = new Message(Message.Type.SUCCESS);
                                byte[] content = Base64.getEncoder().encode(kcs.getEncoded());
                                reply.setContent(content);
                                sendRSAMessage(outputToClient, reply, kpubC);
                            } catch (InvalidMessageException e) {
                                Message reply = new Message(Message.Type.FAILED);
                                sendRSAMessage(outputToClient, reply, kpubC);
                            }
                        }
                    }
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
}
