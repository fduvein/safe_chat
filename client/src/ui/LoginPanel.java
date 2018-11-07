package ui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Created by Tanxin on 2016/5/16.
 */
/*
*The panel used to login and register
 */
public class LoginPanel extends JPanel {

    public LP login = new LP();
    public RP register = new RP();

    public LoginPanel(int x, int y) {
        this.setSize(x, y);

        this.setLayout(null);
        this.add(login);
        this.add(register);
        login.setBounds(5, 0, x / 2 - 20, y - 40);
        register.setBounds(x / 2 + 20, 0, x / 2 - 20, y - 40);
        Border b = BorderFactory.createLineBorder(Color.BLACK);
        login.setBorder(b);
        register.setBorder(b);
    }

    // panel to login
    public class LP extends JPanel {
        private File password = null;
        public JLabel l4 = new JLabel("ID can contain letters,digits and \"_\"");

        LP() {
            this.setLayout(null);
            JLabel l1 = new JLabel("Login");
            JLabel l2 = new JLabel("ID");
            JLabel l3 = new JLabel("Password");

            l4.setBorder(BorderFactory.createEtchedBorder());
            JTextField id = new JTextField();
            JTextArea pass = new JTextArea();
            pass.setEditable(false);
            l1.setBounds(20, 20, 300, 30);
            l2.setBounds(40, 90, 60, 20);
            l3.setBounds(40, 140, 60, 20);
            l4.setBounds(20, 230, 380, 150);
            id.setBounds(110, 90, 280, 20);
            pass.setBounds(110, 140, 280, 20);
            JButton choose = new JButton("Choose password file");
            JButton bt = new JButton("Login");
            bt.setBounds(150, 400, 80, 30);
            choose.setBounds(120, 180, 200, 30);
            this.add(l1);
            this.add(l2);
            this.add(l3);
            this.add(l4);
            this.add(id);
            this.add(pass);
            this.add(bt);
            this.add(choose);
            //choose a file including the password
            //read the file and save the content as key
            choose.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JFileChooser jc = new JFileChooser();
                    jc.setCurrentDirectory(new File("."));
                    jc.setVisible(true);
                    jc.setMultiSelectionEnabled(false);
                    int result = jc.showOpenDialog(null);
                    File selectedFile;
                    if (result == JFileChooser.APPROVE_OPTION) {
                        selectedFile = jc.getSelectedFile();
                        if (selectedFile.exists()) {
                            password = selectedFile;
                            pass.setText(password.getPath());
                        } else {
                            JOptionPane.showMessageDialog(null, "You did not select the right file.");
                        }
                    }
                }
            });
            //The login button send login message to server
            bt.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    //add code here
                    // if(password!=null) //to check whether the password is read
                    String lid = id.getText();
                    if (password == null) {
                        JOptionPane.showMessageDialog(null, "illegal password");
                    } else {
                        if (checkId(lid)) {
                            MainFrame.client.login(lid, password.getPath());
                        } else {
                            JOptionPane.showMessageDialog(null, "illegal id");
                        }
                    }
                }
            });
        }
    }

    // panel to register
    public class RP extends JPanel {
        public JLabel l4 = new JLabel("ID can contain letters,digits and \"_\"");

        RP() {
            this.setLayout(null);
            JLabel l1 = new JLabel("Register");
            JLabel l2 = new JLabel("ID");
            l4.setBorder(BorderFactory.createEtchedBorder());
            JTextField id = new JTextField();
            l1.setBounds(20, 20, 300, 30);
            l2.setBounds(40, 90, 60, 20);
            l4.setBounds(20, 140, 380, 180);
            id.setBounds(110, 90, 280, 20);
            JButton bt = new JButton("Register");
            bt.setBounds(150, 350, 120, 30);
            this.add(l1);
            this.add(l2);
            this.add(l4);
            this.add(id);
            this.add(bt);
            //The register button send register message to server
            bt.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String rid = id.getText();
                    if (checkId(rid)) {
                        MainFrame.client.register(rid);
                    } else {
                        JOptionPane.showMessageDialog(null, "illegal id");
                    }
                }
            });
        }
    }

    private boolean checkId(String id) {
        if (id.length() > 15 || id.length() == 0) {
            return false;
        }
        for (int i = 0; i < id.length(); i++) {
            int a = id.charAt(i);
            if (!((a > 47 && a < 58) || (a == 95) || (a > 96 && a < 123) || (a > 64 && a < 91))) {
                return false;
            }
        }
        return true;
    }

}
