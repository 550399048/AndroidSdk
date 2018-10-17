package com.wppai.adsdk.base;

import android.content.Context;
import android.util.Log;

import com.wppai.adsdk.comm.ConstantUtil;
import com.wppai.adsdk.comm.SystemUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AdApi {
    public final static String TAG = "AdApi";
    private static final String TEST_FILE_NAME = "adsdktest";
    private final String REQUEST_URL= "http://api.adx.wppapi.com/api/v1";
    private final String REQUEST_URL_BATE= "http://beta-api.adx.wppapi.com/api/v1";

    private static AdApi instance;

    static {
        instance = new AdApi();
    }

    public static AdApi getInstance() {
        return instance;
    }

    public String getUrl(final Context context, final String appId, final String posId, final int type, int adCount, String idsStr) {
        final Map<String, String> map = new HashMap<>();
        map.put("api_version", getApiVersionField());
        map.put("pos", getPosField(posId, type,adCount,idsStr));
        map.put("media", getMediaField(context, appId));
        map.put("device", getDeviceField(context));
        map.put("network", getNetworkField(context));

        String url = (hasUrlTestFile(context) ? REQUEST_URL_BATE : REQUEST_URL) + "?" + compose(map);
        Log.d(TAG, "url:" + url);

        return url;
    }

    public String getUrl(final Context context, final String appId, final String posId, final int type, int adCount) {
        final Map<String, String> map = new HashMap<>();
        map.put("api_version", getApiVersionField());
        map.put("pos", getPosField(posId, type,adCount));
        map.put("media", getMediaField(context, appId));
        map.put("device", getDeviceField(context));
        map.put("network", getNetworkField(context));

        String url = (hasUrlTestFile(context) ? REQUEST_URL_BATE : REQUEST_URL) + "?" + compose(map);
        Log.d(TAG, "url:" + url);

        return url;
    }

    private boolean hasUrlTestFile(Context context) {
        File cacheDir = context.getExternalCacheDir();
        if (cacheDir != null) {
            File testFile = new File(cacheDir, TEST_FILE_NAME);
            return testFile.exists();
        }
        return false;
    }

    private String getApiVersionField() {
        return "1.0";
    }

    private String getPosField(String posId, int type, int adCount, String idsStr ) {

        JSONObject object = new JSONObject();
        try {
            object.put("id", posId);
            object.put("width", getPosWidthByAdType(type));
            object.put("height", getPosHeightByAdType(type));
            object.put("support_full_screen_interstitial", true);
            object.put("ad_count", adCount);
            object.put(ConstantUtil.KEY_POS_LAST_IDS,idsStr);
            //object.put("need_rendered_ad", true);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            String encodeJsonObj = URLEncoder.encode(object.toString(), "utf-8");
            return encodeJsonObj;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getPosField(String posId, int type, int adCount) {

        JSONObject object = new JSONObject();
        try {
            object.put("id", posId);
            object.put("width", getPosWidthByAdType(type));
            object.put("height", getPosHeightByAdType(type));
            object.put("support_full_screen_interstitial", true);
            object.put("ad_count", adCount);
            //object.put("need_rendered_ad", true);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            String encodeJsonObj = URLEncoder.encode(object.toString(), "utf-8");
            return encodeJsonObj;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getMediaField(Context context, String appId) {

        JSONObject object = new JSONObject();
        try {
            object.put("app_id", appId);
            object.put("app_bundle_id", SystemUtil.getPackageName(context));
            object.put("app_version",SystemUtil.getAppVersion(context));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            String encodeJsonObj = URLEncoder.encode(object.toString(), "utf-8");
            Log.e(TAG, "getGDTMediaField, encodeJsonObj:" + encodeJsonObj);

            return encodeJsonObj;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getDeviceField(Context context) {

        JSONObject object = new JSONObject();

        try {
            object.put("os", "android");
            object.put("os_version", SystemUtil.getSystemVersion());
            object.put("model", SystemUtil.getSystemModel());
            object.put("manufacturer", SystemUtil.getSystemManufacturer());
            object.put("device_type", 1);
            object.put("imei", SystemUtil.getIMEI(context));
            object.put("android_id", SystemUtil.getAndroidId(context));
            object.put("screen_width",SystemUtil.getScreenWidth(context));
            object.put("screen_height",SystemUtil.getScreenHeight(context));
            object.put("screen_density",SystemUtil.getScreenDensity(context));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            String encodeJsonObj = URLEncoder.encode(object.toString(), "utf-8");
            Log.e(TAG, "getGDTDeviceField, encodeJsonObj:" + encodeJsonObj);

            return encodeJsonObj;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getNetworkField(final Context ctx) {

        JSONObject object = new JSONObject();
        try {
            object.put("connect_type", SystemUtil.getNetworkType(ctx));
            object.put("carrier", SystemUtil.getNetworkCarrier(ctx));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            String encodeJsonObj = URLEncoder.encode(object.toString(), "utf-8");
            Log.e(TAG, "getGDTNetworkField, encodeJsonObj:" + encodeJsonObj);

            return encodeJsonObj;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String compose(Map<String, String> params) {
        if (params != null) {
            //Log.d(TAG, "compose() -->> params.size() = " + params.size());
        }
        StringBuffer paramStr = new StringBuffer();
        if (params != null && !params.isEmpty()) {
            Set<String> keySet = params.keySet();
            ArrayList<String> keyList = new ArrayList<String>(keySet);
            Collections.sort(keyList);// 排序
            for (int i = 0, size = keyList.size(); i < size; i++) {
                String key = keyList.get(i);
                String value = params.get(key);
                //Log.d(TAG, "compose() -->> key = " + key + " value = " + value);
                if (i != 0) {
                    paramStr.append("&");
                }
                paramStr.append(key);
                paramStr.append("=");
                paramStr.append(value);
            }
        }
        //Log.d(TAG, "compose() -->> paramStr = " + paramStr.toString());
        return paramStr.toString();
    }

    /*
    type:
    原生: 0
    Banner: 1
    插屏: 2
    开屏: 3
    视频: 4
    信息流: 5
    */
    private String getPosWidthByAdType(int type) {
        if (type == 0) {
            return "0";
        } else if (type == 1) {
            return "640";
        } else if (type == 2) {
            return "600";
        } else if (type == 3) {
            return "640";
        } else if (type == 4) {
            return "640";
        } else if (type == 5) {
            return "1280";
        }
        return "0";
    }

    private String getPosHeightByAdType(int type) {
        if (type == 0) {
            return "0";
        } else if (type == 1) {
            return "100";
        } else if (type == 2) {
            return "500";
        } else if (type == 3) {
            return "960";
        } else if (type == 4) {
            return "360";
        } else if (type == 5) {
            return "720";
        }
        return "0";
    }
}
