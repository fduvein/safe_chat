import java.io.*;
import java.security.Key;
import java.security.KeyPair;

/**
 * Created by mso on 16-5-21.
 */
public class ServerKey {
    private Key publicKey;
    private Key privateKey;

    public ServerKey(String public_key_file, String private_key_file) {
        try {
            File publicKeyFile = new File(public_key_file);
            File privateKeyFile = new File(private_key_file);
            if (publicKeyFile.exists() && privateKeyFile.exists()) {
                // read server public-private key from file
                ObjectInputStream publicKeyInputStream = new ObjectInputStream(new FileInputStream(publicKeyFile));
                ObjectInputStream privateKeyInputStream = new ObjectInputStream(new FileInputStream(privateKeyFile));
                publicKey = (Key)publicKeyInputStream.readObject();
                privateKey = (Key)privateKeyInputStream.readObject();
                publicKeyInputStream.close();
                privateKeyInputStream.close();
            } else {
                // generate a new pair of public-private key
                KeyPair keyPair = RSAKey.geneKeyPair();
                publicKey = keyPair.getPublic();
                privateKey = keyPair.getPrivate();
                ObjectOutputStream publicKeyOutputStream = new ObjectOutputStream(new FileOutputStream(publicKeyFile));
                ObjectOutputStream privateKeyOutputStream = new ObjectOutputStream(new FileOutputStream(privateKeyFile));
                publicKeyOutputStream.writeObject(publicKey);
                privateKeyOutputStream.writeObject(privateKey);
                publicKeyOutputStream.close();
                privateKeyOutputStream.close();
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Can not generate key pair for server");
        }
    }

    public Key getPublic() {
        return publicKey;
    }

    public Key getPrivate() {
        return privateKey;
    }
}
