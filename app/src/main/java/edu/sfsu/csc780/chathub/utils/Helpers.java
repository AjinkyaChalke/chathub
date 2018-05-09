package edu.sfsu.csc780.chathub.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

public class Helpers
    implements GoogleApiClient.OnConnectionFailedListener ,MessageUtil.MessageLoadListener{

    private Context mContext;
    private final String TAG = this.getClass().getSimpleName();
    private ProgressBar mProgressBar;
    private ImageView mImageView;

    public Helpers(Context context, ProgressBar progressBar, ImageView imageView){
        this.mContext = context;
        this.mProgressBar = progressBar;
        this.mImageView = imageView;
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(mContext, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLoadComplete() {
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);
    }

    @Override
    public void animateBackground(boolean bool) {
        mImageView.clearAnimation();
        if(bool){

            Animation animation = new AlphaAnimation(1, 0);
            animation.setDuration(1000);
            animation.setInterpolator(new LinearInterpolator());
            animation.setRepeatCount(3);
            animation.setRepeatMode(Animation.REVERSE);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    mImageView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mImageView.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            mImageView.startAnimation(animation);
        }
    }

}
