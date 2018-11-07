package dao;

import encryptor.RSAEncryptor;
import kernel.HandleAClient;

import java.io.*;
import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;

public class ServerDao {
    private static final String SERVER_PUBLIC_KEY_PATH = "server/res/public.key";
    private static final String SERVER_PRIVATE_KEY_PATH = "server/res/private.key";

    private static Key publicKey;
    private static Key privateKey;
    private static ArrayList<HandleAClient> onlineUserThreadList;

    public static void init() {
        onlineUserThreadList = new ArrayList<>();
        try {
            File publicKeyFile = new File(SERVER_PUBLIC_KEY_PATH);
            File privateKeyFile = new File(SERVER_PRIVATE_KEY_PATH);
            if (publicKeyFile.exists() && privateKeyFile.exists()) {
                // read server public-private key from file
                ObjectInputStream publicKeyInputStream = new ObjectInputStream(new FileInputStream(publicKeyFile));
                ObjectInputStream privateKeyInputStream = new ObjectInputStream(new FileInputStream(privateKeyFile));
                publicKey = (PublicKey) publicKeyInputStream.readObject();
                privateKey = (PrivateKey) privateKeyInputStream.readObject();
                publicKeyInputStream.close();
                privateKeyInputStream.close();
            } else {
                // generate a new pair of public-private key
                KeyPair keyPair = RSAEncryptor.geneRSAKeyPair();
                publicKey = keyPair.getPublic();
                privateKey = keyPair.getPrivate();
                // save it to local
                ObjectOutputStream publicKeyOutputStream = new ObjectOutputStream(new FileOutputStream(publicKeyFile));
                ObjectOutputStream privateKeyOutputStream = new ObjectOutputStream(new FileOutputStream(privateKeyFile));
                publicKeyOutputStream.writeObject(publicKey);
                privateKeyOutputStream.writeObject(privateKey);
                publicKeyOutputStream.close();
                privateKeyOutputStream.close();
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("can not generate key pair for server");
        }
    }

    public static Key getPrivateKey() {
        return privateKey;
    }

    public static void addOnlineUserThread(HandleAClient handleAClient) {
        onlineUserThreadList.add(handleAClient);
        System.out.println(onlineUserThreadList.size());
    }

    public static HandleAClient getOnlineUserThreadWithId(String userId) {
        for (HandleAClient handleAClient: onlineUserThreadList) {
            if (handleAClient.getThreadUser().getId().equals(userId)) {
                return handleAClient;
            }
        }
        return null;
    }
}
