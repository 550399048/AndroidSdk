package com.wppai.adusdk.demo;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.Toast;

import com.wppai.adsdk.splash.SplashAd;

import java.util.ArrayList;
import java.util.List;

public class SplashActivity extends Activity {

    private static String TAG = "SplashActivity";
    private final int SPLASH_DISPLAY_LENGHT = 3000;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                    startActivity(intent);
                    SplashActivity.this.finish();
                    break;
            }
            return false;
        }
    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_splash);

        if (Build.VERSION.SDK_INT >= 23) {
            checkAndRequestPermission();
        } else {
            getSplashAd();
            // 如果是Android6.0以下的机器，默认在安装时获得了所有权限，可以直接调用SDK
            //fetchSplashAD(this, container, skipView, SyncStateContract.Constants.APPID, getPosId(), this, 0);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkAndRequestPermission() {
        List<String> lackedPermission = new ArrayList<String>();
        if (!(checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)) {
            lackedPermission.add(Manifest.permission.READ_PHONE_STATE);
        }

        if (!(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            lackedPermission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (!(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            lackedPermission.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        /*if (!(checkSelfPermission(Manifest.permission.REQUEST_INSTALL_PACKAGES) == PackageManager.PERMISSION_GRANTED)) {
            lackedPermission.add(Manifest.permission.REQUEST_INSTALL_PACKAGES);
        }*/

        // 权限都已经有了，那么直接调用SDK
        if (lackedPermission.size() == 0) {
            getSplashAd();
        } else {
            // 请求所缺少的权限，在onRequestPermissionsResult中再看是否获得权限，如果获得权限就可以调用SDK，否则不要调用SDK。
            String[] requestPermissions = new String[lackedPermission.size()];
            lackedPermission.toArray(requestPermissions);
            requestPermissions(requestPermissions, 1024);
        }


    }

    private boolean hasAllPermissionsGranted(int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1024 && hasAllPermissionsGranted(grantResults)) {
            getSplashAd();
        } else {
            // 如果用户没有授权，那么应该说明意图，引导用户去设置里面授权。
            Toast.makeText(this, "应用缺少必要的权限！请点击\"权限\"，打开所需要的权限。", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            finish();
        }
    }

    private void getSplashAd() {
        Log.i(TAG, "getSplashAd");

        // type:
        // 原生: 0
        // Banner: 1
        // 插屏: 2
        // 开屏: 3
        // 视频: 4
        // 信息流: 5

        SplashAd.SplashAdListener listener = new SplashAd.SplashAdListener() {

            @Override
            public void onADPresent() {
                Log.i(TAG, "onADPresent");
            }

            @Override
            public void onADClicked() {
                Log.i(TAG, "onSplashADClicked");
            }

            @Override
            public void onADDismissed() {
                Log.i(TAG, "onADDismissed");
                jumpWhenCanClick();
            }

            @Override
            public void onNoAD() {
                Log.i(TAG, "onNoAD");
            }

            @Override
            public void onADError(int error) {
                Toast.makeText(getApplicationContext(), "错误码 = " + error, Toast.LENGTH_LONG).show();
                jump();
            }
        };

        ViewGroup container = findViewById(R.id.splash_container);
        ViewGroup skipContainer = findViewById(R.id.splash_skip_container);
        SplashAd ad = new SplashAd(this, container, skipContainer, Constants.APP_ID, Constants.SPLASH_POS_ID, listener);
        ad.loadAd();
        // splashAD = new SplashAD(activity, adContainer, skipContainer, appId, posId, adListener, fetchDelay);
    }

    //防止用户返回键退出APP
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public boolean canJumpImmediately = false;

    private void jumpWhenCanClick() {
        Log.i(TAG, "jumpWhenCanClick");
        if (canJumpImmediately) {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            SplashActivity.this.finish();
        } else {
            canJumpImmediately = true;
        }
    }

    /**
     * 不可点击的开屏，使用该jump方法，而不是用jumpWhenCanClick
     */
    private void jump() {
        Log.i(TAG, "jump");
        mHandler.sendEmptyMessageDelayed(1, SPLASH_DISPLAY_LENGHT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");

        if (canJumpImmediately) {
            jumpWhenCanClick();
        }
        canJumpImmediately = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");

        canJumpImmediately = false;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
