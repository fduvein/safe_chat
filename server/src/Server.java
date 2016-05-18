import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by mso on 16-5-18.
 */
public class Server {
    public static void main(String[] args) {
        new Server();
    }

    public Server() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(8000);
            int clientNo = 1;
            while (true) {
                Socket socket = serverSocket.accept();

                HandleAClient task = new HandleAClient(socket);

                new Thread(task).start();
            }
        } catch (IOException e) {
            System.err.println(e);
        }

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
                    double radius = inputFromClient.readDouble();

                    double area = radius * radius * Math.PI;

                    outputToClient.writeDouble(area);

                }
            }
            catch (IOException e) {
                System.err.println(e);
            }
        }
    }
}
