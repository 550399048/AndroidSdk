package com.wppai.adsdk.splash;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.MainThread;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.baidu.mobads.AdView;
import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTSplashAd;
import com.qq.e.ads.splash.SplashAD;
import com.qq.e.ads.splash.SplashADListener;
import com.qq.e.comm.util.AdError;
import com.wppai.adsdk.R;
import com.wppai.adsdk.base.AdApi;
import com.wppai.adsdk.base.BaseSdkData;
import com.wppai.adsdk.tt.TTAdManagerHolder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class SplashAd {
    public final static String TAG = "SplashAd";

    private final int TYPE = 3;
    private final int SPLASH_DISMISS_TIME = 6000;
    private static final int GET_SDK_TT_TIME_OUT = 300;
    private static final int GET_SDK_BD_TIME_OUT = 1500;

    private SplashAdListener mListener;

    private Context mContext;
    private String mAppId;
    private String mPosId;
    private int mAdCount = 1;
    private ViewGroup mContainer;
    private ViewGroup mSkipContainer;

    private int mScreenWidth = 0;
    private int mScreenHeight = 0;
    private float mDownX = 0;
    private float mDownY = 0;
    private float mUpX = 0;
    private float mUpY = 0;

    private boolean mBaiduSDKTimeOutFlag = false;

    private static final int MSG_SWITCH_SDK = 3;
    private static final int MSG_AD_DISMISS = 4;
    private static final int MSG_BD_SDK_TIMEOUT = 5;

    private ImageView mSplashAdIv;
    private SplashAdData mSplashAdData;

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (mListener != null) {
                        mListener.onADPresent();
                    }
                    break;
                case 0:
                    int err = (int) msg.obj;
                    if (mListener != null) {
                        mListener.onADError(err);
                    }
                    break;
                case MSG_SWITCH_SDK:
                    switchToSdkAd();
                    break;
                case MSG_AD_DISMISS:
                    Log.i(TAG, "case 4");
                    if (mListener != null) {
                        mListener.onADDismissed();
                        mListener = null;
                    }
                    mSdkDataList.clear();
                    break;
                case MSG_BD_SDK_TIMEOUT:
                    Log.i(TAG, "case 5");
                    mBaiduSDKTimeOutFlag = true;
                    switchToSdkAd();
                    break;
                default:
                    break;
            }
        }
    };

    public SplashAd(final Context ctx, ViewGroup container, ViewGroup skipContainer, final String appId, final String posId, int adCount, SplashAdListener listener) {
        this(ctx, container, skipContainer, appId, posId, listener);
        mAdCount = adCount;
    }

    public SplashAd(final Context ctx, ViewGroup container, ViewGroup skipContainer, final String appId, final String posId, SplashAdListener listener) {

        mListener = listener;
        mContext = ctx;
        mContainer = container;
        mSkipContainer = skipContainer;
        mAppId = appId;
        mPosId = posId;

        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        mScreenWidth = dm.widthPixels;
        mScreenHeight = dm.heightPixels;
        Log.i(TAG, "screen width: " + mScreenWidth);
        Log.i(TAG, "screen height: " + mScreenHeight);

        mSplashAdIv = new ImageView(ctx);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mSplashAdIv.setLayoutParams(params);
        mSplashAdIv.setScaleType(ImageView.ScaleType.FIT_XY);
        mSplashAdIv.setOnTouchListener(mOnTouchListener);

        mContainer.addView(mSplashAdIv);

        View layout = LayoutInflater.from(mContext).inflate(R.layout.sdk_skip_view, null);
        skipContainer.addView(layout);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "click skip");
                if (mListener != null) {
                    mListener.onADDismissed();
                }
            }
        });
    }

    public View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.i(TAG, "down x: " + event.getX());
                    Log.i(TAG, "down y: " + event.getY());
                    mDownX = event.getX();
                    mDownY = event.getY();

                    break;
                case MotionEvent.ACTION_UP:
                    Log.i(TAG, "up x: " + event.getX());
                    Log.i(TAG, "up y: " + event.getY());
                    mUpX = event.getX();
                    mUpY = event.getY();

                    if (mDownX < 0 || mDownY < 0 || mUpX < 0 || mUpY < 0) {
                        return true;
                    }
                    if (mSplashAdData == null || mSplashAdData.req_height == null || mSplashAdData.req_width == null) {
                        return false;
                    }

                    int maxY = mScreenWidth * Integer.valueOf(mSplashAdData.req_height) / Integer.valueOf(mSplashAdData.req_width);

                    if (mUpY > maxY) {
                        return true;
                    }

                    int transformDownX = (int) mDownX * Integer.valueOf(mSplashAdData.req_width) / mScreenWidth;
                    int transformDownY = (int) mDownY * Integer.valueOf(mSplashAdData.req_height) / maxY;

                    int transformUpX = (int) mUpX * Integer.valueOf(mSplashAdData.req_width) / mScreenWidth;
                    int transformUpY = (int) mUpY * Integer.valueOf(mSplashAdData.req_height) / maxY;


                    Log.i(TAG, "transform down x: " + transformDownX);
                    Log.i(TAG, "transform down y: " + transformDownY);
                    Log.i(TAG, "transform up x: " + transformUpX);
                    Log.i(TAG, "transform up y: " + transformUpY);
                    if (mSplashAdData != null) {
                        mSplashAdData.onClicked(transformDownX, transformDownY, transformUpX, transformUpY);
                    }

                    if (mListener != null) {
                        mListener.onADClicked();
                    }

                    break;
            }
            return true;
        }
    };


    public void loadAd() {
        String url = AdApi.getInstance().getUrl(mContext, mAppId, mPosId, TYPE, mAdCount);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)//设置连接超时时间
                .readTimeout(3, TimeUnit.SECONDS)//设置读取超时时间
                .build();
        Request request = new Request.Builder().get().url(url).build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i(TAG, "onFailure, e:" + e.toString());
                Message message = new Message();
                message.what = 0;
                message.obj = -1;
                handler.sendMessage(message);
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                try {
                    JSONObject obj = new JSONObject(response.body().string());

                    int ret = obj.optInt("ret");
                    Log.i(TAG, "ret:" + ret);
                    Log.i(TAG, "obj:" + obj.toString());

                    if (ret == 0) {
                        JSONObject jsonObject = obj.optJSONObject("data");
                        if (jsonObject != null) {
                            JSONArray array = jsonObject.optJSONObject(mPosId).optJSONArray("list");
                            final List<SplashAdData> adDatas = new ArrayList<>();
                            for (int i = 0; i < array.length(); i++) {
                                SplashAdData data = new SplashAdData(mContext);
                                data.ad_id = array.optJSONObject(i).optString("ad_id");
                                data.impression_link = array.optJSONObject(i).optString("impression_link");
                                data.click_link = array.optJSONObject(i).optString("click_link");
                                data.conversion_link = array.optJSONObject(i).optString("conversion_link");
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
                                adDatas.add(data);
                            }

                            if (adDatas.size() > 0) {
                                mSplashAdData = adDatas.get(0);

                                new DownloadImageTask(mSplashAdIv).execute(mSplashAdData.img_url);
                                Log.i(TAG, "download image task");

                                mSplashAdData.onExposured();
                            }
                            handler.sendEmptyMessageDelayed(MSG_AD_DISMISS, SPLASH_DISMISS_TIME);

                            Message message = new Message();
                            message.what = 1;
                            handler.sendMessage(message);

                            /*for (int i = 0; i < adDatas.size(); i++) {
                                Log.i(TAG, "ad_id:" + adDatas.get(i).ad_id);
                                Log.i(TAG, "impression_link:" + adDatas.get(i).impression_link);
                                Log.i(TAG, "click_link:" + adDatas.get(i).click_link);
                                Log.i(TAG, "conversion_link:" + adDatas.get(i).conversion_link);
                                Log.i(TAG, "interact_type:" + adDatas.get(i).interact_type);
                                Log.i(TAG, "crt_type:" + adDatas.get(i).crt_type);
                                Log.i(TAG, "img_url:" + adDatas.get(i).img_url);
                                Log.i(TAG, "impression_link_cc:" + adDatas.get(i).impression_link_cc);
                                Log.i(TAG, "click_link_cc:" + adDatas.get(i).click_link_cc);
                                Log.i(TAG, "req_width:" + adDatas.get(i).req_width);
                                Log.i(TAG, "req_height:" + adDatas.get(i).req_height);
                                Log.i(TAG, "pos_width:" + adDatas.get(i).pos_width);
                                Log.i(TAG, "pos_height:" + adDatas.get(i).pos_height);
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
                            handler.sendEmptyMessage(MSG_SWITCH_SDK);
                            handler.sendEmptyMessageDelayed(MSG_AD_DISMISS, SPLASH_DISMISS_TIME);
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

    public interface SplashAdListener {
        void onADPresent();

        void onADClicked();

        void onADDismissed();

        void onNoAD();

        void onADError(int error);
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

    /*{"data":{"55679":{"list":[
      {"ad_id":"56320979",
       "impression_link":"http:\/\/v.gdt.qq.com\/gdt_stats.fcg?viewid282FoTODnu5WwFNCsdPdyEz0bSplE&i=1&os=2&datatype=json",
       "click_link":"http:\/\/c.gdt.22down_y%22%3A%22__DOWN_Y__%22%2C%22up_x%22%3A%22__UP_X__%22%2C%22up_y%22%3A%22__UP_Y__%22%7D",
       "conversion_link":"http:\/\/t.gdt/conv?client=6&action_id=__ACTION_ID__&click_id=__CLICK_ID__&product_id=1105602870",
       "interact_type":1,
       "crt_type":2,
       "img_url":"http:\/\/pgdt.ugdtimg.com\/gdt\/0\/DAAVUSmAKvC8p62mFi.jpg\/0?ck=3d69c3da9376aa9e71e4cfa476c74202",
       "impression_link_cc":"http:\/\/api.adx.scloud.lfengmobile.com\/api\/v1\/report\/expose?report_id=16950",
       "click_link_cc":"http:\/\/api.a_x=__DOWN_X__&down_y=__DOWN_Y__&up_x=__UP_X__&up_y=__UP_Y__&report_id=16950",
       "req_width":"640",
       "req_height":"960",
       "pos_width":null,
       "pos_height":null}]}},"ret":0,"msg":""}*/
    //getGDTSplashAd("1101152570", "8863364436303842593");
    //getTTSplashAd("5001121", "801121648");
    //getBaiDuSplashAd("e866cfb0", "2058622");
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
            getGDTSplashAd(mCurrentSdkData.getAppId(), mCurrentSdkData.getPosId());
        } else if (mCurrentSdkData.getMediaId().equals("8")) {
            getBaiDuSplashAd(mCurrentSdkData.getAppId(), mCurrentSdkData.getPosId());
        } else if (mCurrentSdkData.getMediaId().equals("2")) {
            getTTSplashAd(mCurrentSdkData.getAppId(), mCurrentSdkData.getPosId());
        }
    }

    // GDT
    private SplashADListener mGDTListener = new SplashADListener() {

        @Override
        public void onADDismissed() {
            handler.removeMessages(MSG_AD_DISMISS);
            handler.sendEmptyMessage(MSG_AD_DISMISS);
        }

        @Override
        public void onNoAD(AdError adError) {
            if (mSdkDataList.size() > 0) {
                handler.sendEmptyMessage(MSG_SWITCH_SDK);
                return;
            }
            if (mListener != null) {
                mListener.onADError(adError.getErrorCode());
            }
        }

        @Override
        public void onADPresent() {
            if (mListener != null) {
                mListener.onADPresent();
            }
            if (mCurrentSdkData != null) {
                mCurrentSdkData.onExposured();
            }
        }

        @Override
        public void onADClicked() {
            if (mListener != null) {
                mListener.onADClicked();
            }
            if (mCurrentSdkData != null) {
                mCurrentSdkData.onClicked(0, 0, 0, 0);
            }
        }

        @Override
        public void onADTick(long l) {
        }

        @Override
        public void onADExposure() {
        }
    };

    private void getGDTSplashAd(final String appId, final String posId) {
        Log.i(TAG, "getGDTSplashAd");
        mSkipContainer.setVisibility(View.VISIBLE);
        SplashAD splashAD = new SplashAD((Activity) mContext, mContainer, mSkipContainer, appId, posId, mGDTListener, 0);
    }

    // TT, 头条, 网盟
    private void getTTSplashAd(final String appId, final String posId) {
        Log.i(TAG, "getTTSplashAd");
        mSkipContainer.setVisibility(View.INVISIBLE);

        Context appContext = mContext.getApplicationContext();
        TTAdManagerHolder.getInstance(appContext).setAppId(appId);
        TTAdManagerHolder.getInstance(appContext).requestPermissionIfNecessary(appContext);
        TTAdNative mTTAdNative = TTAdManagerHolder.getInstance(mContext).createAdNative(mContext);

        //开屏广告参数
        AdSlot adSlot = new AdSlot.Builder()
                .setCodeId(posId)
                .setSupportDeepLink(true)
                .setImageAcceptedSize(1080, 1920)
                .build();
        //调用开屏广告异步请求接口
        mTTAdNative.loadSplashAd(adSlot, new TTAdNative.SplashAdListener() {
            @Override
            @android.support.annotation.MainThread
            public void onError(int code, String message) {
                Log.i(TAG, "onError, code: " + code);
                if (mSdkDataList.size() > 0) {
                    handler.sendEmptyMessage(MSG_SWITCH_SDK);
                    return;
                }
                if (mListener != null) {
                    mListener.onADError(code);
                }
            }

            @Override
            @android.support.annotation.MainThread
            public void onTimeout() {
                Log.i(TAG, "onTimeout");
                if (mSdkDataList.size() > 0) {
                    handler.sendEmptyMessage(MSG_SWITCH_SDK);
                    return;
                }
                if (mListener != null) {
                    mListener.onADError(-1);
                }
            }

            @Override
            @MainThread
            public void onSplashAdLoad(TTSplashAd ad) {
                Log.i(TAG, "onSplashAdLoad");
                if (ad == null) {
                    Log.i(TAG, "onSplashAdLoad, ad == null");
                    return;
                }
                //获取SplashView
                View view = ad.getSplashView();
                //mSplashContainer.removeAllViews();
                //把SplashView 添加到ViewGroup中
                mContainer.addView(view);
                //设置不开启开屏广告倒计时功能以及不显示跳过按钮
                //ad.setNotAllowSdkCountdown();

                //设置SplashView的交互监听器
                ad.setSplashInteractionListener(new TTSplashAd.AdInteractionListener() {
                    @Override
                    public void onAdClicked(View view, int type) {
                        Log.i(TAG, "onAdClicked, type:" + type);
                        if (mListener != null) {
                            mListener.onADClicked();
                        }
                        if (mCurrentSdkData != null) {
                            mCurrentSdkData.onClicked(0, 0, 0, 0);
                        }
                        if (mListener != null && type == 3) {
                            handler.sendEmptyMessageDelayed(MSG_AD_DISMISS, 2000);
                        }
                    }

                    @Override
                    public void onAdShow(View view, int type) {
                        Log.i(TAG, "onAdShow, type:" + type);
                        if (mListener != null) {
                            mListener.onADPresent();
                        }
                        if (mCurrentSdkData != null) {
                            mCurrentSdkData.onExposured();
                        }
                    }

                    @Override
                    public void onAdSkip() {
                        Log.i(TAG, "onAdSkip");
                        handler.removeMessages(MSG_AD_DISMISS);
                        handler.sendEmptyMessage(MSG_AD_DISMISS);
                    }

                    @Override
                    public void onAdTimeOver() {
                        Log.i(TAG, "onAdTimeOver");
                        handler.removeMessages(MSG_AD_DISMISS);
                        handler.sendEmptyMessage(MSG_AD_DISMISS);
                    }
                });
            }
        }, GET_SDK_TT_TIME_OUT);
    }

    // Baidu
    private com.baidu.mobads.SplashAdListener mBaidDuSplashAdlistener = new com.baidu.mobads.SplashAdListener() {
        @Override
        public void onAdDismissed() {
            Log.i(TAG, "onAdDismissed");
            if (mBaiduSDKTimeOutFlag) {
                return;
            }
            handler.removeMessages(MSG_BD_SDK_TIMEOUT);
            handler.removeMessages(MSG_AD_DISMISS);
            handler.sendEmptyMessage(MSG_AD_DISMISS);
        }

        @Override
        public void onAdFailed(String arg0) {
            Log.i(TAG, "onAdFailed, reason: " + arg0);
            if (mBaiduSDKTimeOutFlag) {
                return;
            }
            handler.removeMessages(MSG_BD_SDK_TIMEOUT);

            if (mSdkDataList.size() > 0) {
                handler.sendEmptyMessage(MSG_SWITCH_SDK);
                return;
            }
            if (mListener != null) {
                mListener.onADError(-1);
            }
        }

        @Override
        public void onAdPresent() {
            Log.i(TAG, "onAdPresent");
            if (mBaiduSDKTimeOutFlag) {
                return;
            }
            handler.removeMessages(MSG_BD_SDK_TIMEOUT);

            if (mListener != null) {
                mListener.onADPresent();
            }
            if (mCurrentSdkData != null) {
                mCurrentSdkData.onExposured();
            }
        }

        @Override
        public void onAdClick() {
            Log.i(TAG, "onAdClick");
            if (mBaiduSDKTimeOutFlag) {
                return;
            }
            handler.removeMessages(MSG_BD_SDK_TIMEOUT);

            if (mListener != null) {
                mListener.onADClicked();
            }
            if (mCurrentSdkData != null) {
                mCurrentSdkData.onClicked(0, 0, 0, 0);
            }
        }
    };

    private void getBaiDuSplashAd(final String appId, final String posId) {
        Log.i(TAG, "getBaiDuSplashAd");
        mSkipContainer.setVisibility(View.VISIBLE);

        mBaiduSDKTimeOutFlag = false;
        handler.sendEmptyMessageDelayed(MSG_BD_SDK_TIMEOUT, GET_SDK_BD_TIME_OUT);

        AdView.setAppSid(mContext, appId);
        new com.baidu.mobads.SplashAd(mContext, mContainer, mBaidDuSplashAdlistener, posId, true);
    }
}
