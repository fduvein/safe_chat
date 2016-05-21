import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
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
                id.setTextContent(u.getID());
                user.appendChild(id);
                Element kpub = document.createElement("kpub");
                kpub.setTextContent(Base64.getEncoder().encodeToString(u.getKpubC().getEncoded()));
                user.appendChild(kpub);
                Element friendIDList = document.createElement("friendIDList");
                for (String fID: u.getFriendsIDList()) {
                    Element friendID = document.createElement("friendID");
                    friendID.setTextContent(fID);
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
        File user_data = new File(user_data_file);
        if (!user_data.exists()) {
            return ret;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory
                    .newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(user_data_file);
            Node root = document.getDocumentElement();
            NodeList userNodeList = root.getChildNodes();
            for (int i = 0; i < userNodeList.getLength(); i++) {
                Node userNode = userNodeList.item(i);
                String id = null;
                Key kpub = null;
                ArrayList<String> friendList = new ArrayList<>();
                for (Node node = userNode.getFirstChild(); node != null; node = node.getNextSibling()) {
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        if ("id".equals(node.getNodeName())) {
                            System.out.println(node.getFirstChild().getNodeValue());
                            id = node.getFirstChild().getNodeValue();
                        }
                    }
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        if ("kpub".equals(node.getNodeName())) {
                            System.out.println(node.getFirstChild().getNodeValue());
                            byte[] decodedKey = Base64.getDecoder().decode(node.getFirstChild().getNodeValue());
                            kpub = new SecretKeySpec(decodedKey, 0, decodedKey.length, "RSA");
                        }
                    }
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        if ("friendIDList".equals(node.getNodeName())) {
                            if (node.hasChildNodes()) {
                                for (Node childNode = node.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
                                    if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                                        if ("friendID".equals(childNode.getNodeName())) {
                                            System.out.println(childNode.getFirstChild().getNodeValue());
                                            friendList.add(childNode.getFirstChild().getNodeValue());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                User user = new User(id, kpub);
                user.setFriendsIDList(friendList);
                ret.add(user);
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return ret;
    }
}
