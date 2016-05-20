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
    private final String SERVER_PUBLIC_KEY_FILE = "client/kpubS";

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

    public void register(String userID) throws IOException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, ClassNotFoundException {
        User user = new User(userID);
        // construct register message
        // KpubS("REG"+id+kpubC+KpriC(time))
        Message message = new Message(Message.Type.REGISTER, userID, "server");
        message.setSenderPubKey(user.getKpubC());
        String timeStamp = System.currentTimeMillis()+"";
        message.setEncryptedTimeStamp(KeyGene.encrypt(timeStamp.getBytes(), user.getKpriC()));
//        String test = "Doing so tells your program that a field named \"gear\" exists, holds numerical data, and has an initial value of \"1\". A variable's data type determines the values it may contain, plus the operations that may be performed on it. In addition to int, the Java programming language supports seven other primitive data types. A primitive type is predefined by the language and is named by a reserved keyword. Primitive values do not share state with other primitive values. The eight primitive data types supported by the Java programming language are:";
//        byte[] messageBytes = test.getBytes();

        byte[] messageBytes = Message.writeObject(message);
        Message test = Message.readObject(new String(messageBytes).getBytes());
        System.out.print(test.getSenderID());

        // encrypt the message
        int segmentNum = messageBytes.length/117;
        int remainder = messageBytes.length%117;
        int messageLength;
        if (remainder == 0) {
            messageLength = segmentNum*128;
        } else {
            messageLength = (segmentNum+1)*128;
        }
        new DataOutputStream(toServer).writeInt(messageLength);
        for (int i = 0; i < segmentNum; i++) {
            byte[] bytes = new byte[117];
            for (int j = 0; j < bytes.length; j++) {
                bytes[j] = messageBytes[i*117 + j];
            }
            byte[] encryptBytes = KeyGene.encrypt(bytes, kpubS);
            System.out.println(bytes.length + " " + encryptBytes.length);
            toServer.write(encryptBytes);
        }
        if (remainder != 0) {
            byte[] bytes = new byte[remainder];
            for (int k = 0; k < remainder; k++) {
                bytes[k] = messageBytes[(segmentNum)*117+k];
            }
            byte[] encryptBytes = KeyGene.encrypt(bytes, kpubS);
            System.out.println(bytes.length + " " + encryptBytes.length);
            toServer.write(encryptBytes);
        }
        System.out.println(new String(messageBytes));
        toServer.flush();

//        // get reply from server
//        // KpubC(Message+KpriS(time))
//        String encryptedReply = fromServer.read();
//        String reply = KeyGene.decrypt(encryptedReply, user.getKpriC());
    }

}
