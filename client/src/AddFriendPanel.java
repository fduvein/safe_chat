import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by TanXin on 2016/5/16.
 */
/*
*the panel used to add friends
 */
public class AddFriendPanel extends JPanel {
    private JLabel l1 = new JLabel("Add friend");
    private JTextField id = new JTextField();
    private JButton bt = new JButton("Send Request");
    private JLabel l2 = new JLabel("ID of Friend");
    private JLabel l4 = new JLabel();

    public AddFriendPanel(int x, int y) {
        l4.setBorder(BorderFactory.createEtchedBorder());
        this.setSize(x, y - 50);
        this.setLayout(null);
        l1.setBounds(30, 20, 80, 30);
        l2.setBounds(50, 70, 100, 30);
        l4.setBounds(50, 250, 500, 140);
        id.setBounds(60, 130, 400, 30);
        bt.setBounds(100, 190, 180, 40);
        this.add(bt);
        this.add(id);
        this.add(l1);
        this.add(l2);
        this.add(l4);
        bt.addActionListener(new Add());
    }

    //add friends button
    //send request to server
    private class Add implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            //String friendemail = id.getText();
            //add code here

            //if necessary show some info in l4
            //l4.setText(return infomation);

        }
    }

}
