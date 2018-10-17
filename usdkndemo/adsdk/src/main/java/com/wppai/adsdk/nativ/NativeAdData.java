package com.wppai.adsdk.nativ;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.baidu.mobad.feeds.NativeResponse;
import com.bytedance.sdk.openadsdk.TTFeedAd;
import com.qq.e.ads.nativ.NativeADDataRef;
import com.wppai.adsdk.base.BaseAdData;
import com.wppai.adsdk.comm.AntiCheatUtils;


public class NativeAdData extends BaseAdData implements NativeAdDataRef {
    public final static String TAG = "NativeAdData";

    private Context mContext;
    private NativeADDataRef mGDTData;
    private TTFeedAd mTTData;
    private NativeResponse mBaiDuData;
    private int mNativeType = 0; // 0为默认API,1为GDT,2为头条,3为baidu

    NativeAdData(Context ctx) {
        super(ctx);
        mContext = ctx;
    }

    public void onExposured(final View viewContainer, final View view) {
        Log.i(TAG, "onExposured, native type: " + mNativeType);
        if (mNativeType == 0) {
        } else if (mNativeType == 1) {
            mGDTData.onExposured(viewContainer);
        } else if (mNativeType == 2) {
            boolean isValid = AntiCheatUtils.get().isValid();
            if (!isValid) {
                return;
            }
            mTTData.registerViewForInteraction((ViewGroup) viewContainer, view, new TTFeedAd.AdInteractionListener() {
                @Override
                public void onAdClicked(View view, TTFeedAd ad) {
                    Log.i(TAG, "onAdClicked");
                    onClicked(0, 0, 0, 0);
                }

                @Override
                public void onAdCreativeClick(View view, TTFeedAd ad) {
                    Log.i(TAG, "onAdCreativeClick");
                }

                @Override
                public void onAdShow(TTFeedAd ad) {
                    Log.i(TAG, "onAdShow");
                }
            });
        } else if (mNativeType == 3) {
            mBaiDuData.recordImpression(viewContainer);
        }
        super.onExposured();
    }

    public void onClicked(final View view, final int down_x, final int down_y, final int up_x, final int up_y) {
        Log.i(TAG, "onClicked, native type: " + mNativeType);

        boolean isValid = AntiCheatUtils.get().isValid();
        if (!isValid) {
            return;
        }

        if (mNativeType == 0) {
        } else if (mNativeType == 1) {
            mGDTData.onClicked(view);
        } else if (mNativeType == 3) {
            mBaiDuData.handleClick(view);
        }
        super.onClicked(down_x, down_y, up_x, up_y);
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

    public void setNativeType(final int type) {
        mNativeType = type;
    }

    public void setGDTData(final NativeADDataRef data) {
        mGDTData = data;
    }

    public void setTTData(final TTFeedAd data) {
        mTTData = data;
    }

    public void setBaiDuData(final NativeResponse data) {
        mBaiDuData = data;
    }
}
