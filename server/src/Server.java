import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

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
            Message reply = null;
            try {
                inputFromClient = socket.getInputStream();
                outputToClient = socket.getOutputStream();
                while (true) {
                    int messageLength = new DataInputStream(inputFromClient).readInt();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    for (int i = 0; i < messageLength; i += STREAM_SEGMENT_LENGTH) {
                        byte[] encryptBytes = new byte[STREAM_SEGMENT_LENGTH];
                        inputFromClient.read(encryptBytes);
                        byte[] bytes = KeyGene.decrypt(encryptBytes, kpriS);
                        baos.write(bytes);
                    }
                    byte[] messageBytes = baos.toByteArray();
                    message = Message.readObject(messageBytes);
                    // check time stamp
                    byte[] encryptTimeStamp = message.getEncryptedTimeStamp();
                    Key kpubC = message.getSenderPubKey();
                    byte[] timeStampBytes = KeyGene.decrypt(encryptTimeStamp, kpubC);
                    long timeStamp = Long.parseLong(new String(timeStampBytes));
                    long timeDiff = System.currentTimeMillis() - timeStamp;
                    if (timeDiff > 0 && timeDiff < MAX_TIME_DIFF) {
                        switch (message.getType()) {
                            case REGISTER: {
                                reply = register(message);
                                break;
                            }
                        }
                    } else {
                        // time stamp out of date error
                        reply = new Message(Message.Type.FAILED, "", "");
                        reply.setContent("time out");
                    }
                    secureSend(outputToClient, reply, message.getSenderPubKey());
                }
            }
            catch (IOException e) {
                // socket error
                System.err.println("socket error");
            } catch (ClassNotFoundException e) {
                // bytes array can not deserialize to Message object error
                System.err.println("bytes array can not deserialize");
            } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
                // can not decrypt message error
                System.err.println("can not decrypt message error");
            }

        }

        private Message register(Message message) {
            Message reply;
            // check whether id exist in user list
            boolean exist = false;
            for (User u: userList) {
                if (u.getID().equals(message.getSenderID())) {
                    exist = true;
                }
            }
            if (exist) {
                // user id exist
                reply = new Message(Message.Type.FAILED, "", "");
                reply.setContent("user id existed");
            } else {
                // create the user and add it into userList
                User user = new User(message.getSenderID(), message.getSenderPubKey());
                userList.add(user);
                userDataXML.updateXml(userList);
                // tell the client success
                reply = new Message(Message.Type.SUCCESS, "", "");
            }
            return reply;
        }

        public void secureSend(OutputStream outputToClient, Message reply, Key key) {
            try {
                // add time stamp
                String replyTimeStamp = System.currentTimeMillis()+"";
                reply.setEncryptedTimeStamp(KeyGene.encrypt(replyTimeStamp.getBytes(), kpriS));
                // encrypt the message
                byte[] replyBytes = Message.writeObject(reply);
                int segmentNum = replyBytes.length/MESSAGE_SEGMENT_LENGTH;
                int remainder = replyBytes.length%MESSAGE_SEGMENT_LENGTH;
                int replyLength;
                if (remainder == 0) {
                    replyLength = segmentNum*STREAM_SEGMENT_LENGTH;
                } else {
                    replyLength = (segmentNum+1)*STREAM_SEGMENT_LENGTH;
                }
                new DataOutputStream(outputToClient).writeInt(replyLength);
                for (int i = 0; i < segmentNum; i++) {
                    byte[] bytes = new byte[MESSAGE_SEGMENT_LENGTH];
                    for (int j = 0; j < bytes.length; j++) {
                        bytes[j] = replyBytes[i*MESSAGE_SEGMENT_LENGTH + j];
                    }
                    byte[] encryptBytes = KeyGene.encrypt(bytes, key);
                    outputToClient.write(encryptBytes);
                }
                if (remainder != 0) {
                    byte[] bytes = new byte[remainder];
                    for (int k = 0; k < remainder; k++) {
                        bytes[k] = replyBytes[(segmentNum)*MESSAGE_SEGMENT_LENGTH+k];
                    }
                    byte[] encryptBytes = KeyGene.encrypt(bytes, key);
                    outputToClient.write(encryptBytes);
                }
                outputToClient.write(replyBytes);
                outputToClient.flush();
            } catch (IOException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                e.printStackTrace();
            }
        }
    }
}
