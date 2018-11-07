package dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.Key;

public class ServerDao {
    private static final String SERVER_PUBLIC_KEY_FILE = "client/res/serverKey/public.key";
    private static Key publicKey;
    private static Key kcs;

    public static void init() {
        try {
            // read server public key
            File publicKeyFile = new File(SERVER_PUBLIC_KEY_FILE);
            if (publicKeyFile.exists()) {
                ObjectInputStream publicKeyInputStream = new ObjectInputStream(new FileInputStream(publicKeyFile));
                publicKey = (Key) publicKeyInputStream.readObject();
                publicKeyInputStream.close();
            } else {
                throw new IOException();
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("can not load server public key");
            e.printStackTrace();
        }
    }

    public static Key getPublicKey() {
        return publicKey;
    }

    public static Key getKcs() {
        return kcs;
    }

    public static void setKcs(Key kcs) {
        ServerDao.kcs = kcs;
    }
}
