package message;

/**
 * Created by Administrator on 2016/5/24.
 */
public class FileKeyRequest extends AbstractMessage {
    String recordId;
    String fileId;

    public FileKeyRequest(byte[] cipherTimeStamp, String recordId, String fileId) {
        super(Type.FILE_KEY_REQUEST, cipherTimeStamp);
        this.recordId = recordId;
        this.fileId = fileId;
    }
    public String getRecordId(){
        return recordId;
    }
    public String getFileId(){
        return fileId;
    }

}
