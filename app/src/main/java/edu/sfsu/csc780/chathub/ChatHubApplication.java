package edu.sfsu.csc780.chathub;

import android.app.Application;
import android.speech.tts.TextToSpeech;

import com.facebook.drawee.backends.pipeline.Fresco;

import java.util.Locale;

import edu.sfsu.csc780.chathub.utils.EncryptionHelper;

public class ChatHubApplication extends Application {

    private static EncryptionHelper sEncryptionHelper;
    private TextToSpeech sTextToSpeechObj;
    private static ChatHubApplication sInstance;

    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;

        if(sEncryptionHelper == null) {
            sEncryptionHelper = EncryptionHelper.getEncryptionHelper();
        }
        sTextToSpeechObj=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    sTextToSpeechObj.setLanguage(Locale.UK);
                }
            }
        });
        Fresco.initialize(this);

    }

    public static EncryptionHelper getEncryptionHelper() {
        if(sEncryptionHelper == null){
            sEncryptionHelper = EncryptionHelper.getEncryptionHelper();
        }
        return sEncryptionHelper;
    }

    public TextToSpeech getsTextToSpeechObj(){
        if(sTextToSpeechObj == null){
            sTextToSpeechObj=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if(status != TextToSpeech.ERROR) {
                        sTextToSpeechObj.setLanguage(Locale.UK);
                    }
                }
            });
        }
        return sTextToSpeechObj;
    }

    public static ChatHubApplication getChatHubApplication(){
        return sInstance;
    }
}
