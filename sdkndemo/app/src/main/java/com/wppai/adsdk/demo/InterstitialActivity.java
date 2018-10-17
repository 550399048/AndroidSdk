package com.wppai.adsdk.demo;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.wppai.adsdk.interstitial.InterstitialAd;

public class InterstitialActivity extends Activity implements View.OnClickListener{
    private static String TAG = "InterstitialActivity";
    private InterstitialAd intersititialAd;
    private String posId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interstitial);
        ((EditText) findViewById(R.id.posId)).setText(Constants.INTERTERISTAL_POS_ID);
        findViewById(R.id.start_interstitial);
        findViewById(R.id.close_interstitial);

        loadIntersititialAd();
    }

    private InterstitialAd loadIntersititialAd() {
        String posId = getPosID();
        if (intersititialAd != null && this.posId.equals(posId)) {
            return intersititialAd;
        }
        this.posId = posId;
        if (intersititialAd != null) {
            intersititialAd.closeAsPopupWindow();
            intersititialAd = null;
        }
        if (intersititialAd == null) {
            /**
             * 创建插屏广告
             */
            intersititialAd = new InterstitialAd(this, Constants.APP_ID, Constants.INTERTERISTAL_POS_ID);
        }

        return intersititialAd;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_interstitial:
                showAD();
                break;
            case R.id.close_interstitial:
                closePopup();
                break;
                default:break;

        }
    }

    private void showAD() {
        InterstitialAd.InterstitialAdListener listener = new InterstitialAd.InterstitialAdListener() {

            @Override
            public void onADReceive() {
                intersititialAd.showAsPopupWindow();

            }

            @Override
            public void onADClicked() {
                Log.d(TAG,"插屏点击");
            }

            @Override
            public void onNoAD() {

            }

            @Override
            public void onADError(int error) {
                Toast.makeText(getApplicationContext(),"错误码 = "+error,Toast.LENGTH_LONG).show();
            }
        };
        intersititialAd.setADListener(listener);
        intersititialAd.loadAd();
    }

    private void closePopup(){
        if (intersititialAd != null) {
            intersititialAd.closeAsPopupWindow();
        }
    }

    private String getPosID() {
        String posId = ((EditText) findViewById(R.id.posId)).getText().toString();
        return TextUtils.isEmpty(posId) ? Constants.INTERTERISTAL_POS_ID : posId;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (intersititialAd != null) {
            intersititialAd.closeAdInterstitial();
            intersititialAd = null;
        }
    }
}
