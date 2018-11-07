package kernel;

import dao.ServerDao;
import dao.UserDao;

import javax.swing.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static javax.swing.JFrame.EXIT_ON_CLOSE;

public class Server {
    private final int PORT = 8000;

    public Server() {
        init();
    }

    private void init() {
        ServerDao.init();
        UserDao.init();
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

    public void close() {
    }


    public static void main(String[] args) {


        JFrame frame = new JFrame();
        frame.setSize(300, 200);
        frame.setTitle("kernl.Server");
        frame.setLocationRelativeTo(null);
        frame.add(new JLabel("Server is running"));
        frame.setVisible(true);
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        frame.setResizable(false);
        new Server().start();
    }
}
