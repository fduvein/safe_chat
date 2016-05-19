import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by mso on 16-5-18.
 */
public class Server {
    private String KpubS;
    private String KpriS;

    public static void main(String[] args) {
        new Server();
    }

    public Server() {
        // generate server public-private key
        geneKpubS();
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(8000);
            while (true) {
                Socket socket = serverSocket.accept();
                HandleAClient task = new HandleAClient(socket);
                new Thread(task).start();
            }
        } catch (IOException e) {
            System.err.println(e);
        }

    }

    public void geneKpubS() {
        // generate public-private key for server
    }

    public String getKpubS() {
        return KpubS;
    }

    public String getKpriS() {
        return KpriS;
    }

    private class HandleAClient implements Runnable{
        private Socket socket;
        public HandleAClient(Socket socket) {
            this.socket = socket;
        }
        public void run() {
            try {
                DataInputStream inputFromClient = new DataInputStream(socket.getInputStream());
                DataOutputStream outputToClient = new DataOutputStream(socket.getOutputStream());
                while (true) {
                    String encryptMessage = inputFromClient.readUTF();
                    // decrypt using server private key
                    String message = decrypt(encryptMessage, getKpriS());
                    String messageType = message.split("\n")[0];
                    switch (messageType) {
                        case "REG": {
                            registerBiz(message);
                            break;
                        }
                        case "LOG": {

                        }
                    }
                }
            }
            catch (IOException e) {
                System.err.println(e);
            }
        }

        private void registerBiz(String message) {
        }

        private String decrypt(String encryptMessage, String key) {
            // TO DO
            return null;
        }
    }
}
