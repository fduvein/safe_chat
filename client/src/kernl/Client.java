package kernl;

import key.MyAESKey;
import key.MyRSAKey;
import message.InvalidMessageException;
import message.Message;

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
    private final long MAX_TIME_DIFF = 1000 * 5;
    private final int STREAM_SEGMENT_LENGTH = 128;
    private final int MESSAGE_SEGMENT_LENGTH = 117;

    private Key kpubS;

    private OutputStream toServer;
    private InputStream fromServer;



    public static void main(String[] args) {
        //System.out.print(new Client().register("tx"));
        System.out.print(new Client().login("freemso", new File("client/res/kpri_freemso.key")));

        while (true) {

        }
    }

    public Client() {
        try {
            // read server public key
            File publicKeyFile = new File(SERVER_PUBLIC_KEY_FILE);
            if (publicKeyFile.exists()) {
                ObjectInputStream publicKeyInputStream = new ObjectInputStream(new FileInputStream(publicKeyFile));
                kpubS = (Key) publicKeyInputStream.readObject();
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

    public String register(String userID) {
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
            int messageLength = new DataInputStream(fromServer).readInt();
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
                Key kcs = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");


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


    public class ConnectToServer implements Runnable {
        private User user;
        private Key kcs;
        private Socket socket;
        private InputStream inputFromClient;
        private OutputStream outputToClient;

        public ConnectToServer(Socket socket, User user, Key kcs) {
            this.user = user;
        }

        @Override
        public void run() {
            try {
                inputFromClient = socket.getInputStream();
                outputToClient = socket.getOutputStream();
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
                    case YES: {

                    }
                    case NO: {
                        break;
                    }
                    case CHAT: {
                        break;
                    }
                    case SESSION_KEY: {
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
