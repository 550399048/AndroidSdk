package com.wppai.adsdk.comm;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AdCacheUtils {

    private static final String APPID = "_appid";
    private static final String POSID = "_posid";
    private static final String SHARE_PREFERENCES_KEY = "com.wppai.adsdk.ad_cache_prefs";
    private static final String KEY_AD_CACHE_TIME = "wwp.report_last_ad_time";
    private static final String KEY_AD_CACHE_AD_IDS = "wwp.last_ad_ids";
    private static final int REPORT_SERVER_EXPIRE_MINUTE = 1;
    public static List<AdProvider> adProviderList = new ArrayList<>();

    public static void refreshRequestAdTime(Context context, String appId, String posId) {
        SharedPreferences sp = context.getSharedPreferences(SHARE_PREFERENCES_KEY, Context.MODE_PRIVATE);
        long timeMillis = System.currentTimeMillis() + REPORT_SERVER_EXPIRE_MINUTE * 60 * 1000;
        sp.edit().putLong(getAdCacheTimeKey(appId,posId), timeMillis).apply();
    }

    public static long getRequestAdTime (Context context, String appId, String posId) {
        SharedPreferences sp = context.getSharedPreferences(SHARE_PREFERENCES_KEY, Context.MODE_PRIVATE);
        return  sp.getLong(getAdCacheTimeKey(appId,posId), 0l);
    }

    private static String getAdCacheTimeKey(String appId, String posId) {
        return KEY_AD_CACHE_TIME + APPID + appId + POSID + posId;
    }

    private static String getAdCacheAdIdsKey (String appId, String posId) {
        return KEY_AD_CACHE_AD_IDS + APPID + appId + POSID + posId;
    }

    public static void putAdIdsToCache (Context context, String appId, String posId, String adIdsStr) {
        SharedPreferences sp = context.getSharedPreferences(SHARE_PREFERENCES_KEY,Context.MODE_PRIVATE);
        sp.edit().putString(getAdCacheAdIdsKey(appId,posId),adIdsStr).apply();
    }

    public static String getAdIdsStrFromCache (Context context, String appId, String posId) {
        SharedPreferences sp = context.getSharedPreferences(SHARE_PREFERENCES_KEY,Context.MODE_PRIVATE);
        return sp.getString(getAdCacheAdIdsKey(appId,posId),"");
    }

    public static boolean isContainSameReltype(AdProvider adProvider){
            for (AdProvider adProvider1 : AdCacheUtils.adProviderList) {
                if (adProvider1.relType == adProvider.relType) {
                    adProvider1.lastIds = adProvider.lastIds;
                    return true;
                }
            }
            return false;
    }


}
