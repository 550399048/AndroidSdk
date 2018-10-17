package com.wppai.adsdk.comm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

public class NotificationBroadcastReceiver extends BroadcastReceiver {
    public final static String TAG = "NotificationBR";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction()!= null) {
            Log.i(TAG, "onReceive"+intent.getAction());
        }

    }
}
