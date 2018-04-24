package edu.sfsu.csc780.chathub;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import edu.sfsu.csc780.chathub.model.ChatMessage;
import edu.sfsu.csc780.chathub.ui.MainActivity;
import edu.sfsu.csc780.chathub.ui.MessageUtil;
import edu.sfsu.csc780.chathub.ui.SignInActivity;
import edu.sfsu.csc780.chathub.utils.Helpers;

public class ChatHeadService extends Service {

    private WindowManager mWindowManager;
    private View mChatHeadView;
    private WindowManager.LayoutParams mParams;
    private View mCollapsedView;
    private View mExpandedView;

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
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseRecyclerAdapter<ChatMessage, MessageUtil.MessageViewHolder> mFirebaseAdapter;
    private ImageButton mLocationButton;
    private ImageView mImageView;

    public ChatHeadService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mChatHeadView = LayoutInflater.from(this).inflate(R.layout.layout_chat_head, null);

        mCollapsedView = mChatHeadView.findViewById(R.id.chat_head_root);
        mExpandedView = mChatHeadView.findViewById(R.id.chat_head_expanded);


        //Add the view to the window.
        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        //Specify the chat head position
        //Initially view will be added to top-left corner
        mParams.gravity = Gravity.TOP | Gravity.LEFT;
        mParams.x = 0;
        mParams.y = 100;

        //Add the view to the window
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mChatHeadView, mParams);

        //Set the close button.
        ImageView closeButton = mChatHeadView.findViewById(R.id.close_btn);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //close the service and remove the chat head from the window
                stopSelf();
            }
        });



        //Drag and move chat head using user's touch action.
        final ImageView chatHeadImage = mChatHeadView.findViewById(R.id.chat_head_profile_iv);
        chatHeadImage.setOnTouchListener(mOnTouchListener);

        initialize();
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
//            finish();
            return;
        } else {
            mUsername = mUser.getDisplayName();
            if (mUser.getPhotoUrl() != null) {
                mPhotoUrl = mUser.getPhotoUrl().toString();
            }
        }

        mImageView = mChatHeadView.findViewById(R.id.backgroundImage);
        // Initialize ProgressBar and RecyclerView.
        mProgressBar = mChatHeadView.findViewById(R.id.progressBar);
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        Helpers helpers = new Helpers(this, mProgressBar, mImageView);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();

        mMessageRecyclerView = mChatHeadView.findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);


        mFirebaseAdapter = MessageUtil.getFirebaseAdapter(this,
                null,
                helpers,
                mLinearLayoutManager,
                mMessageRecyclerView);
        mMessageRecyclerView.setAdapter(mFirebaseAdapter);

        mMessageEditText = mChatHeadView.findViewById(R.id.messageEditText);
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

        mLocationButton = mChatHeadView.findViewById(R.id.locationButton);
        mLocationButton.setVisibility(View.INVISIBLE);

        mSendButton = mChatHeadView.findViewById(R.id.sendButton);
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


        ImageView closeButton = mChatHeadView.findViewById(R.id.close_expanded_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCollapsedView.setVisibility(View.VISIBLE);
                mExpandedView.setVisibility(View.GONE);
            }
        });
        closeButton.setVisibility(View.VISIBLE);

        ImageButton shareImageButton = mChatHeadView.findViewById(R.id.shareImageButton);
        shareImageButton.setVisibility(View.GONE);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatHeadView != null) {
            mWindowManager.removeView(mChatHeadView);
        }
    }

    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        private int initialX;
        private int initialY;
        private float initialTouchX;
        private float initialTouchY;


        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:

                    //remember the initial position.
                    initialX = mParams.x;
                    initialY = mParams.y;

                    //get the touch location
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_UP:
                    int Xdiff = (int) (event.getRawX() - initialTouchX);
                    int Ydiff = (int) (event.getRawY() - initialTouchY);


                    //The check for Xdiff <10 && YDiff< 10 because sometime elements moves a little while clicking.
                    //So that is click event.
                    if (Xdiff < 10 && Ydiff < 10) {
                        if (isViewCollapsed()) {
                            //When user clicks on the image view of the collapsed layout,
                            //visibility of the collapsed layout will be changed to "View.GONE"
                            //and expanded view will become visible.
                            mCollapsedView.setVisibility(View.GONE);
                            mExpandedView.setVisibility(View.VISIBLE);
                        }
                    }
                    return true;
                case MotionEvent.ACTION_MOVE:
                    //Calculate the X and Y coordinates of the view.
                    mParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                    mParams.y = initialY + (int) (event.getRawY() - initialTouchY);


                    //Update the layout with new X & Y coordinate
                    mWindowManager.updateViewLayout(mChatHeadView, mParams);
                    return true;
            }
            return false;
        }
    };

    private boolean isViewCollapsed() {
        return mChatHeadView == null || mCollapsedView.getVisibility() == View.VISIBLE;
    }
}
