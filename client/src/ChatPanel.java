import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.io.*;
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
    private JLabel l4 = new JLabel();
    private JButton send = new JButton("Send Message");
    private JButton choose = new JButton("choose File");
    private JButton file = new JButton("Send File");
    private JTextField name = new JTextField();
    private JLabel l1 = new JLabel("U means you, H means your friend");

    /**
     * object about logic
     **/
    private File selectedFile = null;//the file to send
    public Vector<Friend> friends = new Vector();//friendsList
    private ArrayList<MessageList> messageList = new ArrayList();//messageList,a messageList object record the messages between the user and one of its friends
    private Friend current;//the current friend you are chatting with

    public ChatPanel(int x, int y) {
        this.setLayout(null);
        loadFriend();
        list.setListData(friends);
        list.addListSelectionListener(new CFriend());
//        These are test code
//        messageList.get(1).sendMessage("Hello");
//        messageList.get(1).sendMessage("I am hero");
//        messageList.get(1).receiveMessage("Hello");
//        messageList.get(1).sendMessage("What's your name");
//        messageList.get(1).receiveMessage("I am bob");
//        messageList.get(1).receiveMessage("I am 18");
//        addSendMessage(new Friend("aa","aa").id,"Hello");
//        addReceiveMessage(new Friend("aa","aa").id,"Hi");
//        test end

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

    //load the friendList from a file
    private void loadFriend() {
        File file = new File("friendList.txt");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            int i;
            while ((tempString = reader.readLine()) != null) {
                i = tempString.indexOf(' ');
                friends.add(new Friend(tempString.substring(0, i), tempString.substring(i, tempString.length())));
                messageList.add(new MessageList(tempString.substring(0, i)));
            }
            reader.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Readind Friend List fails");
        }
    }

    private class Friend {
        String id;
        String key;

        Friend(String a, String b) {
            id = a;
            key = b;
        }

        @Override
        public String toString() {
            return id;
        }
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
            //current:the friend which the message is sent
            //selectedFile:the file to send
            //check whether the file is null
            //if send successfully set the file=null
            //inform the user if necessary
            //l4 is a info JLable
            //l4.setText(m);
        }
    }

    //The Button to send the input message

    private class SMessage implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            //current:the friend which the message is sent
            //String m=message.getText();:the message to send
           // if send successfully add the message to list and reload the messageList
             //addSendMessage(current.id,m);
            //loadMessage(current.id);
            //inform the user if necessary
            //l4 is a info JLable
            //l4.setText(m);
            //What's more
            // if receive a message add the message to list
            // addReceiveMessage(id,m);
           }
    }

    //The List to choose friend to chat with
    private class CFriend implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            {
                String a = ((JList) e.getSource()).getSelectedValue().toString();
                for (int i = 0; i < friends.size(); i++) {
                    if (a.equals(friends.get(i).id)) {
                        current = friends.get(i);
                        break;
                    }
                }
                loadMessage(a);
            }
        }
    }

    private void addSendMessage(String id, String message) {
        for (int i = 0; i < messageList.size(); i++) {
            if (messageList.get(i).id.equals(id)) {
                messageList.get(i).sendMessage(message);
            }
        }
    }

    private void addReceiveMessage(String id, String message) {
        for (int i = 0; i < messageList.size(); i++) {
            if (messageList.get(i).id.equals(id)) {
                messageList.get(i).receiveMessage(message);
            }
        }
    }

}
