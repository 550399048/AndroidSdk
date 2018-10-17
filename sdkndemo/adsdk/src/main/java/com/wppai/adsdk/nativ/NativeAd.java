package com.wppai.adsdk.nativ;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.wppai.adsdk.base.AdApi;
import com.wppai.adsdk.comm.AdCacheUtils;
import com.wppai.adsdk.comm.AdProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.annotation.Native;
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
                                JSONObject jsonObject = array.getJSONObject(i);
                                data.ad_id = jsonObject.optString("ad_id");
                                data.impression_link = jsonObject.optString("impression_link");
                                data.conversion_link = jsonObject.optString("conversion_link");
                                data.click_link = jsonObject.optString("click_link");
                                data.interact_type = jsonObject.optInt("interact_type");
                                data.crt_type = jsonObject.optInt("crt_type");
                                data.title = jsonObject.optString("title");
                                data.description = jsonObject.optString("description");
                                data.img_url = jsonObject.optString("img_url");
                                data.img2_url = jsonObject.optString("img2_url");
                                data.impression_link_cc = jsonObject.optString("impression_link_cc");
                                data.click_link_cc = jsonObject.optString("click_link_cc");
                                data.req_width = jsonObject.optString("req_width");
                                data.req_height = jsonObject.optString("req_height");
                                data.pos_width = jsonObject.optString("pos_width");
                                data.pos_height = jsonObject.optString("pos_height");
                                data.location_url = jsonObject.optString("location_url");
                                data.rel_type = jsonObject.optInt("rel_type");
                                data.open_type = jsonObject.optInt("open_type");
                                data.package_name = jsonObject.optString("package_name");
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

                            for (int i = 0; i < adDatas.size(); i++) {
                                Log.i(TAG, "ad_id:" + adDatas.get(i).ad_id);
                                Log.i(TAG,"conversion_link:" + adDatas.get(i).conversion_link);
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
                            }
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
}
