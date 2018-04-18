package edu.sfsu.csc780.chathub.utils;

import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import edu.sfsu.csc780.chathub.CodeablePreferences;

public class EncryptionHelper {
    private static String AES_ALGORITHM = "AES";
    private static String AES_TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String  TAG = "EncryptionHelper";
    private static SecretKeySpec secretKey;
    private static byte[] key;
    private static EncryptionHelper sEncryptionHelper;

    public static EncryptionHelper getEncryptionHelper() {
        if(sEncryptionHelper == null){
            sEncryptionHelper = new EncryptionHelper();
        }
        return sEncryptionHelper;
    }

    public static EncryptionHelper getEncryptionHelper(String secret) {
        if(sEncryptionHelper == null){
            sEncryptionHelper = new EncryptionHelper(secret);
        }
        return sEncryptionHelper;
    }

    private EncryptionHelper(String secret){
        setKey(secret);
    }

    private EncryptionHelper(){
        setKey(CodeablePreferences.SECRET);
    }



    public void setKey(String myKey)
    {
        MessageDigest sha;
        try {
            key = myKey.getBytes("UTF-8");
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, AES_ALGORITHM);
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public String encrypt(String strToEncrypt)
    {
        try
        {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] inputBytes = cipher.doFinal(strToEncrypt.getBytes());
            return Base64.encodeToString(inputBytes,Base64.NO_PADDING);
        }
        catch (Exception e)
        {
            System.out.println("Error while encrypting: " + e.toString());
        }
        return null;
    }

    public String decrypt(String strToDecrypt/*, String secret*/)
    {
        try
        {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] inputBytes = cipher.doFinal(Base64.decode(strToDecrypt, Base64.NO_PADDING));
            return new String(inputBytes);
        }
        catch (Exception e)
        {
            Log.d(TAG,"Error while decrypting: " + e.toString());
        }
        return null;
    }
}
