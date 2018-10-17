package com.wppai.adusdk.demo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bannerADButton:
                startActivity(new Intent(this,BannerActivity.class));
                break;
            case R.id.interstitialADButton:
                startActivity(new Intent(this,InterstitialActivity.class));
                break;
            case R.id.splashADButton:
                startActivity(new Intent(this,SplashActivity.class));
                break;
            case R.id.nativeADButton:
                startActivity(new Intent(this,NativeActivity.class));
                break;
            case R.id.nativeADButton1:
                startActivity(new Intent(this,NativeVerticalActivity.class));
                break;
                default:
                    break;

        }
    }
}
