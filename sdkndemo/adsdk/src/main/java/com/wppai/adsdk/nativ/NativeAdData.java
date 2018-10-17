package com.wppai.adsdk.nativ;

import android.content.Context;
import android.view.View;

import com.wppai.adsdk.base.BaseAdData;


public class NativeAdData extends BaseAdData implements NativeAdDataRef {
    public final static String TAG = "NativeAdData";

    private Context mContext;

    NativeAdData(Context ctx) {
        super(ctx);
        mContext = ctx;
    }

    public void onClicked(View view , int down_x, int down_y, int up_x, int up_y) {
        super.onClicked(down_x, down_y, up_x, up_y);
    }

    public void onExposured(View groupView  ,View view) {
        super.onExposured();
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getDesc() {
        return description;
    }

    @Override
    public String getIconUrl() {
        return img2_url;
    }

    @Override
    public String getImgUrl() {
        return img_url;
    }

    @Override
    public int getPosWidth() {
        return Integer.valueOf(pos_width);
    }

    @Override
    public int getPosHeight() {
        return Integer.valueOf(pos_height);
    }

}
