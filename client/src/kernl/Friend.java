package kernl;

import java.security.Key;

/**
 * Created by mso on 16-5-22.
 */
public class Friend {
    private String id;
    private Key publicKey;
    private Key sessionKey;

    public Friend(String id, Key publicKey) {
        this.id = id;
        this.publicKey = publicKey;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Key getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(Key publicKey) {
        this.publicKey = publicKey;
    }

    public Key getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(Key sessionKey) {
        this.sessionKey = sessionKey;
    }
}
