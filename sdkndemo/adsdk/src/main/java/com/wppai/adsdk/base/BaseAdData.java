package com.wppai.adsdk.base;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.StatusUtil;
import com.wppai.adsdk.R;
import com.wppai.adsdk.WebViewActivity;
import com.wppai.adsdk.comm.ConstantUtil;
import com.wppai.adsdk.comm.DownloadUtil;
import com.wppai.adsdk.comm.NetworkUtils;
import com.wppai.adsdk.comm.SystemUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class BaseAdData {
    public final static String TAG = "BaseAdData";
    private static final String APK_SUFFIX = ".apk";

    public String ad_id;
    public String impression_link;
    public String click_link;
    public String conversion_link;
    public int interact_type;
    public int crt_type;
    public String title;
    public String description;
    public String img_url;
    public String img2_url;
    public String impression_link_cc;
    public String click_link_cc;
    public String req_width;
    public String req_height;
    public String pos_width;
    public String pos_height;
    public String location_url;
    public int rel_type;
    public int open_type;
    public String package_name;

    private Context mContext;

    public BaseAdData(Context ctx) {
        mContext = ctx.getApplicationContext();
    }

    public void onExposured() {
        // exposure to gdt
        OkHttpClient client = new OkHttpClient();
        Request request1 = new Request.Builder().get().url(impression_link).build();
        Call call1 = client.newCall(request1);
        call1.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                try {
                    JSONObject obj = new JSONObject(response.body().string());

                    Log.i(TAG, "exposure response:" + obj.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        // exposure cc to server
        Request request2 = new Request.Builder().get().url(impression_link_cc).build();
        Call call2 = client.newCall(request2);
        call2.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                try {
                    JSONObject obj = new JSONObject(response.body().string());

                    Log.i(TAG, "cc exposure response:" + obj.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void onClicked(int down_x, int down_y, int up_x, int up_y) {
        if (!NetworkUtils.isOnline(mContext)) {
            DownloadUtil.get().showTip(mContext.getResources().getString(R.string.sdk_toast_network_offline),mContext);
            return;
        }

        OkHttpClient client = new OkHttpClient();

        if (down_x > Integer.parseInt(pos_width)) {
            down_x = down_x - (int) Math.random()*10;
        }

        if (up_x > Integer.parseInt(pos_width)) {
            up_x = up_x - (int) Math.random()*10;
        }

        if (down_y > Integer.parseInt(pos_height)) {
            down_y = down_y - (int) Math.random()*10;
        }

        if (up_y > Integer.parseInt(pos_width)) {
            up_y = up_y - (int) Math.random()*10;
        }

        String click_url = click_link.replace("__REQ_WIDTH__", req_width);
        click_url = click_url.replace("__REQ_HEIGHT__", req_height);
        click_url = click_url.replace("__WIDTH__", pos_width);
        click_url = click_url.replace("__HEIGHT__", pos_height);
        click_url = click_url.replace("__DOWN_X__", String.valueOf(down_x));
        click_url = click_url.replace("__DOWN_Y__", String.valueOf(down_y));
        click_url = click_url.replace("__UP_X__", String.valueOf(up_x));
        click_url = click_url.replace("__UP_Y__", String.valueOf(up_y));
        Log.i(TAG, "click url: " + click_url);
            if (interact_type == 0) {
                switch (rel_type) {
                    case ConstantUtil.TYPE_REL_GDT:
                        Intent intent = new Intent(mContext, WebViewActivity.class);
                        intent.putExtra("click_url", click_url);
                        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intent);
                        break;
                    case ConstantUtil.TYPE_REL_ZK:
                        Request request1 = new Request.Builder().get().url(click_url).build();
                        Call call1 = client.newCall(request1);
                        call1.enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                            }

                            @Override
                            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                                try {
                                    JSONObject obj = new JSONObject(response.body().string());

                                    Log.i(TAG, "click response zk:" + obj.toString());
                                    int ret = obj.optInt("ret");
                                    if (ret == 0) {
                                        if (open_type != 2) {
                                            Intent intent = new Intent(mContext, WebViewActivity.class);
                                            intent.putExtra("click_url", location_url);
                                            intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                                            mContext.startActivity(intent);
                                        } else {
                                            Uri uri = Uri.parse(location_url);
                                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                            intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                                            mContext.startActivity(intent);
                                        }
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        break;
                    case ConstantUtil.TYPE_REL_TT:
                    case ConstantUtil.TYPE_REL_WM:
                    default:
                        Intent intent1 = new Intent(mContext, WebViewActivity.class);
                        intent1.putExtra("click_url", location_url);
                        intent1.setFlags(FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intent1);
                        break;
                }

            } else if (interact_type == 1) {
                switch (rel_type) {
                    case ConstantUtil.TYPE_REL_GDT:
                    case ConstantUtil.TYPE_REL_ZK:
                        Request request1 = new Request.Builder().get().url(click_url).build();
                        Call call1 = client.newCall(request1);
                        call1.enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                            }

                            @Override
                            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                                try {
                                    JSONObject obj = new JSONObject(response.body().string());

                                    Log.i(TAG, "click response:" + obj.toString());
                                    int ret = obj.optInt("ret");
                                    if (ret == 0) {
                                        String clickId = obj.optJSONObject("data").optString("clickid");
                                        String dstLink = obj.optJSONObject("data").optString("dstlink");
                                        Log.i(TAG, "gdt click id: " + clickId);
                                        Log.i(TAG, "gdt dst link: " + dstLink);
                                        handleDstLink(dstLink);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        break;
                        case ConstantUtil.TYPE_REL_TT:
                        case ConstantUtil.TYPE_REL_WM:
                            handleDstLink(conversion_link);
                            break;
                }
            }


        // cc to server
        String click_cc_url = click_link_cc.replace("__REQ_WIDTH__", req_width);
        click_cc_url = click_cc_url.replace("__REQ_HEIGHT__", req_height);
        click_cc_url = click_cc_url.replace("__WIDTH__", pos_width);
        click_cc_url = click_cc_url.replace("__HEIGHT__", pos_height);
        click_cc_url = click_cc_url.replace("__DOWN_X__", String.valueOf(down_x));
        click_cc_url = click_cc_url.replace("__DOWN_Y__", String.valueOf(down_y));
        click_cc_url = click_cc_url.replace("__UP_X__", String.valueOf(up_x));
        click_cc_url = click_cc_url.replace("__UP_Y__", String.valueOf(up_y));
        Log.i(TAG, "click cc url: " + click_cc_url);

        Request request2 = new Request.Builder().get().url(click_cc_url).build();
        Call call2 = client.newCall(request2);
        call2.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                try {
                    JSONObject obj = new JSONObject(response.body().string());

                    Log.i(TAG, "cc click response:" + obj.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void handleDstLink(final String dstLink) {

        String fsnameStr = Uri.parse(dstLink).getQueryParameter("fsname");
        String pkgName = null;
        String fileName = null;
        String guessFileName = guessFileNameFromUrl(dstLink);;

        if (!TextUtils.isEmpty(package_name)) {
            pkgName = package_name;
            fileName = package_name + APK_SUFFIX;
        } else if (!TextUtils.isEmpty(fsnameStr)) {
            pkgName = fsnameStr.split("_")[0];
            fileName = fsnameStr;
        } else if (!TextUtils.isEmpty(guessFileName)){
            fileName = guessFileName;
        } else if (!TextUtils.isEmpty(pkgName)) {
            fileName = pkgName + APK_SUFFIX;
        }

        if (!fileName.endsWith(APK_SUFFIX)) {
            fileName = fileName + APK_SUFFIX;
        }

        Log.i(TAG, "fsname:" + pkgName+",img2_url="+img2_url + ",fileName = "+ fileName);

        DownloadInfo downloadInfo = new DownloadInfo.Builder()
                .setLocalFilename(fileName)
                .setDowloadIconUrl(img2_url)
                .setDownloadLink(dstLink)
                .setDownloadTitle(title)
                .build();
        if (!TextUtils.isEmpty(pkgName)) {
            if (SystemUtil.isInstalled(mContext, pkgName)) {
                PackageManager packageManager = mContext.getPackageManager();
                Intent intent = packageManager.getLaunchIntentForPackage(pkgName);
                if (intent != null) {
                    mContext.startActivity(intent);
                }
            } else {
                DownloadUtil.get().download(mContext, downloadInfo);
            }
        } else {
            DownloadUtil.get().download(mContext, downloadInfo);
        }
    }

    public static String guessFileNameFromUrl(String url) {
        String fileName = null;
        String decodedUrl = Uri.decode(url);
        if (decodedUrl != null) {
            int queryIndex = decodedUrl.indexOf('?');
            if (queryIndex > 0) {
                decodedUrl = decodedUrl.substring(0, queryIndex);
            }
            if (!decodedUrl.endsWith("/")) {
                int index = decodedUrl.lastIndexOf('/') + 1;
                if (index > 0) {
                    fileName = decodedUrl.substring(index);
                }
            }
        }
        return fileName;
    }



}
