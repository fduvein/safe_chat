/**
 * Created by mso on 16-5-19.
 */
public class User {
    private String userID;
    private String kpubC;
    private String kpriC;

    public User(String userID) {
        this.userID = userID;

        // generate client public-private key
        geneKpubC();
    }

    private void geneKpubC() {
    }

    public String getKpubC() {
        return kpubC;
    }

    public String getKpriC() {
        return kpriC;
    }
}
