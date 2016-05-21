import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * Created by Tanxin on 2016/5/16.
 */
/*
*The panel used to login and register
 */
public class LoginPanel extends JPanel {
    public LoginPanel(int x, int y, JMenuItem it2, JMenuItem it3) {
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
    }

    // panel to login
    class LP extends JPanel {
        private String password = null;

        LP() {
            this.setLayout(null);
            JLabel l1 = new JLabel("Login");
            JLabel l2 = new JLabel("ID");
            JLabel l3 = new JLabel("Password");
            JLabel l4 = new JLabel();
            l4.setBorder(BorderFactory.createEtchedBorder());
            JTextField id = new JTextField();
            final JTextArea pass = new JTextArea();
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
                            FileReader fr = null;
                            try {
                                fr = new FileReader(selectedFile);
                                pass.setText(selectedFile.getPath());
                                BufferedReader bf = new BufferedReader(fr);
                                password = bf.readLine();
                                bf.close();
                                fr.close();
                            } catch (Exception e1) {
                                JOptionPane.showMessageDialog(null, "Selected file Error.");
                            }
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
                    //String email=id.getText();


                    //if login successful ,set the menuitem true
                    //if necessary show some info in l4
                    //                        l4.setText(return infomation);
                    //                        it2.setEnabled(true);
                    //                        it3.setEnabled(true);
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
            JLabel l4 = new JLabel();
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
                    //add code here
                    //need to create a pair of keys
//                    String email=id.getText();


                    //if register successful
                    //save the private key as email.key(or email.txt?)
                    //and then tell the user to login next
                    //if necessary show some info in l4
                    //    l4.setText(return infomation);

//                    try{
//                        File file =new File(id.getText()+".key");
//                        //if file doesnt exists, then create it
//                        if(!file.exists()){
//                            file.createNewFile();
//                        }
//                        data=//the string to write
//                        FileWriter fileWritter = new FileWriter(file.getName(),true);
//                        BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
//                        bufferWritter.write(data);
//                        bufferWritter.close();
//
//                    }catch(Exception e1){
//                       JOptionPane.showMessageDialog(null,"Write file error");
//                    }


                }
            });
        }
    }
}
