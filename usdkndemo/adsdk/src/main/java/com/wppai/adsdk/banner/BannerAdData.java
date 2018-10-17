package com.wppai.adsdk.banner;

import android.content.Context;

import com.wppai.adsdk.base.BaseAdData;

public class BannerAdData extends BaseAdData {
    public final static String TAG = "BannerAdData";

    String conversion_link;

    private Context mContext;

    BannerAdData(Context ctx) {
        super(ctx);
        mContext = ctx;
    }
}
