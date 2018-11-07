package ui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Created by TanXin on 2016/5/21.
 */
public class DRMPanel extends JPanel {
    private JTextField name = new JTextField();
    private File selectedFile;
    public JTextArea l4 = new JTextArea("");
    public JTextField id = new JTextField();

    public DRMPanel() {
        this.setLayout(null);
        JButton buy = new JButton("Buy");
        l4.setLineWrap(true);
        l4.setEditable(false);
        l4.setBorder(BorderFactory.createEtchedBorder());
        JButton request = new JButton("Request");
        JButton choose = new JButton("Choose File");
        JLabel l1 = new JLabel("ID");
        buy.setBounds(100, 80, 150, 30);
        l4.setBounds(100, 140, 460, 100);
        choose.setBounds(100, 260, 200, 30);
        name.setBorder(BorderFactory.createEtchedBorder());
        name.setBounds(330, 260, 240, 30);
        name.setEditable(false);
        l1.setBounds(130, 310, 30, 30);
        id.setBounds(200, 310, 360, 30);
        request.setBounds(220, 370, 200, 30);
        choose.addActionListener(new FChoose());
        request.addActionListener(new FRequest());
        buy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MainFrame.client.buy();
            }
        });
        //request.addActionListener();
        this.add(buy);
        this.add(l4);
        this.add(request);
        this.add(choose);
        this.add(id);
        this.add(l1);
        this.add(name);

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

    public File getFile() {
        return selectedFile;
    }

    private class FRequest implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String i = id.getText();
            if (!i.equals("")) {
                if (selectedFile == null) {
                    l4.setText("Please select a file");
                } else {
                    MainFrame.client.requestFileKey(i, selectedFile);
                }
            } else {
                l4.setText("Please input your Record id");
            }

        }
    }
}