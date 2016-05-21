import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;

/**
 * Created by mso on 16-5-20.
 */
public class RSAKey {
    public static KeyPair geneKeyPair() {
        // generate public-private key for server
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            return keyPair;
        } catch (NoSuchAlgorithmException e) {
            System.err.println("no such algorithm");
        }
        return null;
    }

    public static byte[] decrypt(byte[] encryptMessageBytes, Key key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] messageBytes = cipher.doFinal(encryptMessageBytes);
        return messageBytes;
    }

    public static byte[] encrypt(byte[] messageBytes, Key key) throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptMessageBytes = cipher.doFinal(messageBytes);
        return encryptMessageBytes;
    }
}
