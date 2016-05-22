package ui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

/**
 * Created by Tanxin on 2016/5/16.
 */
/*
*The panel used to login and register
 */
public class LoginPanel extends JPanel {
    JMenuItem it2;
    JMenuItem it3;
    JMenuItem it4;
    public LoginPanel(int x, int y, JMenuItem t2, JMenuItem t3,JMenuItem t4) {
        this.setSize(x, y);
        LP login = new LP();
        RP register = new RP();
        this.setLayout(null);
        this.add(login);
        this.add(register);
        login.setBounds(5, 0, x / 2 - 20, y - 40);
        register.setBounds(x / 2 + 20, 0, x / 2 - 20, y - 40);
        Border b = BorderFactory.createLineBorder(Color.BLACK);
        login.setBorder(b);
        register.setBorder(b);
        it2=t2;
        it3=t3;
        it4=t4;
    }

    // panel to login
    class LP extends JPanel {
        private File password = null;

        LP() {
            this.setLayout(null);
            JLabel l1 = new JLabel("Login");
            JLabel l2 = new JLabel("ID");
            JLabel l3 = new JLabel("Password");
            JLabel l4 = new JLabel("ID can contain letters,digits and \"_\"");
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
                            password=selectedFile;
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
                    if(checkId(lid)&&(password!=null)){

                        String a=MainFrame.client.login(lid,password);
                        l4.setText(a);
                        if(a.equals("login success")){

                                                    it2.setEnabled(true);
                                                    it3.setEnabled(true);
                            it4.setEnabled(true);
                        }

                    }else{
                        JOptionPane.showMessageDialog(null,"illegal id");
                    }


                }
            });
        }
    }

    // panel to register
    class RP extends JPanel {
        RP() {
            this.setLayout(null);
            JLabel l1 = new JLabel("Register");
            JLabel l2 = new JLabel("ID");
            JLabel l4 = new JLabel("ID can contain letters,digits and \"_\"");
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
                      String rid=id.getText();
                    if(checkId(rid)){
                        l4.setText(MainFrame.client.register(rid));
                    }else{
                        JOptionPane.showMessageDialog(null,"illegal id");
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
