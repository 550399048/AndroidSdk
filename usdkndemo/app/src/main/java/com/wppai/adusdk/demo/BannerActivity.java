package com.wppai.adusdk.demo;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.wppai.adsdk.banner.BannerAdView;

public class BannerActivity extends Activity implements View.OnClickListener {
    private BannerAdView mBannerAdView;
    private String mPosId;
    private ViewGroup mBannerContainer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_banner);
        mBannerContainer = (ViewGroup) this.findViewById(R.id.bannerContainer);
        ((EditText) findViewById(R.id.posId)).setText(Constants.BANNER_POS_ID);
        this.findViewById(R.id.loadBanner).setOnClickListener(this);
        getBannerView().loadAd();
    }

    private BannerAdView getBannerView() {
        String posId = getPosID();
        if (mBannerAdView != null && posId.equals(posId)) {
            return mBannerAdView;
        }

        if (mBannerAdView != null) {
            mBannerContainer.removeView(mBannerAdView);
        }

        mPosId = posId;
        BannerAdView.BannerAdListener listener = new BannerAdView.BannerAdListener() {
            @Override
            public void onADReceive() {

            }

            @Override
            public void onADClicked() {

            }

            @Override
            public void onADError(int error) {
                Toast.makeText(getApplicationContext(),"错误码 = "+error,Toast.LENGTH_LONG).show();
            }
        };
        mBannerAdView = new BannerAdView(this,Constants.APP_ID,mPosId,listener);
        mBannerAdView.setBannerBackgroup(true);
        mBannerContainer.addView(mBannerAdView);
        return mBannerAdView;
    }

    private String getPosID() {
        EditText posIdEdit = (EditText) findViewById(R.id.posId);
        String posId = posIdEdit.getText().toString();
        return TextUtils.isEmpty(posId) ? Constants.BANNER_POS_ID : posId;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.loadBanner:
                getBannerView().loadAd();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
