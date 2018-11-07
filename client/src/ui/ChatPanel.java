package ui;

import sun.applet.Main;
import user.Friend;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by Tanxin on 2016/5/16.
 */
/*
*The panel used to chat with friend and send file
 */
public class ChatPanel extends JPanel {
    /**
     * object about gui
     **/
    private JList list = new JList();
    private JTextArea chat = new JTextArea();
    private JTextArea message = new JTextArea();
    public JTextArea l4 = new JTextArea();
    private JButton send = new JButton("Send message");
    private JButton choose = new JButton("choose File");
    private JButton file = new JButton("Send File");
    private JTextField name = new JTextField();
    private JLabel l1 = new JLabel("U means you, H means your friend");

    /**
     * object about logic
     **/
    private File selectedFile = null;//the file to send
    public Vector<Friend> friends = new Vector<>();//friendsList
    private ArrayList<MessageList> messageList = new ArrayList();//messageList,a messageList object record the messages between the user and one of its friends
    private String current;//the current friend you are chatting with

    public ChatPanel(int x, int y) {
        this.setLayout(null);
        l4.setLineWrap(true);
        l4.setEditable(false);
        list.addListSelectionListener(new CFriend());
        //the code below is about gui
        JScrollPane js1 = new JScrollPane(list);
        l1.setBounds(250, 0, 400, 20);
        js1.setBounds(30, 20, 200, 230);
        JScrollPane js3 = new JScrollPane(chat);
        js3.setBounds(250, 20, 610, 280);
        l4.setBounds(30, 270, 200, 200);
        l4.setBorder(BorderFactory.createEtchedBorder());
        message.setLineWrap(true);
        JScrollPane js2 = new JScrollPane(message);
        js2.setBounds(250, 320, 400, 150);
        send.setBounds(670, 320, 200, 30);
        choose.setBounds(670, 360, 200, 30);
        file.setBounds(670, 400, 200, 30);
        name.setBorder(BorderFactory.createEtchedBorder());
        name.setBounds(670, 440, 200, 30);
        send.addActionListener(new SMessage());
        file.addActionListener(new SFile());
        choose.addActionListener(new FChoose());
        chat.setEditable(false);
        this.add(js2);
        this.add(send);
        this.add(choose);
        this.add(file);
        this.add(js1);
        this.add(js3);
        this.add(l4);
        this.add(name);
        this.add(l1);
        //gui end
    }

    //when you selected another friend to talk,load the message before and show it
    private void loadMessage(String id) {
        for (int i = 0; i < messageList.size(); i++) {
            if (messageList.get(i).id.equals(id)) {
                chat.setText(messageList.get(i).toString());
                break;
            }
        }

    }

    public void loadFriend() {
        list.setListData(friends);
    }

    public void addFriend(Friend friend) {
        for (int i = 0; i < friends.size(); i++) {
            if (friends.get(i).getId().equals(friend.getId())) {
                return;
            }
        }
        friends.add(friend);
        messageList.add(new MessageList(friend.getId()));
    }

    //choose a file
    private class FChoose implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser jc = new JFileChooser();
            jc.setCurrentDirectory(new File("."));
            jc.setVisible(true);
            jc.setMultiSelectionEnabled(false);
            int result = jc.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedFile = jc.getSelectedFile();
                if (selectedFile.exists()) {
                    name.setText(selectedFile.getPath());
                } else {
                    JOptionPane.showMessageDialog(null, "You did not select the right file.");
                }
            }
        }
    }

    //The Button to send the selected file
    private class SFile implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (selectedFile != null) {
                try {
                    MainFrame.client.sendFile(current, selectedFile);
                } catch (NoSuchAlgorithmException | InvalidKeyException e1) {
                    e1.printStackTrace();
                    l4.setText("Encrypt error");
                }
            } else {
                l4.setText("Please choose a file to send");
            }
        }
    }

    //The Button to send the input message

    private class SMessage implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (current != null) {
                MainFrame.client.chat(current, message.getText());
                message.setText("");
            } else {
                l4.setText("Please choose a friend to chat");
            }
        }
    }

    //The List to choose friend to chat with
    private class CFriend implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            {
                String a = ((JList) e.getSource()).getSelectedValue().toString();
                // MainFrame.client.sendSessionKey(a);
                for (int i = 0; i < friends.size(); i++) {
                    if (a.equals(friends.get(i).getId())) {
                        current = friends.get(i).getId();
                        break;
                    }
                }
                loadMessage(a);
            }
        }
    }

    public void addSendMessage(String id, String message) {
        for (int i = 0; i < messageList.size(); i++) {
            if (messageList.get(i).id.equals(id)) {
                messageList.get(i).sendMessage(message);
            }
        }
        if (id.equals(current)) {
            loadMessage(id);
        }
    }

    public void addReceiveMessage(String id, String message) {
        for (int i = 0; i < messageList.size(); i++) {
            if (messageList.get(i).id.equals(id)) {
                messageList.get(i).receiveMessage(message);
            }
        }
        if (id.equals(current)) {
            loadMessage(id);
        }
    }

}
