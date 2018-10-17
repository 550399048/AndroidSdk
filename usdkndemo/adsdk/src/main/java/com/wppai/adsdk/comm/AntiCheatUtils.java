package com.wppai.adsdk.comm;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class AntiCheatUtils {
    public final static String TAG = "AntiCheatUtils";

    private int mClickNum = 0;
    private final int TIME_ONE_MINUTE = 60 * 1000;
    private final int TIME_FIVE_MINUTE = 5 * 60 * 1000;
    private int mFlag = 0; // 0代表观察期, 1代表冷却期
    private final int FLAG_0_MAX_TIME = 20;
    private final int FLAG_1_MAX_TIME = 5;

    private final int MSG_UPDATE_TIME = 0;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_TIME:
                    Log.i(TAG, "Handler, flag: " + mFlag + ", click num: " + mClickNum);

                    mClickNum = 0;
                    mFlag = 0;
                    break;
                default:
                    break;
            }
        }
    };

    private static AntiCheatUtils mACUtils;

    public static AntiCheatUtils get() {
        if (mACUtils == null) {
            mACUtils = new AntiCheatUtils();
        }
        return mACUtils;
    }

    private AntiCheatUtils() {
    }

    public boolean isValid() {
        Log.i(TAG, "isValid, 0, flag: " + mFlag + ", click num: " + mClickNum);
        mClickNum++;
        if (mClickNum > FLAG_0_MAX_TIME && mFlag == 0) {
            mFlag = 1;
            mClickNum = 1;
            mHandler.removeMessages(MSG_UPDATE_TIME);
        }
        Log.i(TAG, "isValid, 1, flag: " + mFlag + ", click num: " + mClickNum);

        if (mClickNum > FLAG_1_MAX_TIME && mFlag == 1) {
            return false;
        }

        if (!mHandler.hasMessages(MSG_UPDATE_TIME)) {
            Log.i(TAG, "isValid, msg, flag: " + mFlag + ", click num: " + mClickNum);
            if (mFlag == 0) {
                mHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, TIME_ONE_MINUTE);
            } else if (mFlag == 1) {
                mHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, TIME_FIVE_MINUTE);
            }
        }

        return true;
    }
}
