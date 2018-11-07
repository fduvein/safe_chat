package message;

import java.security.Key;

/**
 * Created by Administrator on 2016/5/24.
 */
public class FileKeyResponse extends AbstractMessage {
    private Key key;
    public FileKeyResponse(byte[] cipherTimeStamp, Key key) {
        super(Type.FILE_KEY_RESPONSE, cipherTimeStamp);
        this.key = key;
    }
    public Key getKey(){
        return key;
    }

}
