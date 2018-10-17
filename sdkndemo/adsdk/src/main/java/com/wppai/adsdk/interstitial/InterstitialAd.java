package com.wppai.adsdk.interstitial;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;

import com.wppai.adsdk.R;
import com.wppai.adsdk.base.AdApi;
import com.wppai.adsdk.nativ.NativeAdData;

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

public class InterstitialAd {
    public final static String TAG = "InterstitialAd";

    private final int TYPE = 2;

    private InterstitialAdListener mListener;

    private Context mContext;
    private String mAppId;
    private String mPosId;
    private int mAdCount = 1;

    private PopupWindow mPopupWindow;
    private ImageView mAdIv;
    private InterstitialAdData mIntersititialAdData;

    private float mDownX = 0;
    private float mDownY = 0;
    private float mUpX = 0;
    private float mUpY = 0;

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
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
                case 2:
            }
        }
    };

    public InterstitialAd(final Context ctx, final String appId, final String posId, int adCount) {
        this(ctx,appId,posId);
        mAdCount = adCount;
    }

    public InterstitialAd(final Context ctx, final String appId, final String posId) {

        mContext = ctx;
        mAppId = appId;
        mPosId = posId;

        View layout = LayoutInflater.from(mContext).inflate(R.layout.sdk_popupwindow_view, null);
        if(mPopupWindow == null) {
            mPopupWindow = new PopupWindow(layout,
                    (int) (getWidth(mContext) * 0.7),
                    (int) (getHeight(mContext) * 0.61),
                    false);
            mPopupWindow.setBackgroundDrawable(new ColorDrawable(0xeeffee));
            mPopupWindow.setOutsideTouchable(false);
            mPopupWindow.setFocusable(false);
        }

        layout.findViewById(R.id.sdk_cancel_popupwindow_iv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "click cancel");
                closeAsPopupWindow();
            }
        });

        mAdIv = layout.findViewById(R.id.sdk_ad_popupwindow_iv);

        mAdIv.setOnTouchListener(mOnTouchListener);
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

                    int transformDownX = (int) mDownX;
                    int transformDownY = (int) mDownY;

                    int transformUpX = (int) mUpX;
                    int transformUpY = (int) mUpY;


                    Log.i(TAG, "transform down x: " + transformDownX);
                    Log.i(TAG, "transform down y: " + transformDownY);
                    Log.i(TAG, "transform up x: " + transformUpX);
                    Log.i(TAG, "transform up y: " + transformUpY);
                    if (mIntersititialAdData != null) {
                        mIntersititialAdData.onClicked(transformDownX, transformDownY, transformUpX, transformUpY);
                    }

                    if (mListener != null) {
                        mListener.onADClicked();
                    }

                    break;
            }
            return true;
        }
    };

    private int getWidth(Context applicationContext) {
        WindowManager windowManager = (WindowManager) applicationContext.getSystemService(Context.WINDOW_SERVICE);
        int width = windowManager.getDefaultDisplay().getWidth();
        return width;
    }

    private int getHeight(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int height = windowManager.getDefaultDisplay().getHeight();
        return  height;
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
                        JSONArray array = obj.optJSONObject("data").optJSONObject(mPosId).optJSONArray("list");
                        final List<InterstitialAdData> adDatas = new ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            InterstitialAdData data = new InterstitialAdData(mContext);
                            data.ad_id = array.optJSONObject(i).optString("ad_id");
                            data.impression_link = array.optJSONObject(i).optString("impression_link");
                            data.click_link = array.optJSONObject(i).optString("click_link");
                            data.conversion_link = array.optJSONObject(i).optString("conversion_link");
                            data.interact_type = array.optJSONObject(i).optInt("interact_type");
                            data.is_full_screen_interstitial = array.optJSONObject(i).optBoolean("is_full_screen_interstitial");
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
                            data.package_name = array.optJSONObject(i).optString("package_name");
                            adDatas.add(data);
                        }

                        if (adDatas.size() > 0) {
                            mIntersititialAdData = adDatas.get(0);
                            if (!mIntersititialAdData.is_full_screen_interstitial) {
                                Log.i(TAG, "kbg, get ad again");
                                //loadAd();
                                //return;
                            }

                            new DownloadImageTask(mAdIv).execute(mIntersititialAdData.img_url);
                            Log.i(TAG, "kbg, download image task xxxxxx");
                        }

                        Message message = new Message();
                        message.what = 1;
                        handler.sendMessage(message);

                        for (int i = 0; i < adDatas.size(); i++) {
                            Log.i(TAG, "ad_id:" + adDatas.get(i).ad_id);
                            Log.i(TAG, "impression_link:" + adDatas.get(i).impression_link);
                            Log.i(TAG, "click_link:" + adDatas.get(i).click_link);
                            Log.i(TAG, "conversion_link:" + adDatas.get(i).conversion_link);
                            Log.i(TAG, "is_full_screen_interstitial:" + adDatas.get(i).is_full_screen_interstitial);
                            Log.i(TAG, "interact_type:" + adDatas.get(i).interact_type);
                            Log.i(TAG, "crt_type:" + adDatas.get(i).crt_type);
                            Log.i(TAG, "img_url:" + adDatas.get(i).img_url);
                            Log.i(TAG, "impression_link_cc:" + adDatas.get(i).impression_link_cc);
                            Log.i(TAG, "click_link_cc:" + adDatas.get(i).click_link_cc);
                            Log.i(TAG, "req_width:" + adDatas.get(i).req_width);
                            Log.i(TAG, "req_height:" + adDatas.get(i).req_height);
                            Log.i(TAG, "pos_width:" + adDatas.get(i).pos_width);
                            Log.i(TAG, "pos_height:" + adDatas.get(i).pos_height);
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

    public void setADListener(InterstitialAdListener listener) {
        mListener = listener;
    }

    public void showAsPopupWindow() {
        handler.sendEmptyMessage(2);
            ((Activity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mPopupWindow.showAtLocation(((Activity) mContext).getWindow().getDecorView(), Gravity.CENTER, 0, 25);
                    if (mIntersititialAdData != null) {
                        mIntersititialAdData.onExposured();
                    }
                }
            });
    }

    public void closeAsPopupWindow() {
        if (mPopupWindow != null) {
            mPopupWindow.dismiss();
        }
    }

    public void closeAdInterstitial() {
        if (mPopupWindow != null) {
            mPopupWindow.dismiss();
            mPopupWindow = null;
        }
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

    public interface InterstitialAdListener {
        void onADReceive();

        void onADClicked();

        void onNoAD();

        void onADError(int error);
    }
    /*{"data":{"55678":{"list":[{
    "ad_id":"56953624",
    "impression_link":"http:\/\/v.gdt.qq.com\/gdRxGXrvySWQpK0HHPaY&i=1&os=2&datatype=json",
    "click_link":"http:\/\/c.gdt.qq.com\/gdt_m_X__%22%2C%22up_y%22%3A%22__UP_Y__%22%7D",
    "conversion_link":"http:\/\/t.gdt.qq.com\/ck_id=__CLICK_ID__&product_id=1105380575",
    "interact_type":1,
    "is_full_screen_interstitial":true,
    "crt_type":2,
    "img_url":"http:\/\/pgdt.ugdtimg.com\/gdt\/0\/DAAL10CAKAAPAAA9BbI3qbDJpxsmTV.jpg\/0?ck=ff06afd423aaa42a57de61636b8b4cb5",
    "impression_link_cc":"http:\/\/api.adx.scloud.lfengmobile.com\/api\/v1\/report\/expose?report_id=17434",
    "click_link_cc":"http:\/\/api.adx.scloud.lfengmobDOWN_Y__&up_x=__UP_X__&up_y=__UP_Y__&report_id=17434",
    "req_width":"600",
    "req_height":"500",
    "pos_width":null,
    "pos_height":null}]}},
    "ret":0,"msg":""}
    */
}
