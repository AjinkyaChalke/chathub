package edu.sfsu.csc780.chathub;

import android.app.Application;

import edu.sfsu.csc780.chathub.utils.EncryptionHelper;

public class ChatHubApplication extends Application {

    private static EncryptionHelper sEncryptionHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        if(sEncryptionHelper == null) {
            sEncryptionHelper = EncryptionHelper.getEncryptionHelper();
        }
    }

    public static EncryptionHelper getEncryptionHelper() {
        if(sEncryptionHelper == null){
            sEncryptionHelper = EncryptionHelper.getEncryptionHelper();
        }
        return sEncryptionHelper;
    }
}
