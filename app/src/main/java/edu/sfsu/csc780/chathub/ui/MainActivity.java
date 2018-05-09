/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.sfsu.csc780.chathub.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Locale;

import edu.sfsu.csc780.chathub.ChatHeadService;
import edu.sfsu.csc780.chathub.ChatHubApplication;
import edu.sfsu.csc780.chathub.CodeablePreferences;
import edu.sfsu.csc780.chathub.ImageUtil;
import edu.sfsu.csc780.chathub.LocationUtils;
import edu.sfsu.csc780.chathub.MapLoader;
import edu.sfsu.csc780.chathub.R;
import edu.sfsu.csc780.chathub.model.ChatMessage;
import edu.sfsu.csc780.chathub.utils.Helpers;
import edu.sfsu.csc780.chathub.utils.MessageUtil;

import static edu.sfsu.csc780.chathub.ImageUtil.saveCustomFile;
import static edu.sfsu.csc780.chathub.ImageUtil.savePhotoImage;
import static edu.sfsu.csc780.chathub.ImageUtil.scaleImage;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_IMAGE = 1;
    private static final int REQ_CODE_SPEECH_INPUT = 2;
    private ImageButton mImageButton;
    private static final String TAG = "MainActivity";
    public static final String MESSAGES_CHILD = "messages";
    private static final int REQUEST_INVITE = 1;
    public static final String ANONYMOUS = "anonymous";
    private String mUsername;
    private String mPhotoUrl;
    private SharedPreferences mSharedPreferences;
    private GoogleApiClient mGoogleApiClient;

    private ImageButton mSendButton;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private EditText mMessageEditText;

    // Firebase instance variables
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    private FirebaseRecyclerAdapter<ChatMessage, MessageUtil.MessageViewHolder> mFirebaseAdapter;
    private ImageButton mLocationButton;
    private Context mContext;
    private ImageView mImageView;
    private ImageButton mSpeechText;

    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_main);

        initialize();
        initializeImageButton();

        mLocationButton = findViewById(R.id.locationButton);
        mLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadMap();
            }
        });

        stopChatHeadService();

    }

    private void initializeImageButton() {
        mImageButton = findViewById(R.id.shareImageButton);
        if(Build.VERSION.SDK_INT >= 19){
            mImageButton.setOnClickListener(new View.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                @Override
                public void onClick(View v) {
                pickImage();
                }
            });
            mImageButton.setVisibility(View.VISIBLE);
        }
        else {
            mImageButton.setVisibility(View.GONE);
        }
    }

    private void stopChatHeadService() {
        stopService(new Intent(MainActivity.this, ChatHeadService.class));
    }

    private void initialize() {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // Set default username is anonymous.
        mUsername = ANONYMOUS;
        //Initialize Auth
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        if (mUser == null) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else {
            mUsername = mUser.getDisplayName();
            if (mUser.getPhotoUrl() != null) {
                mPhotoUrl = mUser.getPhotoUrl().toString();
            }
        }

        mImageView =  findViewById(R.id.backgroundImage);
        // Initialize ProgressBar and RecyclerView.
        mProgressBar = findViewById(R.id.progressBar);
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        Helpers helpers = new Helpers(this, mProgressBar, mImageView);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, helpers )
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();

        mMessageRecyclerView = findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);


        mFirebaseAdapter = MessageUtil.getFirebaseAdapter(this,
                this,
                helpers,
                mLinearLayoutManager,
                mMessageRecyclerView);
        mMessageRecyclerView.setAdapter(mFirebaseAdapter);

        mMessageEditText = findViewById(R.id.messageEditText);
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(CodeablePreferences.MSG_LENGTH_LIMIT)});
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mSendButton = findViewById(R.id.sendButton);
        mSendButton.setEnabled(false);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessageOnClick(false);
            }
        });

        mSendButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                sendMessageOnClick(true);
                return false;
            }
        });

        mSpeechText = findViewById(R.id.speech_input);
        mSpeechText.setVisibility(View.VISIBLE);
        mSpeechText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptSpeechInput();
            }
        });

    }

    private void sendMessageOnClick(boolean animateBackgroundHear){
        // Send messages on click.
        mMessageRecyclerView.scrollToPosition(-1);
        ChatMessage chatMessage = new
                ChatMessage(ChatHubApplication.getEncryptionHelper()
                .encrypt(mMessageEditText.getText().toString()),
                mUsername,
                mPhotoUrl, animateBackgroundHear);

        MessageUtil.send(chatMessage);
        mMessageEditText.setText("");
    }

    private void loadMap(){
        LoaderManager.LoaderCallbacks<Bitmap> loaderCallbacks =
                new LoaderManager.LoaderCallbacks<Bitmap>() {
                    @Override
                    public Loader<Bitmap> onCreateLoader(final int id, final Bundle args) {
                        return new MapLoader(mContext);
                    }
                    @Override
                    public void onLoadFinished(final Loader<Bitmap> loader, final Bitmap result) {
                        mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                        mLocationButton.setEnabled(true);
                        if (result == null) return;
                        // Resize if too big for messaging
                        Bitmap resizedBitmap = scaleImage(result);
                        Uri uri = null;
                        if (result != resizedBitmap) {
                            uri = savePhotoImage(resizedBitmap, mContext);
                        } else {
                            uri = savePhotoImage(result, mContext);
                        }
                        createImageMessage(uri);
                        getLoaderManager().destroyLoader(0);
                    }
                    @Override
                    public void onLoaderReset(final Loader<Bitmap> loader) {
                    }
                };
        Loader<Bitmap> loader;
        if (getSupportLoaderManager().getLoader(0) == null) {
            loader = getSupportLoaderManager().initLoader(0, null, loaderCallbacks);
        } else {
            loader = getSupportLoaderManager().restartLoader(0, null, loaderCallbacks);

        }
        loader.forceLoad();
        mProgressBar.setVisibility(View.VISIBLE);
        mLocationButton.setEnabled(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        LocationUtils.startLocationUpdates(this);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        boolean isGranted = (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        if (isGranted && requestCode == LocationUtils.REQUEST_CODE) {
            LocationUtils.startLocationUpdates(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                mAuth.signOut();
                Auth.GoogleSignInApi.signOut(mGoogleApiClient);
                mUsername = ANONYMOUS;
                startActivity(new Intent(this, SignInActivity.class));
                return true;

            case R.id.chat_head_option:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {

                    //If the draw over permission is not available open the settings screen
                    //to grant the permission.
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
                } else {
                    initializeChatHeadView();
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void initializeChatHeadView() {
        startService(new Intent(MainActivity.this, ChatHeadService.class));
        finish();
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: request=" + requestCode + ", result=" + resultCode);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                if(uri == null){
                    Log.e(TAG, "Cannot get image for uploading");
                    return;
                }
                Log.i(TAG, "Uri: " + uri.toString());
                String filePath = ImageUtil.getExtensionForUri(uri, this);
                String extension = filePath.substring(filePath.lastIndexOf("."));

                if(extension.equalsIgnoreCase(".webp") || extension.equalsIgnoreCase(".gif")){
                    uri = saveCustomFile(uri, this, extension.toLowerCase());
                }
                else {
                    // Resize if too big for messaging
                    Bitmap bitmap = ImageUtil.getBitmapForUri(uri, this);
                    Bitmap resizedBitmap = scaleImage(bitmap);
                    if (bitmap != resizedBitmap) {
                        uri = savePhotoImage(resizedBitmap, this);
                    }
                }
                createImageMessage(uri);
            }
            else {
                Log.e(TAG, "Cannot get image for uploading");
            }
        }
        if (requestCode == CODE_DRAW_OVER_OTHER_APP_PERMISSION) {

            //Check if the permission is granted or not.
            if (resultCode == RESULT_OK) {
                initializeChatHeadView();
            } else { //Permission is not available
                Toast.makeText(this,
                        "Draw over other app permission not available. Closing the application",
                        Toast.LENGTH_SHORT).show();

                finish();
            }
        }
        if(requestCode == REQ_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && null != data) {

                ArrayList<String> result = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                mMessageEditText.setText(result.get(0));
            }
    }   }

    private void createImageMessage(Uri uri) {
        if (uri == null) Log.e(TAG, "Could not create image message with null uri");
        final StorageReference imageReference = MessageUtil.getImageStorageReference(mUser, uri);
        UploadTask uploadTask = imageReference.putFile(uri);
        // Register observers to listen for when task is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.e(TAG, "Failed to upload image message");
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                ChatMessage chatMessage = new
                        ChatMessage(mMessageEditText.getText().toString(),
                        mUsername,
                        mPhotoUrl, imageReference.toString(), false);
                MessageUtil.send(chatMessage);
                mMessageEditText.setText("");
            }
        });
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_input_string));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }
}
