package com.wppai.adsdk.banner;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.baidu.mobads.AdView;
import com.baidu.mobads.AdViewListener;
import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdDislike;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTAppDownloadListener;
import com.bytedance.sdk.openadsdk.TTBannerAd;
import com.qq.e.ads.banner.ADSize;
import com.qq.e.ads.banner.AbstractBannerADListener;
import com.qq.e.ads.banner.BannerView;
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

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class BannerAdView extends FrameLayout {
    public final static String TAG = "BannerAd";

    private final int TYPE = 1;

    private BannerAdListener mListener;

    private Context mContext;
    private String mAppId;
    private String mPosId;
    private int mAdCount = 1;

    private BannerAdData mBannerAdData;

    private float mDownX = 0;
    private float mDownY = 0;
    private float mUpX = 0;
    private float mUpY = 0;

    private int mScreenWidth = 0;
    private int mScreenHeight = 0;
    private float mDensity = 0;

    static private int AD_HEIGHT = 64;

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    handleData();
                    if (mListener != null) {
                        mListener.onADReceive();
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
    private boolean mIsSetBackgroup = false;

    public BannerAdView(final Context ctx, final String appId, final String posId, int adCount, BannerAdListener listener) {
        this(ctx, appId, posId, listener);
        mAdCount = adCount;
    }

    public BannerAdView(final Context ctx, final String appId, final String posId, BannerAdListener listener) {
        super(ctx);

        mContext = ctx;
        mAppId = appId;
        mPosId = posId;
        mListener = listener;

        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        mScreenWidth = dm.widthPixels;
        mScreenHeight = dm.heightPixels;
        mDensity = dm.density;

        Log.i(TAG, "screen width: " + mScreenWidth);
        Log.i(TAG, "screen height: " + mScreenHeight);
        Log.i(TAG, "screen density: " + mDensity);
    }

    public void setBannerBackgroup(boolean isSetBackgroup) {
        mIsSetBackgroup = isSetBackgroup;
    }


    public void loadAd() {
        String url = AdApi.getInstance().getUrl(mContext, mAppId, mPosId, TYPE, mAdCount);

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
                        JSONObject dataObject = obj.getJSONObject("data");
                        if (dataObject != null) {
                            JSONArray array = dataObject.optJSONObject(mPosId).optJSONArray("list");
                            final List<BannerAdData> adDatas = new ArrayList<>();
                            for (int i = 0; i < array.length(); i++) {
                                BannerAdData data = new BannerAdData(mContext);
                                data.ad_id = array.optJSONObject(i).optString("ad_id");
                                data.impression_link = array.optJSONObject(i).optString("impression_link");
                                data.click_link = array.optJSONObject(i).optString("click_link");
                                data.conversion_link = array.optJSONObject(i).optString("conversion_link");
                                data.interact_type = array.optJSONObject(i).optInt("interact_type");
                                data.crt_type = array.optJSONObject(i).optInt("crt_type");
                                data.rel_type = array.optJSONObject(i).optInt("rel_type");
                                data.open_type = array.optJSONObject(i).optInt("open_type");
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
                                adDatas.add(data);
                            }


                            if (adDatas.size() > 0) {
                                mBannerAdData = adDatas.get(0);
                                // new DownloadImageTask(mAdIv).execute(mBannerAdData.img_url);
                                Message message = new Message();
                                message.what = 1;
                                handler.sendMessage(message);
                            }

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

    // private BannerAdData mBannerAdData;
    private void handleData() {
        int crt_type = mBannerAdData.crt_type;
        int rel_type = mBannerAdData.rel_type;
        Log.i(TAG, "handle data, type: " + crt_type + " ,rel_type : " + rel_type);
        removeAllViews();

        if (crt_type == 1) {
            displayBannerForTitleDes(mBannerAdData);
        } else if (crt_type == 2) {
            displayBannerForImg(mBannerAdData);
        } else if (crt_type == 7) {
            displayBannerForImgText(mBannerAdData);
        }

        mBannerAdData.onExposured();
    }

    private void displayBannerForTitleDes(BannerAdData bannerAdData) {
        String title = bannerAdData.title;
        String description = bannerAdData.description;

        View layout = LayoutInflater.from(mContext).inflate(R.layout.sdk_banner_type_1_view, null);
        if (mIsSetBackgroup) {
            layout.setBackgroundResource(R.drawable.sdk_banner_backgroud);
        }

        TextView titleTv = layout.findViewById(R.id.sdk_banner_1_title_tv);
        titleTv.setText(title);

        TextView descriptionTv = layout.findViewById(R.id.sdk_banner_1_description_tv);
        descriptionTv.setText(description);
        ImageView downloadTag = layout.findViewById(R.id.banner_download_tag);

        TextView htmlTag = layout.findViewById(R.id.banner_html_tag);
        if (bannerAdData.interact_type == 0) {
            htmlTag.setVisibility(VISIBLE);
            downloadTag.setVisibility(INVISIBLE);
        } else {
            htmlTag.setVisibility(INVISIBLE);
            downloadTag.setVisibility(VISIBLE);
        }

        layout.setOnTouchListener(mOnTouchListener);

        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (AD_HEIGHT * mDensity));
        params.gravity = Gravity.CENTER_VERTICAL;
        addView(layout, params);
    }

    private void displayBannerForImg(BannerAdData bannerAdData) {
        String img_url = bannerAdData.img_url;

        Log.i(TAG, "handle data, img_url: " + img_url);
        View layout = LayoutInflater.from(mContext).inflate(R.layout.sdk_banner_type_2_view, null);

        ImageView imageTv = layout.findViewById(R.id.sdk_banner_2_img_tv);
        new DownloadImageTask(imageTv).execute(img_url);

        layout.setOnTouchListener(mOnTouchListener);

        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (AD_HEIGHT * mDensity));
        params.gravity = Gravity.CENTER_VERTICAL;
        addView(layout, params);
    }

    private void displayBannerForImgText(BannerAdData bannerAdData) {
        String title = bannerAdData.title;
        String description = bannerAdData.description;
        String img_url = bannerAdData.img_url;

        View layout = LayoutInflater.from(mContext).inflate(R.layout.sdk_banner_type_7_view, null);
        if (mIsSetBackgroup) {
            layout.setBackgroundResource(R.drawable.sdk_banner_backgroud);
        }
        TextView titleTv = layout.findViewById(R.id.sdk_banner_7_title_tv);
        titleTv.setText(title);

        TextView descriptionTv = layout.findViewById(R.id.sdk_banner_7_description_tv);
        descriptionTv.setText(description);

        ImageView imageTv = layout.findViewById(R.id.sdk_banner_7_img_tv);
        new DownloadImageTask(imageTv).execute(img_url);

        TextView htmlTag = layout.findViewById(R.id.banner_html_tag);

        ImageView downloadTag = layout.findViewById(R.id.banner_download_tag);
        if (bannerAdData.interact_type == 0) {
            htmlTag.setVisibility(VISIBLE);
            downloadTag.setVisibility(INVISIBLE);
        } else {
            htmlTag.setVisibility(INVISIBLE);
            downloadTag.setVisibility(VISIBLE);
        }


        layout.setOnTouchListener(mOnTouchListener);

        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (AD_HEIGHT * mDensity));
        params.gravity = Gravity.CENTER_VERTICAL;
        addView(layout, params);

    }

    private OnTouchListener mOnTouchListener = new OnTouchListener() {

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

                    int maxY = mScreenWidth * Integer.valueOf(mBannerAdData.req_height) / Integer.valueOf(mBannerAdData.req_width);

                    if (mUpY > maxY) {
                        return true;
                    }

                    int transformDownX = (int) mDownX * Integer.valueOf(mBannerAdData.req_width) / mScreenWidth;
                    int transformDownY = (int) mDownY * Integer.valueOf(mBannerAdData.req_height) / maxY;

                    int transformUpX = (int) mUpX * Integer.valueOf(mBannerAdData.req_width) / mScreenWidth;
                    int transformUpY = (int) mUpY * Integer.valueOf(mBannerAdData.req_height) / maxY;


                    Log.i(TAG, "transform down x: " + transformDownX);
                    Log.i(TAG, "transform down y: " + transformDownY);
                    Log.i(TAG, "transform up x: " + transformUpX);
                    Log.i(TAG, "transform up y: " + transformUpY);
                    if (mBannerAdData != null) {
                        mBannerAdData.onClicked(transformDownX, transformDownY, transformUpX, transformUpY);
                    }

                    if (mListener != null) {
                        mListener.onADClicked();
                    }

                    break;
            }
            return true;
        }
    };

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

    public interface BannerAdListener {
        void onADReceive();

        void onADClicked();

        void onADError(int error);
    }
    /*{"data":{"55677":{"list":[
    {"ad_id":"39497251",
    "impression_link":"http:S1maRxGXrvyfwvS_cuwbGA&i=1&os=2&datatype=json",
    "click_link":"http:22%2C%22up_x%22%3A%22__UP_X__%22%2C%22up_y%22%3A%22__UP_Y__%22%7D",
    "conversion_link":"http:_ACTION_ID__&click_id=__CLICK_ID__&product_id=1104264007",
    "interact_type":1,
    "crt_type":7,
    "title":"360清理大师",
    "description":"内存不足，清理大师一键清除！",
    "img_url":"http:\6cfadad9700dde5b8096f663f80c9",
    "impression_link_cc":"http:\/\/api.adx.scloud.lfengmobile.com\/api\/v1\/report\/expose?report_id=18321",
    "click_link_cc":"http:\/\/api.adx.scloudown_y=__DOWN_Y__&up_x=__UP_X__&up_y=__UP_Y__&report_id=18321",
    "req_width":"640",
    "req_height":"100",
    "pos_width":null,
    "pos_height":null}]}},
    "ret":0,"msg":""}*/

    /*{"data":{"55677":{"list":[
    {"ad_id":"56457883",
    "impression_link":"http781Mwv9YpLfkFXQLy1maRxGXrvyYxjQEQbC3k0&i=1&os=2&datatype=json",
    "click_link":"http:n_y%22%3A%22__DOWN_Y__%22%2C%22up_x%22%3A%22__UP_X__%22%2C%22up_y%22%3A%22__UP_Y__%22%7D",
    "conversion_link":"http:\/\/t.gdt.qq.com\/conv\/allialick_id=__CLICK_ID__&product_id=1104081998",
    "interact_type":1,
    "crt_type":2,
    "img_url":"http:\/T3.jpg\/0?ck=e01ec74839727bd9e062161667719626",
    "impression_link_cc":"http:\/\/areport\/expose?report_id=18322",
    "click_link_cc":"http:wn_x=__DOWN_X__&down_y=__DOWN_Y__&up_x=__UP_X__&up_y=__UP_Y__&report_id=18322",
    "req_width":"640",
    "req_height":"100",
    "pos_width":null,
    "pos_height":null}]}},
    "ret":0,"msg":""}*/

    /*{"data":{"55677":{"list":[
    {"ad_id":"49998654",
    "impression_link":"http:\&datatype=json",
    "click_link":"httpHN_Y__%22%2C%22up_x%22%3A%22__UP_X__%22%2C%22up_y%22%3A%22__UP_Y__%22%7D",
    "conversion_link":"http:_&product_id=1104264007",
    "interact_type":1,
    "crt_type":1,
    "title":"360清理大师",
    "description":"发现大量缓存垃圾，建议一键清理",
    "impression_link_cc":"http:i\/v1\/report\/expose?report_id=18340",
    "click_link_cc":"httpEIGHT__&down_x=__DOWN_X__&down_y=__DOWN_Y__&up_x=__UP_X__&up_y=__UP_Y__&report_id=18340",
    "req_width":"640",
    "req_height":"100",
    "pos_width":null,
    "pos_height":null}]}},
    "ret":0,"msg":""}*/
    //getGDTBannerAd("1101152570", "9079537218417626401");
    //getTTBannerAd("5001121", "901121895");
    //getBaiDuBannerAd("e866cfb0", "2015351");
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
            getGDTBannerAd(mCurrentSdkData.getAppId(), mCurrentSdkData.getPosId());
        } else if (mCurrentSdkData.getMediaId().equals("8")) {
            getBaiDuBannerAd(mCurrentSdkData.getAppId(), mCurrentSdkData.getPosId());
        } else if (mCurrentSdkData.getMediaId().equals("2")) {
            getTTBannerAd(mCurrentSdkData.getAppId(), mCurrentSdkData.getPosId());
        }
    }

    // GDT
    private void getGDTBannerAd(final String appId, final String posId) {
        Log.i(TAG, "getGDTBannerAd");

        BannerView banner = new BannerView((Activity) mContext, ADSize.BANNER, appId, posId);
        banner.setRefresh(0);
        banner.setADListener(new AbstractBannerADListener() {

            @Override
            public void onNoAD(AdError error) {
                if (mSdkDataList.size() > 0) {
                    handler.sendEmptyMessage(3);
                    return;
                }
                if (mListener != null) {
                    mListener.onADError(error.getErrorCode());
                }
            }

            @Override
            public void onADReceiv() {
                if (mListener != null) {
                    mListener.onADReceive();
                }
                if (mCurrentSdkData != null) {
                    mCurrentSdkData.onExposured();
                }
            }

            @Override
            public void onADClicked() {
                if (mCurrentSdkData != null) {
                    mCurrentSdkData.onClicked(0, 0, 0, 0);
                }
            }
        });
        banner.loadAD();

        removeAllViews();
        addView(banner);
    }

    // TT, 头条, 网盟
    private void getTTBannerAd(final String appId, final String posId) {
        Log.i(TAG, "getTTBannerAd");

        Context appContext = mContext.getApplicationContext();
        TTAdManagerHolder.getInstance(appContext).setAppId(appId);
        TTAdManagerHolder.getInstance(appContext).requestPermissionIfNecessary(appContext);

        AdSlot adSlot = new AdSlot.Builder()
                .setCodeId(posId)
                .setSupportDeepLink(true)
                .setImageAcceptedSize(600, 257)
                .build();
        TTAdNative mTTAdNative = TTAdManagerHolder.getInstance(mContext).createAdNative(mContext);

        mTTAdNative.loadBannerAd(adSlot, new TTAdNative.BannerAdListener() {

            @Override
            public void onError(int code, String message) {
                if (mSdkDataList.size() > 0) {
                    handler.sendEmptyMessage(3);
                    return;
                }
                if (mListener != null) {
                    mListener.onADError(code);
                }
            }

            @Override
            public void onBannerAdLoad(final TTBannerAd ad) {
                if (ad == null) {
                    return;
                }
                View bannerView = ad.getBannerView();
                if (bannerView == null) {
                    return;
                }
                if (mListener != null) {
                    mListener.onADReceive();
                }

                //设置轮播的时间间隔
                ad.setSlideIntervalTime(30 * 1000);
                removeAllViews();
                addView(bannerView);
                ad.setBannerInteractionListener(new TTBannerAd.AdInteractionListener() {
                    @Override
                    public void onAdClicked(View view, int type) {
                        if (mListener != null) {
                            mListener.onADClicked();
                        }
                        if (mCurrentSdkData != null) {
                            mCurrentSdkData.onClicked(0, 0, 0, 0);
                        }
                    }

                    @Override
                    public void onAdShow(View view, int type) {
                        if (mCurrentSdkData != null) {
                            mCurrentSdkData.onExposured();
                        }
                    }
                });
                ad.setDownloadListener(new TTAppDownloadListener() {
                    @Override
                    public void onIdle() {
                    }

                    @Override
                    public void onDownloadActive(long totalBytes, long currBytes, String fileName, String appName) {
                    }

                    @Override
                    public void onDownloadPaused(long totalBytes, long currBytes, String fileName, String appName) {
                    }

                    @Override
                    public void onDownloadFailed(long totalBytes, long currBytes, String fileName, String appName) {
                    }

                    @Override
                    public void onInstalled(String fileName, String appName) {
                    }

                    @Override
                    public void onDownloadFinished(long totalBytes, String fileName, String appName) {
                    }
                });
                //在banner中显示网盟提供的dislike icon
                ad.setShowDislikeIcon(new TTAdDislike.DislikeInteractionCallback() {
                    @Override
                    public void onSelected(int position, String value) {
                    }

                    @Override
                    public void onCancel() {
                    }
                });
            }
        });
    }

    // Baidu
    private void getBaiDuBannerAd(final String appId, final String posId) {
        Log.i(TAG, "getBaiDuBannerAd");

        AdView.setAppSid(mContext, appId);

        AdView adView = new AdView(mContext, posId);
        // 设置监听器
        adView.setListener(new AdViewListener() {
            public void onAdSwitch() {
                Log.w(TAG, "onAdSwitch");
            }

            public void onAdShow(JSONObject info) {
                // 广告已经渲染出来
                Log.w(TAG, "onAdShow " + info.toString());
                if (mListener != null) {
                    mListener.onADReceive();
                }
                if (mCurrentSdkData != null) {
                    mCurrentSdkData.onExposured();
                }
            }

            public void onAdReady(AdView adView) {
                // 资源已经缓存完毕，还没有渲染出来
                Log.w(TAG, "onAdReady " + adView);
            }

            public void onAdFailed(String reason) {
                Log.w(TAG, "onAdFailed " + reason);
                if (mSdkDataList.size() > 0) {
                    handler.sendEmptyMessage(3);
                    return;
                }
                if (mListener != null) {
                    mListener.onADError(-1);
                }
            }

            public void onAdClick(JSONObject info) {
                // Log.w("", "onAdClick " + info.toString());
                if (mListener != null) {
                    mListener.onADClicked();
                }
                if (mCurrentSdkData != null) {
                    mCurrentSdkData.onClicked(0, 0, 0, 0);
                }
            }

            @Override
            public void onAdClose(JSONObject arg0) {
                Log.w(TAG, "onAdClose");
            }
        });

        removeAllViews();
        addView(adView);
    }
}
