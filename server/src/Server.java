
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.util.ArrayList;

/**
 * Created by mso on 16-5-18.
 */
public class Server {
    private final String PUBLIC_KEY_FILE = "server/kpubS";
    private final String PRIVATE_KEY_FILE = "server/kpriS";

    private Key kpubS;
    private Key kpriS;

    public static void main(String[] args) {
        new Server();
    }

    public Server() {
        // read server public-private key from file
        try {
            File publicKeyFile = new File(PUBLIC_KEY_FILE);
            File privateKeyFile = new File(PRIVATE_KEY_FILE);
            if (publicKeyFile.exists() && privateKeyFile.exists()) {
                ObjectInputStream publicKeyInputStream = new ObjectInputStream(new FileInputStream(publicKeyFile));
                ObjectInputStream privateKeyInputStream = new ObjectInputStream(new FileInputStream(privateKeyFile));
                kpubS = (Key)publicKeyInputStream.readObject();
                kpriS = (Key)privateKeyInputStream.readObject();
                publicKeyInputStream.close();
                privateKeyInputStream.close();
            } else {
                // generate a new pair of public-private key
                KeyPair keyPair = KeyGene.geneKeyPair();
                kpubS = keyPair.getPublic();
                kpriS = keyPair.getPrivate();
                ObjectOutputStream publicKeyOutputStream = new ObjectOutputStream(new FileOutputStream(publicKeyFile));
                ObjectOutputStream privateKeyOutputStream = new ObjectOutputStream(new FileOutputStream(privateKeyFile));
                publicKeyOutputStream.writeObject(kpubS);
                privateKeyOutputStream.writeObject(kpriS);
                publicKeyOutputStream.close();
                privateKeyOutputStream.close();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }


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
            try {
                InputStream inputFromClient = socket.getInputStream();
                OutputStream outputToClient = socket.getOutputStream();
                while (true) {
                    int messageLength = new DataInputStream(inputFromClient).readInt();
                    StringBuilder stringBuilder = new StringBuilder();
                    for (int i = 0; i < messageLength; i += 128) {
                        byte[] encryptBytes = new byte[128];
                        inputFromClient.read(encryptBytes);
                        byte[] bytes = KeyGene.decrypt(encryptBytes, kpriS);
                        System.out.println(bytes.length + " " + encryptBytes.length);
                        stringBuilder.append(new String(bytes));
                    }

                    Message message = Message.readObject(new String(stringBuilder).getBytes());
                    System.out.println(message.getSenderID());
//                    System.out.print(message.getSenderID());
                    // decrypt using server private key
//                    Message message = Message.readObject(KeyGene.decrypt(encryptMessage, getKpriS()));
//                    switch (message.getType()) {
//                        case REGISTER: {
//
//                        }
//                    }
                }
            }
            catch (IOException e) {
                System.err.println(e);
            } catch (NoSuchPaddingException | NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private void registerBiz(Message message) {
            // KpubS("REG"+id+kpubC+KpriC(time))
            System.out.print(message.getSenderID());

        }
    }
}
