package kernl;

import key.MyRSAKey;

import java.io.*;
import java.security.Key;
import java.security.KeyPair;

/**
 * Created by mso on 16-5-22.
 */
public class ClientKey {
    private Key privateKey;

    public ClientKey(File private_key_file) {
        try {
            File privateKeyFile = private_key_file;
            if (privateKeyFile.exists()) {
                // read client public-private key from file
                ObjectInputStream privateKeyInputStream = new ObjectInputStream(new FileInputStream(privateKeyFile));
                privateKey = (Key)privateKeyInputStream.readObject();
                privateKeyInputStream.close();
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Can not read private key of user");
        }
    }

    public Key getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(Key privateKey) {
        this.privateKey = privateKey;
    }

}
