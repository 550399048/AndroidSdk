package com.wppai.adsdk.nativ;

import android.view.View;

public interface NativeAdDataRef {
    String getTitle();

    String getDesc();

    String getIconUrl();

    String getImgUrl();

    int getPosWidth();

    int getPosHeight();

    void onExposured();

    void onClicked(final int down_x, final int down_y, final int up_x, final int up_y);
}
