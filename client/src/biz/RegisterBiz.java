package biz;

import action.AskKpubSAction;
import action.GeneKpubCAction;
import message.Message;
import message.RegisterMessage;
import reply.RegisterReply;
import reply.Reply;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by mso on 16-5-18.
 */
public class RegisterBiz {
    private Socket socket;
    private String userID;
    private DataOutputStream toServer;
    private DataInputStream fromServer;

    public RegisterBiz(String userID, Socket socket) {
        this.socket = socket;
        this.userID = userID;
        try {
            this.toServer = new DataOutputStream(socket.getOutputStream());
            this.fromServer = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void perform() {
        // generate client public-private key
        GeneKpubCAction geneKpubCAction = new GeneKpubCAction();
        String kpubC = geneKpubCAction.getKpubC();
        String kpriC = geneKpubCAction.getKpriC();

        // ask public key of server
        AskKpubSAction askKpubSAction = new AskKpubSAction();
        String kpubS = askKpubSAction.getKpubS();

        // construct register message
        RegisterMessage registerMessage = new RegisterMessage(userID, kpubC, kpriC, kpubS);
        String encryptMessage = registerMessage.encrypt();

        try {
            // send register message to server
            toServer.writeUTF(encryptMessage);
            toServer.flush();

            // get reply from server
            String encryptReply = fromServer.readUTF();
            RegisterReply registerReply = new RegisterReply(kpriC, kpubS);
            registerReply.decrypt(encryptReply);

            // check whether success or not
            if (registerReply.isSuccess()) {

            } else {

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
