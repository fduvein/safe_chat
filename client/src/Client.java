import javax.crypto.spec.SecretKeySpec;
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
    private final long MAX_TIME_DIFF = 1000*5;
    private final int STREAM_SEGMENT_LENGTH = 128;
    private final int MESSAGE_SEGMENT_LENGTH = 117;

    private Key kpubS;

    private OutputStream toServer;
    private InputStream fromServer;

    public static void main(String[] args) {
        new Client();
    }

    public Client() {
        try {
            // read server public key
            File publicKeyFile = new File(SERVER_PUBLIC_KEY_FILE);
            if (publicKeyFile.exists()) {
                ObjectInputStream publicKeyInputStream = new ObjectInputStream(new FileInputStream(publicKeyFile));
                kpubS = (Key)publicKeyInputStream.readObject();
                publicKeyInputStream.close();
            }
            // construct socket with server
            Socket socket = new Socket(HOST, PORT);
            fromServer = socket.getInputStream();
            toServer = socket.getOutputStream();
        } catch (IOException e) {
            // tell user connection not available
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void sendRSAMessage(Message message, Key key) {
        try {
            // add time stamp
            String messageTimeStamp = System.currentTimeMillis()+"";
            message.setEncryptedTimeStamp(RSAKey.encrypt(messageTimeStamp.getBytes(), key));
            byte[] messageBytes = Message.writeObject(message);
            // encrypt the message
            int segmentNum = messageBytes.length/MESSAGE_SEGMENT_LENGTH;
            int remainder = messageBytes.length%MESSAGE_SEGMENT_LENGTH;
            int messageLength;
            if (remainder == 0) {
                messageLength = segmentNum*STREAM_SEGMENT_LENGTH;
            } else {
                messageLength = (segmentNum+1)*STREAM_SEGMENT_LENGTH;
            }
            new DataOutputStream(toServer).writeInt(messageLength);
            for (int i = 0; i < segmentNum; i++) {
                byte[] bytes = new byte[MESSAGE_SEGMENT_LENGTH];
                for (int j = 0; j < bytes.length; j++) {
                    bytes[j] = messageBytes[i*MESSAGE_SEGMENT_LENGTH + j];
                }
                byte[] encryptBytes = RSAKey.encrypt(bytes, kpubS);
                toServer.write(encryptBytes);
            }
            if (remainder != 0) {
                byte[] bytes = new byte[remainder];
                for (int k = 0; k < remainder; k++) {
                    bytes[k] = messageBytes[(segmentNum)*MESSAGE_SEGMENT_LENGTH+k];
                }
                byte[] encryptBytes = RSAKey.encrypt(bytes, kpubS);
                toServer.write(encryptBytes);
            }
            toServer.write(messageBytes);
            toServer.flush();
        } catch (IOException e) {
            // socket error
            // TODO
        } catch (Exception e) {
            // can not encrypt error
            // TODO
        }
    }

    public void register(String userID) {
        User user = new User(userID);
        Message message = new Message(Message.Type.REGISTER);
        message.setSenderID(userID);
        // add user public key
        message.setSenderPubKey(user.getKpubC());
        // send message to server through secure channel
        sendRSAMessage(message, user.getKpriC());
        // wait server to response and get the reply from server
        try {
            int messageLength = new DataInputStream(fromServer).readInt();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            for (int i = 0; i < messageLength; i += STREAM_SEGMENT_LENGTH) {
                byte[] encryptBytes = new byte[STREAM_SEGMENT_LENGTH];
                fromServer.read(encryptBytes);
                byte[] bytes = new byte[0];
                try {
                    bytes = RSAKey.decrypt(encryptBytes, user.getKpriC());
                } catch (Exception e) {
                    // can not decrypt error
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
                throw new InvalidMessageException();
            }
            // get necessary info
            byte[] encryptTimeStamp = message.getEncryptedTimeStamp();
            if (encryptTimeStamp.length == 0) {
                throw new InvalidMessageException();
            }
            // check time stamp
            Key kpubC = message.getSenderPubKey();
            byte[] replyTimeStampBytes = new byte[0];
            try {
                replyTimeStampBytes = RSAKey.decrypt(encryptTimeStamp, kpubC);
            } catch (Exception e) {
                // can not decrypt time stamp
                throw new InvalidMessageException();
            }
            long replyTimeStamp = Long.parseLong(new String(replyTimeStampBytes));
            long timeDiff = System.currentTimeMillis() - replyTimeStamp;
            if (timeDiff < 0 || timeDiff > MAX_TIME_DIFF) {
                throw new InvalidMessageException();
            }
            if (reply.getType() == Message.Type.SUCCESS) {
                System.out.println("register success");
                // TODO
                // store user key in local
                ObjectOutputStream publicKeyOutputStream = new ObjectOutputStream(new FileOutputStream("client/res/kpub_" + userID + ".key"));
                ObjectOutputStream privateKeyOutputStream = new ObjectOutputStream(new FileOutputStream("client/res/kpri_" + userID + ".key"));
                publicKeyOutputStream.writeObject(user.getKpubC());
                privateKeyOutputStream.writeObject(user.getKpriC());
                publicKeyOutputStream.close();
                privateKeyOutputStream.close();
            } else if (reply.getType() == Message.Type.FAILED) {
                // register failed
                // TODO
            }
        } catch (IOException e) {
            // socket error
            // TODO
        } catch (InvalidMessageException e) {
            // invalid message
            // TODO
        } catch (NumberFormatException e) {
            // invalid message
            // TODO
        }
    }

    public void login(String userID, Key kpriC) {
        Message message = new Message(Message.Type.LOGIN);
        message.setSenderID(userID);
        sendRSAMessage(message, kpriC);

        // wait server to response and get the reply from server
        try {
            int messageLength = new DataInputStream(fromServer).readInt();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            for (int i = 0; i < messageLength; i += STREAM_SEGMENT_LENGTH) {
                byte[] encryptBytes = new byte[STREAM_SEGMENT_LENGTH];
                fromServer.read(encryptBytes);
                byte[] bytes = new byte[0];
                try {
                    bytes = RSAKey.decrypt(encryptBytes, kpriC);
                } catch (Exception e) {
                    // can not decrypt error
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
                throw new InvalidMessageException();
            }
            // get necessary info
            byte[] encryptTimeStamp = message.getEncryptedTimeStamp();
            if (encryptTimeStamp.length == 0) {
                throw new InvalidMessageException();
            }
            // check time stamp
            Key kpubC = message.getSenderPubKey();
            byte[] replyTimeStampBytes = new byte[0];
            try {
                replyTimeStampBytes = RSAKey.decrypt(encryptTimeStamp, kpubC);
            } catch (Exception e) {
                // can not decrypt time stamp
                throw new InvalidMessageException();
            }
            long replyTimeStamp = Long.parseLong(new String(replyTimeStampBytes));
            long timeDiff = System.currentTimeMillis() - replyTimeStamp;
            if (timeDiff < 0 || timeDiff > MAX_TIME_DIFF) {
                throw new InvalidMessageException();
            }
            if (reply.getType() == Message.Type.SUCCESS) {
                System.out.println("login success");
                // TODO
                // get KCS
                byte[] decodedKey = Base64.getDecoder().decode(reply.getContent());
                Key kcs = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");

            } else if (reply.getType() == Message.Type.FAILED) {
                System.out.println("login fail");
                // TODO
            }
        } catch (IOException e) {
            // socket error
            // TODO
        } catch (InvalidMessageException e) {
            // invalid message
            // TODO
        } catch (NumberFormatException e) {
            // invalid message
            // TODO
        }
    }

}
