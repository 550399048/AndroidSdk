package com.wppai.adusdk.demo;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.util.Util;
import com.wppai.adsdk.nativ.NativeAd;
import com.wppai.adsdk.nativ.NativeAdData;

import java.util.List;

public class NativeActivity extends Activity implements View.OnClickListener{
    private static final String TAG = "NativeActivity";
    private TextView nativeTitle;
    private TextView nativeDescrip;
    private ImageView nativeImg;
    private NativeAd nativeAd;
    private String mPosId;
    private FrameLayout showAdContain;
    private View nativeView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native);
        nativeView = getLayoutInflater().inflate(R.layout.native_ad_layout,null);
        showAdContain = (FrameLayout)findViewById(R.id.show_ad_contain);
        ((EditText) findViewById(R.id.posId)).setText(Constants.NATIVE_POS_ID);
        nativeTitle = nativeView.findViewById(R.id.native_ad_title);
        nativeDescrip = nativeView.findViewById(R.id.native_ad_descript);
        nativeImg = nativeView.findViewById(R.id.native_ad_img);
        this.findViewById(R.id.load_native);

        getNativeAd().loadAd();
    }

    private NativeAdData mTestAdData;
    private float mDownX = 0;
    private float mDownY = 0;
    private float mUpX = 0;
    private float mUpY = 0;

    private  NativeAd getNativeAd(){
        String posId = getPosID();
        if (nativeAd != null && mPosId.equals(posId)) {
            return nativeAd;
        }

        if (nativeAd != null) {
            nativeAd = null;
            showAdContain.removeAllViews();
        }
        mPosId = posId;
        if (nativeAd == null) {
            NativeAd.NativeAdListener listener = new NativeAd.NativeAdListener() {
                @Override
                public void onADLoaded(final List<NativeAdData> adData) {
                    showAdContain.removeAllViews();
                    showAdContain.addView(nativeView);
                    for (int i = 0; i < adData.size(); i++) {
                        Log.i(TAG, "xxx title:" + adData.get(i).getTitle());
                        Log.i(TAG, "xxx desc:" + adData.get(i).getDesc());
                        Log.i(TAG, "xxx icon url:" + adData.get(i).getIconUrl());
                        Log.i(TAG, "xxx img url:" + adData.get(i).getImgUrl());

                        String imageUrl = adData.get(i).getImgUrl();
                        if (Util.isOnMainThread()) {
                            Glide.with(NativeActivity.this.getApplicationContext())
                                    .load(imageUrl)
                                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                                    .into(nativeImg);
                        }
                        mTestAdData = adData.get(i);
                        nativeTitle.setText(mTestAdData.getTitle());
                        nativeDescrip.setText(mTestAdData.getDesc());
                        mTestAdData.onExposured(showAdContain,nativeView);
                    }
                }

                @Override
                public void onNoAD() {
                    showAdContain.removeAllViews();
                }

                @Override
                public void onADError(int error) {
                    Toast.makeText(getApplicationContext(), "错误码 = " + error, Toast.LENGTH_LONG).show();
                }
            };

            nativeView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            Log.i(TAG, "down x: " + event.getX());
                            Log.i(TAG, "down y: " + event.getY());
                            mDownX = event.getX();
                            mDownY = event.getY();

                            break;
                        case MotionEvent.ACTION_UP:
                            Log.i(TAG, "up x: " + event.getX());
                            Log.i(TAG, "up y: " + event.getY());
                            mUpX = event.getX();
                            mUpY = event.getY();
                            if (mDownX < 0 || mDownY < 0 || mUpX < 0 || mUpY < 0) {
                                return true;
                            }

                            int transformDownX = (int) mDownX;
                            int transformDownY = (int) mDownY;

                            int transformUpX = (int) mUpX;
                            int transformUpY = (int) mUpY;


                            Log.i(TAG, "transform down x: " + transformDownX);
                            Log.i(TAG, "transform down y: " + transformDownY);
                            Log.i(TAG, "transform up x: " + transformUpX);
                            Log.i(TAG, "transform up y: " + transformUpY);
                            if (mTestAdData != null) {
                                mTestAdData.onClicked(v,transformDownX, transformDownY, transformUpX, transformUpY);
                            }
                            return false;
                    }
                    return true;
                }
            });

            nativeAd = new NativeAd(this, Constants.APP_ID, Constants.NATIVE_POS_ID,6, listener);
        }
        return nativeAd;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.load_native:
                getNativeAd().loadAd();
                break;
        }
    }

    private String getPosID() {
        EditText posIdEdit = (EditText) findViewById(R.id.posId);
        String posId = posIdEdit.getText().toString();
        return TextUtils.isEmpty(posId) ? Constants.NATIVE_POS_ID : posId;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
