package message;

/**
 * Created by Administrator on 2016/5/24.
 */
public class FileKeyFail extends AbstractMessage {

    public FileKeyFail(byte[] cipherTimeStamp) {
        super(Type.FILE_KEY_FAIL, cipherTimeStamp);
    }

}
