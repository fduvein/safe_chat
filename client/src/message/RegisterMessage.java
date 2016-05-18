package message;

/**
 * Created by mso on 16-5-18.
 */
public class RegisterMessage extends Message{
    private String userID;
    private String kpubC;
    private String kpriC;
    private String kpubS;

    public RegisterMessage(String userID, String kpubC, String kpriC, String kpubS) {
        this.userID = userID;
        this.kpubC = kpubC;
        this.kpriC = kpriC;
        this.kpubS = kpubS;
    }

    public String encrypt() {
        // KpubS("Register"+id+kpubC+KpriC(time))
        return null;
    }

}
