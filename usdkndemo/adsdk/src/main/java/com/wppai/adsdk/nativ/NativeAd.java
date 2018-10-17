package com.wppai.adsdk.nativ;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.baidu.mobad.feeds.BaiduNative;
import com.baidu.mobad.feeds.NativeErrorCode;
import com.baidu.mobad.feeds.NativeResponse;
import com.baidu.mobad.feeds.RequestParameters;
import com.baidu.mobads.AdView;
import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTFeedAd;
import com.google.gson.Gson;
import com.qq.e.ads.nativ.NativeAD;
import com.qq.e.ads.nativ.NativeADDataRef;
import com.qq.e.comm.util.AdError;
import com.wppai.adsdk.base.AdApi;
import com.wppai.adsdk.base.BaseSdkData;
import com.wppai.adsdk.comm.AdCacheUtils;
import com.wppai.adsdk.comm.AdProvider;
import com.wppai.adsdk.tt.TTAdManagerHolder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class NativeAd {
    public final static String TAG = "NativeAd";

    private final int TYPE = 0;

    private NativeAdListener mListener;

    private Context mContext;
    private String mAppId;
    private String mPosId;
    private int mAdCount = 1;

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    List<NativeAdData> dataList = (List<NativeAdData>) msg.obj;
                    if (mListener != null) {
                        mListener.onADLoaded(dataList);
                    }
                    break;
                case 0:
                    int err = (int) msg.obj;
                    if (mListener != null) {
                        mListener.onADError(err);
                    }
                    break;
                case 3:
                    switchToSdkAd();
                    break;
                default:
                    break;
            }
        }
    };

    public NativeAd(final Context ctx, final String appId, final String posId, NativeAdListener listener) {

        mListener = listener;
        mContext = ctx;
        mAppId = appId;
        mPosId = posId;
    }

    public NativeAd(final Context ctx, final String appId, final String posId, int adCount, NativeAdListener listener) {
        this(ctx, appId, posId, listener);
        mAdCount = adCount;
    }

    public void loadAd() {
        String url = AdApi.getInstance().getUrl(mContext, mAppId, mPosId, TYPE, mAdCount);
        String idsStr = AdCacheUtils.getAdIdsStrFromCache(mContext, mAppId, mPosId);
        long currentTime = System.currentTimeMillis();
        if (currentTime >= AdCacheUtils.getRequestAdTime(mContext, mAppId, mPosId)) {
            AdCacheUtils.refreshRequestAdTime(mContext, mAppId, mPosId);
            url = AdApi.getInstance().getUrl(mContext, mAppId, mPosId, TYPE, mAdCount, idsStr);
            AdCacheUtils.adProviderList.clear();
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().get().url(url).build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                try {
                    JSONObject obj = new JSONObject(response.body().string());

                    int ret = obj.optInt("ret");
                    Log.i(TAG, "ret:" + ret);
                    Log.i(TAG, "obj:" + obj.toString());

                    if (ret == 0) {
                        JSONObject dataObject = obj.optJSONObject("data");
                        if (dataObject != null) {
                            JSONArray array = dataObject.optJSONObject(mPosId).optJSONArray("list");
                            final List<NativeAdData> adDatas = new ArrayList<>();
                            AdProvider adProvider = new AdProvider();
                            for (int i = 0; i < array.length(); i++) {
                                NativeAdData data = new NativeAdData(mContext);
                                data.ad_id = array.optJSONObject(i).optString("ad_id");
                                data.impression_link = array.optJSONObject(i).optString("impression_link");
                                data.click_link = array.optJSONObject(i).optString("click_link");
                                data.interact_type = array.optJSONObject(i).optInt("interact_type");
                                data.crt_type = array.optJSONObject(i).optInt("crt_type");
                                data.title = array.optJSONObject(i).optString("title");
                                data.description = array.optJSONObject(i).optString("description");
                                data.img_url = array.optJSONObject(i).optString("img_url");
                                data.img2_url = array.optJSONObject(i).optString("img2_url");
                                data.impression_link_cc = array.optJSONObject(i).optString("impression_link_cc");
                                data.click_link_cc = array.optJSONObject(i).optString("click_link_cc");
                                data.req_width = array.optJSONObject(i).optString("req_width");
                                data.req_height = array.optJSONObject(i).optString("req_height");
                                data.pos_width = array.optJSONObject(i).optString("pos_width");
                                data.pos_height = array.optJSONObject(i).optString("pos_height");
                                data.location_url = array.optJSONObject(i).optString("location_url");
                                data.rel_type = array.optJSONObject(i).optInt("rel_type");
                                data.open_type = array.optJSONObject(i).optInt("open_type");
                                data.setNativeType(0);

                                adDatas.add(data);
                                adProvider.relType = data.rel_type;
                                adProvider.lastIds.add(data.ad_id);
                            }

                            if (!AdCacheUtils.isContainSameReltype(adProvider)) {
                                AdCacheUtils.adProviderList.add(adProvider);
                            }

                            Message message = new Message();
                            message.what = 1;
                            message.obj = adDatas;
                            handler.sendMessage(message);
                            String idsStr = null;
                            if (AdCacheUtils.adProviderList.size() != 0) {
                                idsStr = new Gson().toJson(AdCacheUtils.adProviderList);
                                AdCacheUtils.putAdIdsToCache(mContext, mAppId, mPosId, idsStr);
                            }

                            /*for (int i = 0; i < adDatas.size(); i++) {
                                Log.i(TAG, "ad_id:" + adDatas.get(i).ad_id);
                                Log.i(TAG, "impression_link:" + adDatas.get(i).impression_link);
                                Log.i(TAG, "click_link:" + adDatas.get(i).click_link);
                                Log.i(TAG, "interact_type:" + adDatas.get(i).interact_type);
                                Log.i(TAG, "crt_type:" + adDatas.get(i).crt_type);
                                Log.i(TAG, "title:" + adDatas.get(i).title);
                                Log.i(TAG, "description:" + adDatas.get(i).description);
                                Log.i(TAG, "img_url:" + adDatas.get(i).img_url);
                                Log.i(TAG, "img2_url:" + adDatas.get(i).img2_url);
                                Log.i(TAG, "impression_link_cc:" + adDatas.get(i).impression_link_cc);
                                Log.i(TAG, "click_link_cc:" + adDatas.get(i).click_link_cc);
                                Log.i(TAG, "req_width:" + adDatas.get(i).req_width);
                                Log.i(TAG, "req_height:" + adDatas.get(i).req_height);
                                Log.i(TAG, "pos_width:" + adDatas.get(i).pos_width);
                                Log.i(TAG, "pos_height:" + adDatas.get(i).pos_height);
                                Log.i(TAG, "rel_type" + adDatas.get(i).rel_type);
                                Log.i(TAG, "open_type" + adDatas.get(i).open_type);
                            }*/
                        }
                    } else if (ret == 1) { // sdk
                        JSONArray jsonArray = obj.optJSONArray("data");
                        if (jsonArray != null) {
                            mSdkDataList.clear();
                            for (int i = 0; i < jsonArray.length(); i++) {
                                BaseSdkData data = new BaseSdkData(mContext);
                                data.setAppId(jsonArray.optJSONObject(i).optString("platform_app_id"));
                                data.setPosId(jsonArray.optJSONObject(i).optString("platform_pos_id"));
                                data.setMediaId(jsonArray.optJSONObject(i).optString("platform_media_id"));
                                data.setWeight(jsonArray.optJSONObject(i).optInt("weight"));

                                if (obj.has("impression_link_cc") && obj.has("click_link_cc")) {
                                    data.impression_link_cc = obj.optString("impression_link_cc") + "&channel_id=" + data.getMediaId();
                                    data.click_link_cc = obj.optString("click_link_cc") + "&channel_id=" + data.getMediaId();
                                    data.pos_width = "0";
                                    data.pos_height = "0";
                                    data.req_width = "0";
                                    data.req_height = "0";
                                }
                                mSdkDataList.add(data);
                            }
                            handler.sendEmptyMessage(3);
                        }
                    } else {
                        final int error = ret;
                        Message message = new Message();
                        message.what = 0;
                        message.obj = error;
                        handler.sendMessage(message);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public interface NativeAdListener {
        void onADLoaded(List<NativeAdData> var1);

        void onNoAD();

        void onADError(int error);
    }

    /*{"data":{"55667":{"list":[{
        "ad_id":"56552291",
        "impression_link":"http:\/\/v.gdt.qq.com\/gdt_stats.fcg?viatatype=json",
        "click_link":"http:\/\/c.gdt.qq.com\/gdt_mclic7D",
        "interact_type":0,
        "crt_type":11,
        "title":"京东618大牌狂欢 爆款直降",
        "description":"低至满199减100",
        "img_url":"http:\/\/pgdt.ugdtimg.com\/gdt\/0\/DAAYMWUAUAALQABfBbIItNB7rbtJ3L.jpg\/0?ck=9245ffa7f2aa219f95cf308f496f518e",
        "img2_url":"http:\/\/pgdt.ugdtimg.com\/gdt\/0\/DAAYMWUAEsAEsAAFBbIItPAGW8QpGz.jpg\/0?ck=33956b20d7a16bc25e0ed9cce8a72823",
        "impression_link_cc":"http:\/\/api.adx.scloud.lfengmobile.com\/api\/v1\/report\/expose?report_id=9045",
        "click_link_cc":"http:\/\/api.adx.scloud.lfengmobile.com\/a",
        "req_width":"0",
        "req_height":"0",
        "pos_width":"1080",
        "pos_height":"312"
    }]}},"ret":0,"msg":""}

    {"ret":"107024","errno":10000,"data":[],"errmsg":"success"}*/
    //getGDTNativeAd("1101152570", "5010320697302671");
    //getTTNativeAd("5001121", "901121737");
    //getBaiDuNativeAd("e866cfb0", "2058628");
    private List<BaseSdkData> mSdkDataList = new ArrayList<>();
    private BaseSdkData mCurrentSdkData = null;

    private void switchToSdkAd() {
        Log.i(TAG, "switchToSdkAd");

        if (mSdkDataList.size() <= 0) {
            Log.i(TAG, "switchToSdkAd return");
            if (mListener != null) {
                mListener.onADError(-1);
            }
            return;
        }
        mCurrentSdkData = mSdkDataList.get(0);
        mSdkDataList.remove(0);

        if (mCurrentSdkData.getMediaId().equals("1")) {
            getGDTNativeAd(mCurrentSdkData.getAppId(), mCurrentSdkData.getPosId());
        } else if (mCurrentSdkData.getMediaId().equals("8")) {
            getBaiDuNativeAd(mCurrentSdkData.getAppId(), mCurrentSdkData.getPosId());
        } else if (mCurrentSdkData.getMediaId().equals("2")) {
            getTTNativeAd(mCurrentSdkData.getAppId(), mCurrentSdkData.getPosId());
        }
    }

    // GDT
    private com.qq.e.ads.nativ.NativeAD.NativeAdListener mGDTListener = new com.qq.e.ads.nativ.NativeAD.NativeAdListener() {

        @Override
        public void onADLoaded(List<NativeADDataRef> list) {
            if (mListener != null) {
                if (list.size() > 0) {
                    List<NativeAdData> adDatas = new ArrayList<>();
                    Log.i(TAG, "onADLoaded size:" + list.size());

                    for (int i = 0; i < list.size(); i++) {
                        NativeADDataRef dataRef = list.get(i);
                        NativeAdData data = new NativeAdData(mContext);
                        data.title = dataRef.getTitle();
                        data.description = dataRef.getDesc();
                        data.img_url = dataRef.getImgUrl();
                        data.img2_url = dataRef.getIconUrl();
                        if (mCurrentSdkData != null) {
                            data.pos_width = mCurrentSdkData.pos_width;
                            data.pos_height = mCurrentSdkData.pos_height;
                            data.req_width = mCurrentSdkData.req_width;
                            data.req_height = mCurrentSdkData.req_height;
                            data.impression_link_cc = mCurrentSdkData.impression_link_cc;
                            data.click_link_cc = mCurrentSdkData.click_link_cc;
                        }

                        data.setNativeType(1);
                        data.setGDTData(dataRef);
                        adDatas.add(data);
                    }

                    if (mListener != null) {
                        mListener.onADLoaded(adDatas);
                    }
                }
            }
        }

        @Override
        public void onNoAD(AdError adError) {
            if (mSdkDataList.size() > 0) {
                handler.sendEmptyMessage(3);
                return;
            }
            if (mListener != null) {
                mListener.onADError(adError.getErrorCode());
            }
        }

        @Override
        public void onADStatusChanged(NativeADDataRef nativeADDataRef) {

        }

        @Override
        public void onADError(NativeADDataRef nativeADDataRef, AdError adError) {
            if (mListener != null) {
                mListener.onADError(adError.getErrorCode());
            }
        }
    };

    private void getGDTNativeAd(final String appId, final String posId) {
        Log.i(TAG, "getGDTNativeAd");

        NativeAD nativeAD = new NativeAD(mContext, appId, posId, mGDTListener);
        nativeAD.loadAD(mAdCount);
    }

    // TT, 头条, 网盟
    private void getTTNativeAd(final String appId, final String posId) {
        Log.i(TAG, "getTTNativeAd");

        Context appContext = mContext.getApplicationContext();
        TTAdManagerHolder.getInstance(appContext).setAppId(appId);
        TTAdManagerHolder.getInstance(appContext).requestPermissionIfNecessary(appContext);
        TTAdNative mTTAdNative = TTAdManagerHolder.getInstance(mContext).createAdNative(mContext);

        //feed广告请求类型参数
        AdSlot adSlot = new AdSlot.Builder()
                .setCodeId(posId)
                .setSupportDeepLink(true)
                .setImageAcceptedSize(640, 320)
                .setAdCount(3)
                .build();
        //调用feed广告异步请求接口
        mTTAdNative.loadFeedAd(adSlot, new TTAdNative.FeedAdListener() {
            @Override
            public void onError(int code, String message) {
                Log.i(TAG, "onError, code: " + code);
                if (mSdkDataList.size() > 0) {
                    handler.sendEmptyMessage(3);
                    return;
                }
                if (mListener != null) {
                    mListener.onADError(code);
                }
            }

            @Override
            public void onFeedAdLoad(List<TTFeedAd> ads) {
                if (ads == null || ads.isEmpty()) {
                    return;
                }
                List<NativeAdData> adDatas = new ArrayList<>();

                Log.i(TAG, "tt size:" + ads.size());
                for (int i = 0; i < ads.size(); i++) {
                    TTFeedAd dataRef = ads.get(i);
                    NativeAdData data = new NativeAdData(mContext);
                    data.title = dataRef.getTitle();
                    data.description = dataRef.getDescription();
                    data.img_url = dataRef.getImageList().get(0).getImageUrl();
                    data.img2_url = dataRef.getIcon().getImageUrl();
                    if (mCurrentSdkData != null) {
                        data.pos_width = mCurrentSdkData.pos_width;
                        data.pos_height = mCurrentSdkData.pos_height;
                        data.req_width = mCurrentSdkData.req_width;
                        data.req_height = mCurrentSdkData.req_height;
                        data.impression_link_cc = mCurrentSdkData.impression_link_cc;
                        data.click_link_cc = mCurrentSdkData.click_link_cc;
                    }

                    data.setNativeType(2);
                    data.setTTData(dataRef);

                    adDatas.add(data);
                }

                if (mListener != null) {
                    mListener.onADLoaded(adDatas);
                }
            }
        });
    }

    // BaiDu
    private void getBaiDuNativeAd(final String appId, final String posId) {
        Log.i(TAG, "getBaiDuNativeAd");
        AdView.setAppSid(mContext, appId);

        BaiduNative baidu = new BaiduNative(mContext, posId, new BaiduNative.BaiduNativeNetworkListener() {

            @Override
            public void onNativeFail(NativeErrorCode arg0) {
                Log.w(TAG, "onNativeFail reason:" + arg0.name());
                if (mSdkDataList.size() > 0) {
                    handler.sendEmptyMessage(3);
                    return;
                }
                if (mListener != null) {
                    mListener.onADError(-1);
                }
            }

            @Override
            public void onNativeLoad(List<NativeResponse> arg0) {
                // 一个广告只允许展现一次，多次展现、点击只会计入一次
                Log.i(TAG, "onNativeLoad size:" + arg0.size());

                if (arg0.size() > 0) {
                    // demo仅简单地显示一条。可将返回的多条广告保存起来备用。
                    List<NativeAdData> adDatas = new ArrayList<>();

                    for (int i = 0; i < arg0.size(); i++) {
                        NativeResponse dataRef = arg0.get(i);
                        NativeAdData data = new NativeAdData(mContext);
                        data.title = dataRef.getTitle();
                        data.description = dataRef.getDesc();
                        data.img_url = dataRef.getImageUrl();
                        data.img2_url = dataRef.getIconUrl();
                        if (mCurrentSdkData != null) {
                            data.pos_width = mCurrentSdkData.pos_width;
                            data.pos_height = mCurrentSdkData.pos_height;
                            data.req_width = mCurrentSdkData.req_width;
                            data.req_height = mCurrentSdkData.req_height;
                            data.impression_link_cc = mCurrentSdkData.impression_link_cc;
                            data.click_link_cc = mCurrentSdkData.click_link_cc;
                        }

                        data.setNativeType(3);
                        data.setBaiDuData(dataRef);

                        adDatas.add(data);
                    }
                    if (mListener != null) {
                        mListener.onADLoaded(adDatas);
                    }
                }
            }

        });

        RequestParameters requestParameters =
                new RequestParameters.Builder()
                        .downloadAppConfirmPolicy(
                                RequestParameters.DOWNLOAD_APP_CONFIRM_ONLY_MOBILE).build();

        baidu.makeRequest(requestParameters);
    }
}
