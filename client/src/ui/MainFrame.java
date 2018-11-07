package ui;


import kernel.Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Created by TanXin on 2016/5/16.
 */
/*
*The ui.MainFrame in the ISProject
 */
public class MainFrame extends JFrame {
    JMenuBar menubar = new JMenuBar();
    JMenu m1 = new JMenu("Chat");
    JMenu m2 = new JMenu("DRM");
    JMenuItem it1 = new JMenuItem("Login/Register");
    public static JMenuItem it2 = new JMenuItem("Add friend");
    public static JMenuItem it3 = new JMenuItem("Chat");
    public static JMenuItem it4 = new JMenuItem("DRM System");
    static int x = 900, y = 550;
    public static LoginPanel panel1 = new LoginPanel(x, y);
    public static AddFriendPanel panel2 = new AddFriendPanel(x, y);
    public static ChatPanel panel3 = new ChatPanel(x, y);
    public static DRMPanel panel4 = new DRMPanel();
    static Client client;
    public static CardLayout card = new CardLayout();
    public static Panel s = new Panel();

    public MainFrame() {

        it2.setEnabled(false);
        it3.setEnabled(false);
        it4.setEnabled(false);
        m1.add(it1);
        m1.add(it2);
        m1.add(it3);
        m2.add(it4);
        this.setJMenuBar(menubar);
        menubar.add(m1);
        menubar.add(m2);
        s.setLayout(card);
        s.add("init", new JLabel());
        s.add("login", panel1);
        s.add("addF", panel2);
        s.add("chat", panel3);
        s.add("DRM", panel4);
        it1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                card.show(s, "login");
            }
        });
        it2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                card.show(s, "addF");
            }
        });
        it3.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                card.show(s, "chat");
            }
        });
        it4.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                card.show(s, "DRM");
            }
        });
        s.setBounds(50, 15, x, y);
        this.setLayout(null);
        this.add(s);
        //panel3 is chatPanel
        client = new Client();
    }

    public static void main(String[] args) {
        MainFrame frame = new MainFrame();
        frame.setSize(1000, 600);
        frame.setTitle("kernl.Client");
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {

                client.close();

                super.windowClosed(e);
            }
        });

    }
}
