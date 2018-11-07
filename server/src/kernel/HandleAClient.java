package kernel;

import dao.ServerDao;
import dao.UserDao;
import encryptor.AESEncryptor;
import encryptor.RSAEncryptor;
import encryptor.Util;
import message.*;
import user.Friend;
import user.ServerUser;

import javax.crypto.Mac;
import java.io.*;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class HandleAClient implements Runnable {
    private Socket socket;
    private ServerUser threadUser;
    private Key kcs;
    private final String FILE_PATH = "server/res/DRM/file/poem.txt";
    private final String FILE_KEY_PATH_PREFIX = "server/res/DRM/key/";
    private final String SALE_RECORD_PATH = "server/res/DRM/saleRecord.txt";

    public HandleAClient(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        quit:
        while (true) {
            // income a new message
            // deserialize datagram
            Datagram datagram = null;
            try {
                ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                datagram = (Datagram) objectInputStream.readObject();
            } catch (IOException e) {
                System.err.println("socket error");
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                System.err.println("can not deserialize datagram");
                e.printStackTrace();
            }
            // decrypt massage
            AbstractMessage message = null;
            if (datagram.getMessageEncryptType() == Datagram.MessageEncryptType.AES && kcs != null) {
                try {
                    message = AESEncryptor.decryptMessage(datagram.getCipherMessageBytes(), kcs);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    message = RSAEncryptor.decryptMessage(datagram.getCipherMessageBytes(), ServerDao.getPrivateKey());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // switch message type
            switch (message.getType()) {
                case REGISTER_REQUEST: {
                    handleRegisterRequest(message);
                    break;
                }
                case LOGIN_REQUEST: {
                    handleLoginRequest(message);
                    break;
                }
                case ASK_FRIEND_LIST_REQUEST: {
                    handleFriendListRequest(message);
                    break;
                }
                case FORWARD_MESSAGE: {
                    handleForwardRequest(message);
                    break;
                }
                case FRIEND_REQUEST: {
                    handleFriendRequest(message);
                    break;
                }
                case ACCEPT_FRIEND_RESPONSE: {
                    handleAcceptFriendResponse(message);
                    break;
                }
                case REJECT_FRIEND_RESPONSE: {
                    handleRejectFriendResponse(message);
                    break;
                }
                case BUY_FILE_MESSAGE: {
                    //   System.out.print("a");
                    handleBuyFileRequest(message);
                    break;
                }
                case FILE_KEY_REQUEST: {
                    handleFileKeyRequest(message);
                    break;
                }
                case USER_CLOSE:{
                    //System.out.print("a");
                    break quit;
                }
                default: {

                }
            }
        }
    }

    private void handleFileKeyRequest(AbstractMessage message) {
        String record = ((FileKeyRequest) message).getRecordId();
        String fileID = ((FileKeyRequest) message).getFileId();
        String path = FILE_KEY_PATH_PREFIX + fileID + ".key";
        if (checkRecord(record)) {
            ObjectInputStream privateKeyInputStream = null;
            try {
                privateKeyInputStream = new ObjectInputStream(new FileInputStream(path));
                Key key = (Key) privateKeyInputStream.readObject();
                AbstractMessage reply = new FileKeyResponse(AESEncryptor.getCipherTimeStamp(kcs), key);
                sendMessage(socket, reply, kcs, Datagram.MessageEncryptType.AES);
            } catch (IOException | ClassNotFoundException e) {
                AbstractMessage reply = new FileKeyFail(AESEncryptor.getCipherTimeStamp(kcs));
                sendMessage(socket, reply, kcs, Datagram.MessageEncryptType.AES);
                e.printStackTrace();
            }
        } else {
            AbstractMessage reply = new FileKeyFail(AESEncryptor.getCipherTimeStamp(kcs));
            sendMessage(socket, reply, kcs, Datagram.MessageEncryptType.AES);
            //  System.out.println("not found Record");
        }
    }

    private void handleBuyFileRequest(AbstractMessage message) {
        Key key = AESEncryptor.geneAESKey();
        String rand1 = Util.generateString(8);//file id
        String rand2 = Util.generateString(30);//check code
        try {
            File keyFile = new File(FILE_KEY_PATH_PREFIX + rand1 + ".key");
            if (!keyFile.exists()) {
                keyFile.createNewFile();
            }
            ObjectOutputStream privateKeyOutputStream = new ObjectOutputStream(new FileOutputStream(keyFile));
            privateKeyOutputStream.writeObject(key);
            privateKeyOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] f = Util.file2Byte(new File(FILE_PATH));
        byte[] cipherMessageBytes = AESEncryptor.encrypt(f, key);
        byte[] id = rand1.getBytes();
        byte[] content = Util.arraycat(id, cipherMessageBytes);
        //   System.out.println(rand1);
        Mac mac = null;
        try {
            int a= FILE_PATH.lastIndexOf('.');
            String aa="";
            if(a!=-1){
                aa= FILE_PATH.substring(a, FILE_PATH.length());
            }
            addRecord(rand2);
            mac = Mac.getInstance("HmacSHA256");
            mac.init(kcs);
            byte[] macCode = mac.doFinal(content);
            AbstractMessage reply = new BuyResponse(AESEncryptor.getCipherTimeStamp(kcs), rand2, content, macCode,aa);
            sendMessage(socket, reply, kcs, Datagram.MessageEncryptType.AES);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    private void handleRejectFriendResponse(AbstractMessage message) {
        String senderId = ((RejectFriendResponse) message).getSenderID();
        String receiverId = ((RejectFriendResponse) message).getReceiverID();
        HandleAClient receiverThread = ServerDao.getOnlineUserThreadWithId(receiverId);
        if (receiverThread == null) {
            // offline or not exist
            AbstractMessage reply = new UserOfflineResponse(AESEncryptor.getCipherTimeStamp(kcs), receiverId);
            sendMessage(socket, reply, kcs, Datagram.MessageEncryptType.AES);
        } else {
            // forward the friend message to receiver using its thread
            AbstractMessage forwardMessage = new RejectFriendResponse(AESEncryptor.getCipherTimeStamp(receiverThread.getKcs()), senderId, receiverId);
            sendMessage(receiverThread.getSocket(), forwardMessage, receiverThread.getKcs(), Datagram.MessageEncryptType.AES);
        }
    }

    private void handleAcceptFriendResponse(AbstractMessage message) {
        String senderId = ((AcceptFriendResponse) message).getSenderID();
        String receiverId = ((AcceptFriendResponse) message).getReceiverID();
        HandleAClient receiverThread = ServerDao.getOnlineUserThreadWithId(receiverId);
        if (receiverThread == null) {
            // offline
            AbstractMessage reply = new UserOfflineResponse(AESEncryptor.getCipherTimeStamp(kcs), receiverId);
            sendMessage(socket, reply, kcs, Datagram.MessageEncryptType.AES);
        } else {
            // add friend relation
            UserDao.addFriendRelation(senderId, receiverId);
            // forward the friend message to receiver using its thread
            AbstractMessage forwardMessage = new AcceptFriendResponse(AESEncryptor.getCipherTimeStamp(receiverThread.getKcs()), senderId, receiverId);
            sendMessage(receiverThread.getSocket(), forwardMessage, receiverThread.getKcs(), Datagram.MessageEncryptType.AES);
            // send friend list to sender
            ArrayList<Friend> friendList1 = UserDao.getFriendListWithId(senderId);
            AbstractMessage reply1 = new FriendListResponse(AESEncryptor.getCipherTimeStamp(kcs), friendList1);
            sendMessage(socket, reply1, kcs, Datagram.MessageEncryptType.AES);
            HandleAClient a = ServerDao.getOnlineUserThreadWithId(receiverId);
            ArrayList<Friend> friendList2 = UserDao.getFriendListWithId(receiverId);
            AbstractMessage reply2 = new FriendListResponse(AESEncryptor.getCipherTimeStamp(a.getKcs()), friendList2);
            a.sendMessage(a.getSocket(), reply2, a.getKcs(), Datagram.MessageEncryptType.AES);
        }
    }

    private void handleFriendListRequest(AbstractMessage message) {
        String senderId = ((FriendListRequest) message).getSenderId();
        // send friend list to sender
        // get friend list with user id
        ArrayList<Friend> friendList = UserDao.getFriendListWithId(senderId);
        AbstractMessage reply = new FriendListResponse(AESEncryptor.getCipherTimeStamp(kcs), friendList);
        sendMessage(socket, reply, kcs, Datagram.MessageEncryptType.AES);
    }

    private void handleFriendRequest(AbstractMessage message) {
        String senderId = ((FriendRequest) message).getSenderID();
        String receiverId = ((FriendRequest) message).getReceiverID();
        HandleAClient receiverThread = ServerDao.getOnlineUserThreadWithId(receiverId);
        if (!UserDao.searchUser(receiverId)) {
            AbstractMessage reply = new UserNotExistResponse(AESEncryptor.getCipherTimeStamp(kcs), receiverId);
            sendMessage(socket, reply, kcs, Datagram.MessageEncryptType.AES);
        } else {

            if (receiverThread == null) {
                // offline or not exist
                AbstractMessage reply = new UserOfflineResponse(AESEncryptor.getCipherTimeStamp(kcs), receiverId);
                sendMessage(socket, reply, kcs, Datagram.MessageEncryptType.AES);
            } else {
                // forward the friend message to receiver using its thread
                AbstractMessage forwardMessage = new FriendRequest(AESEncryptor.getCipherTimeStamp(receiverThread.getKcs()), senderId, receiverId);
                sendMessage(receiverThread.getSocket(), forwardMessage, receiverThread.getKcs(), Datagram.MessageEncryptType.AES);
            }
        }
    }

    private void handleForwardRequest(AbstractMessage message) {
        String senderId = ((ForwardMessage) message).getSenderID();
        String receiverId = ((ForwardMessage) message).getReceiverID();
        Datagram datagram = ((ForwardMessage) message).getDatagram();
        HandleAClient receiverThread = ServerDao.getOnlineUserThreadWithId(receiverId);
        if (receiverThread != null) {
            AbstractMessage forwardMessage = new ForwardMessage(AESEncryptor.getCipherTimeStamp(receiverThread.getKcs()), senderId, receiverId, datagram);
            sendMessage(receiverThread.getSocket(), forwardMessage, receiverThread.getKcs(), Datagram.MessageEncryptType.AES);
        } else {
            // receiver offline
            AbstractMessage reply = new UserOfflineResponse(AESEncryptor.getCipherTimeStamp(kcs), receiverId);
            sendMessage(socket, reply, kcs, Datagram.MessageEncryptType.AES);
        }
    }

    private void handleRegisterRequest(AbstractMessage message) {
        Key userKey = ((RegisterRequest) message).getSenderPublicKey();
        // check time stamp
        if (RSAEncryptor.checkTimeStamp(message.getCipherTimeStamp(), userKey)) {
            // check user id
            if (UserDao.getUserWithID(((RegisterRequest) message).getSenderID()) == null) {
                // do not exist in database
                // store user to data base
                UserDao.addUser(new ServerUser(((RegisterRequest) message).getSenderID(), userKey));
                // send message to client
                AbstractMessage reply = new AcceptRegisterResponse(RSAEncryptor.getCipherTimeStamp(ServerDao.getPrivateKey()), ((RegisterRequest) message).getSenderID());
                sendMessage(socket, reply, userKey, Datagram.MessageEncryptType.RSA);
            } else {
                // user id has already existed in server database
                AbstractMessage reply = new RejectRegisterResponse(RSAEncryptor.getCipherTimeStamp(ServerDao.getPrivateKey()), ((RegisterRequest) message).getSenderID());
                sendMessage(socket, reply, userKey, Datagram.MessageEncryptType.RSA);
            }
        } else {
            // time stamp out of date error
            AbstractMessage reply = new RejectRegisterResponse(RSAEncryptor.getCipherTimeStamp(ServerDao.getPrivateKey()), ((RegisterRequest) message).getSenderID());
            sendMessage(socket, reply, userKey, Datagram.MessageEncryptType.RSA);
        }
    }

    private void handleLoginRequest(AbstractMessage message) {
        String userId = ((LoginRequest) message).getSenderID();
        ServerUser user = UserDao.getUserWithID(userId);
        if (user != null) {
            // user exist
            // check time stamp
            Key userKey = user.getPublicKey();
            if (RSAEncryptor.checkTimeStamp(message.getCipherTimeStamp(), userKey)) {
                // valid time stamp
                // login success
                ServerDao.addOnlineUserThread(this);
                // generate kcs
                kcs = AESEncryptor.geneAESKey();
                AbstractMessage reply = new AcceptLoginResponse(RSAEncryptor.getCipherTimeStamp(ServerDao.getPrivateKey()), kcs);
                sendMessage(socket, reply, userKey, Datagram.MessageEncryptType.RSA);
                ArrayList<Friend> friendList = UserDao.getFriendListWithId(userId);
                AbstractMessage r = new FriendListResponse(AESEncryptor.getCipherTimeStamp(kcs), friendList);
                synchronized (this) {
                    try {
                        this.wait(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                sendMessage(socket, r, kcs, Datagram.MessageEncryptType.AES);
                // set current thread user
                threadUser = new ServerUser(userId, userKey);
            } else {
                // invalid time stamp
                // AbstractMessage reply = new RejectLoginResponse(RSAEncryptor.getCipherTimeStamp(ServerDao.getPrivateKey()));
                //  sendMessage(socket, reply, userKey, Datagram.MessageEncryptType.RSA);
            }
        } else {
            // user id does not exist
//            AbstractMessage reply = new RejectLoginResponse(RSAEncryptor.getCipherTimeStamp(ServerDao.getPrivateKey()));
//            sendMessage(socket, reply, userKey, Datagram.MessageEncryptType.RSA);
        }
    }

    public void sendMessage(Socket socket, AbstractMessage message, Key key, Datagram.MessageEncryptType messageEncryptType) {
        byte[] cipherMessageBytes;
        if (messageEncryptType == Datagram.MessageEncryptType.RSA) {
            cipherMessageBytes = RSAEncryptor.encryptMessage(message, key);
        } else {
            cipherMessageBytes = AESEncryptor.encryptMessage(message, key);
        }
        Datagram datagram = new Datagram(cipherMessageBytes, messageEncryptType);
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(datagram);
            objectOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ServerUser getThreadUser() {
        return threadUser;
    }

    public Socket getSocket() {
        return socket;
    }

    public Key getKcs() {
        return kcs;
    }

    public void addRecord(String id) {
        File file = new File(SALE_RECORD_PATH);
        FileWriter fw = null;
        BufferedWriter writer = null;
        try {
            fw = new FileWriter(file);
            writer = new BufferedWriter(fw);
            writer.write(id);
            writer.newLine();//换行
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Write Record error");
        } finally {
            try {
                writer.close();
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private boolean checkRecord(String record) {
        boolean result = false;
        try {
            // read file content from file
            StringBuffer sb = new StringBuffer("");

            FileReader reader = new FileReader(SALE_RECORD_PATH);
            BufferedReader br = new BufferedReader(reader);

            String str = null;

            while ((str = br.readLine()) != null) {
                if (str.equals(record)) {
                    result = true;
                    break;
                }
            }

            br.close();
            reader.close();


        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("DRMRecordFile missing");
        }
        return result;
    }
}
