import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.Timestamp;
import java.util.Date;

/**
 * Created by mso on 16-5-18.
 */
public class Client extends JFrame {
    private final String HOST = "localhost";
    private final int PORT = 8000;
    private final String SERVER_PUBLIC_KEY_FILE = "client/kpubS.key";
    private final long MAX_TIME_DIFF = 1000*5;
    private final int STREAM_SEGMENT_LENGTH = 128;
    private final int MESSAGE_SEGMENT_LENGTH = 117;

    private Key kpubS;

    private User user;

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
            Socket socket = new Socket(HOST, PORT);
            fromServer = socket.getInputStream();
            toServer = socket.getOutputStream();
            register("freemso");
            fromServer.close();
            toServer.close();
        } catch (IOException e) {
            // tell user connection not available
        } catch (ClassNotFoundException | NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
    }

    public void secureSend(Message message) throws IOException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
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
            byte[] encryptBytes = KeyGene.encrypt(bytes, kpubS);
            toServer.write(encryptBytes);
        }
        if (remainder != 0) {
            byte[] bytes = new byte[remainder];
            for (int k = 0; k < remainder; k++) {
                bytes[k] = messageBytes[(segmentNum)*MESSAGE_SEGMENT_LENGTH+k];
            }
            byte[] encryptBytes = KeyGene.encrypt(bytes, kpubS);
            toServer.write(encryptBytes);
        }
        toServer.write(messageBytes);
        toServer.flush();
    }

    public Message getReply(Key key) throws IOException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, ClassNotFoundException {
        int messageLength = new DataInputStream(fromServer).readInt();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < messageLength; i += STREAM_SEGMENT_LENGTH) {
            byte[] encryptBytes = new byte[STREAM_SEGMENT_LENGTH];
            fromServer.read(encryptBytes);
            byte[] bytes = KeyGene.decrypt(encryptBytes, key);
            baos.write(bytes);
        }
        byte[] messageBytes = baos.toByteArray();
        return Message.readObject(messageBytes);
    }

    public void register(String userID) throws IOException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, ClassNotFoundException {
        User user = new User(userID);
        // construct register message
        // KpubS("REG"+id+kpubC+KpriC(time))
        Message message = new Message(Message.Type.REGISTER, userID, "");
        message.setSenderPubKey(user.getKpubC());
        String timeStamp = System.currentTimeMillis()+"";
        message.setEncryptedTimeStamp(KeyGene.encrypt(timeStamp.getBytes(), user.getKpriC()));
        secureSend(message);

        Message reply = getReply(user.getKpriC());
        if (reply.getType() == Message.Type.SUCCESS) {
            System.out.println("success  ");
        } else if (reply.getType() == Message.Type.FAIL) {
            System.out.println("fail: " + reply.getContent());
        }
    }

}
