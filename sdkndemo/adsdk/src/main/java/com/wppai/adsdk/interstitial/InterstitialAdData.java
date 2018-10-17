package com.wppai.adsdk.interstitial;

import android.content.Context;

import com.wppai.adsdk.base.BaseAdData;

public class InterstitialAdData extends BaseAdData {
    public final static String TAG = "InterstitialAdData";

    String conversion_link;
    boolean is_full_screen_interstitial;

    private Context mContext;

    InterstitialAdData(Context ctx) {
        super(ctx);
        mContext = ctx;
    }
}
