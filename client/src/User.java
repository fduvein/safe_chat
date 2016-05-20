import java.security.Key;
import java.security.KeyPair;

/**
 * Created by mso on 16-5-19.
 */
public class User {
    private String userID;
    private Key kpubC;
    private Key kpriC;

    public User(String userID) {
        this.userID = userID;
        // generate client public-private key
        KeyPair keyPair = KeyGene.geneKeyPair();
        kpubC = keyPair.getPublic();
        kpriC = keyPair.getPrivate();
    }

    public Key getKpubC() {
        return kpubC;
    }

    public Key getKpriC() {
        return kpriC;
    }
}
