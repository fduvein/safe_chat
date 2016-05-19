import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by mso on 16-5-18.
 */
public class Client extends JFrame {
    private final String host = "localhost";
    private final int port = 8000;

    private String kpubS;

    private User user;

    private DataOutputStream toServer;
    private DataInputStream fromServer;

    public static void main(String[] args) {
        new Client();
    }

    public Client() {
        try {
            Socket socket = new Socket(host, port);
            fromServer = new DataInputStream(socket.getInputStream());
            toServer = new DataOutputStream(socket.getOutputStream());

            fromServer.close();
            toServer.close();
        } catch (IOException e) {
            // tell user connection not available
        }
    }

    public void register(String userID) {
        User user = new User(userID);

        // construct register message
        // KpubS("REG"+id+kpubC+KpriC(time))
        String encryptedRequest = encrypt("REG\n" + userID + "\n" + user.getKpubC() + "\n" + encrypt(System.currentTimeMillis()+"", user.getKpriC()), kpubS);

        try {
            // send register message to server
            toServer.writeUTF(encryptedRequest);
            toServer.flush();

            // get reply from server
            // KpubC(Message+KpriS(time))
            String encryptedReply = fromServer.readUTF();

            String reply = decrypt(encryptedReply, user.getKpriC());

            // check whether it is from server


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String decrypt(String encryptMessage, String key) {
        return null;
    }

    private String encrypt(String message, String key) {
        return null;
    }

}
