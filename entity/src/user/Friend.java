package user;

import java.security.Key;

public class Friend extends User {
    private Key publicKey;
    private Key sessionKey;
    public Friend(String id, Key publicKey) {
        super(id);
        this.publicKey = publicKey;
    }

    public Key getPublicKey() {
        return publicKey;
    }

    public Key getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(Key sessionKey) {
        this.sessionKey = sessionKey;
    }

    @Override
    public String toString() {
        return getId();
    }
}
