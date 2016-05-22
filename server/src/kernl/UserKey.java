package kernl;

import key.MyRSAKey;

import java.io.*;
import java.security.Key;
import java.security.KeyPair;

/**
 * Created by mso on 16-5-22.
 */
public class UserKey {
    private Key publicKey;

    public UserKey(String public_key_file) {
        try {
            File publicKeyFile = new File(public_key_file);
            if (publicKeyFile.exists()) {
                // read server public-private key from file
                ObjectInputStream publicKeyInputStream = new ObjectInputStream(new FileInputStream(publicKeyFile));
                publicKey = (Key)publicKeyInputStream.readObject();
                publicKeyInputStream.close();
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Can not get user public key");
        }
    }

    public UserKey(Key publicKey) {
        this.publicKey = publicKey;
    }

    public void saveKeyTo(String public_key_file) {
        try {
            File publicKeyFile = new File(public_key_file);
            if (!publicKeyFile.exists()) {
                publicKeyFile.createNewFile();
            }
            ObjectOutputStream publicKeyOutputStream = new ObjectOutputStream(new FileOutputStream(publicKeyFile));
            publicKeyOutputStream.writeObject(publicKey);
            publicKeyOutputStream.close();
        } catch (IOException e) {
            System.err.println("Can not save user public key");
        }
    }



    public Key getPublic() {
        return publicKey;
    }

}
