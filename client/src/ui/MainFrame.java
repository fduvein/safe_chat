package ui;


import kernl.Client;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
    JMenuItem it2 = new JMenuItem("Add friend");
    JMenuItem it3 = new JMenuItem("Chat");
    JMenuItem it4 = new JMenuItem("DRM System");
 static Client client;
    public MainFrame() {
        int x = 900, y = 550;
//        it2.setEnabled(false);
//        it3.setEnabled(false);
        m1.add(it1);
        m1.add(it2);
        m1.add(it3);
        m2.add(it4);
        this.setJMenuBar(menubar);
        menubar.add(m1);
        menubar.add(m2);
        final Panel s = new Panel();
        final CardLayout card = new CardLayout();
        s.setLayout(card);
        LoginPanel panel1 = new LoginPanel(x, y, it2, it3,it4);
        AddFriendPanel panel2 = new AddFriendPanel(x, y);
        ChatPanel panel3 = new ChatPanel(x, y);
        s.add("init", new JLabel());
        s.add("login", panel1);
        s.add("addF", panel2);
        s.add("chat", panel3);
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
        s.setBounds(50, 15, x, y);
        this.setLayout(null);
        this.add(s);
        client=new Client();
    }

    public static void main(String[] args) {
        MainFrame frame = new MainFrame();
        frame.setSize(1000, 600);
        frame.setTitle("kernl.Client");
        frame.setLocationRelativeTo(null);

        frame.setVisible(true);
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        frame.setResizable(false);
    }
}
