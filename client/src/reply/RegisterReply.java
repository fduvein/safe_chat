package reply;

/**
 * Created by mso on 16-5-19.
 */
public class RegisterReply extends Reply {
    private boolean success;
    private String kpriC;
    private String kpubS;

    public RegisterReply(String kpriC, String kpubS) {
        this.kpriC = kpriC;
        this.kpubS = kpubS;
    }

    public void decrypt(String reply) {
        // KpubC(Message+KpriS(time))
        // Message show whether success or not
    }

    public boolean isSuccess() {
        return success;
    }
}
