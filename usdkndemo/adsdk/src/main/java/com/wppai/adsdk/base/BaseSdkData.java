package com.wppai.adsdk.base;

import android.content.Context;

public class BaseSdkData extends BaseAdData {
    public final static String TAG = "BaseSdkData";

    private String appId;
    private String posId;
    private String mediaId;
    private int weight;


    public BaseSdkData(Context ctx) {
        super(ctx);
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getPosId() {
        return posId;
    }

    public void setPosId(String posId) {
        this.posId = posId;
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
}
