import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Key;
import java.util.ArrayList;
import java.util.Base64;

/**
 * Created by mso on 16-5-21.
 */
public class UserDataXML {
    private String user_data_file;

    public UserDataXML(String user_data_file) {
        this.user_data_file = user_data_file;
    }

    public void updateXml(ArrayList<User> userList) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory
                    .newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            Element root = document.createElement("userList");
            for (User u: userList) {
                Element user = document.createElement("user");
                Element id = document.createElement("id");
                id.appendChild(document.createTextNode(u.getID()));
                user.appendChild(id);
                Element kpub = document.createElement("kpub");
                kpub.appendChild(document.createTextNode(Base64.getEncoder().encodeToString(u.getKpubC().getEncoded())));
                user.appendChild(kpub);
                Element friendIDList = document.createElement("friendIDList");
                for (String fID: u.getFriendsIDList()) {
                    Element friendID = document.createElement("friendID");
                    friendID.appendChild(document.createTextNode(fID));
                    friendIDList.appendChild(friendID);
                }
                user.appendChild(friendIDList);
                root.appendChild(user);
            }
            document.appendChild(root);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            DOMSource source = new DOMSource(document);
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            PrintWriter pw = new PrintWriter(new FileOutputStream(user_data_file));
            StreamResult result = new StreamResult(pw);
            transformer.transform(source, result);
        } catch (ParserConfigurationException | IllegalArgumentException | FileNotFoundException | TransformerException e) {
            System.err.println(e.getMessage());
        }
    }

    public ArrayList<User> getUserList() {
        ArrayList<User> ret = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory
                    .newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            NodeList userNodeList = document.getChildNodes();
            for (int i = 0; i < userNodeList.getLength(); i++) {
                Node userNode = userNodeList.item(i);
                NodeList userInfo = userNode.getChildNodes();
                Node idNode = userInfo.item(0);
                Node kpubNode = userInfo.item(1);
                Node friendNode = userInfo.item(2);
                NodeList friendNodeList = friendNode.getChildNodes();
                String id = idNode.getTextContent();
                byte[] decodedKey = Base64.getDecoder().decode(kpubNode.getTextContent());
                Key kpub = new SecretKeySpec(decodedKey, 0, decodedKey.length, "RSA");
                User user = new User(id, kpub);
                ArrayList<String> friendList = user.getFriendsIDList();
                for (int j = 0; j < friendNodeList.getLength(); j++) {
                    friendList.add(friendNodeList.item(j).getTextContent());
                }
            }
        } catch (ParserConfigurationException e) {
            System.out.println(e.getMessage());
        }
        return ret;
    }
}
