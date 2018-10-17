package com.wppai.adsdk.splash;

import android.content.Context;
import com.wppai.adsdk.base.BaseAdData;

public class SplashAdData extends BaseAdData {
    public final static String TAG = "SplashAdData";

    String conversion_link;

    private Context mContext;

    SplashAdData(Context ctx) {
        super(ctx);
        mContext = ctx;
    }
}
