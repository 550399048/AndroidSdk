package com.wppai.adsdk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.content.FileProvider;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.webkit.DownloadListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.wppai.adsdk.comm.NetworkUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class WebViewActivity extends Activity implements DownloadListener {

    private static String TAG = "WebViewActivity";

    private WebView mWebView;
    private String clickUrl;
    private static ArrayList<Long> mDownloadEnqueueIdList = new ArrayList<>();
    private static HashMap<String,Long> mDownloadEnqueueUrlMap = new HashMap<>();

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");

        setContentView(R.layout.sdk_activity_webview);
        RelativeLayout layout = findViewById(R.id.webView_contain);

        clickUrl = getIntent().getStringExtra("click_url");
        mWebView = new WebView(getApplicationContext());
        mWebView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        layout.addView(mWebView);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setBuiltInZoomControls(false);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        mWebView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mWebView.getSettings().setDefaultTextEncodingName("utf-8");
        mWebView.getSettings().setDomStorageEnabled(true);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
           mWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        }
        mWebView.getSettings().setTextZoom(100);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("http:") || url.startsWith("https:")) {
                    return false;
                }
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                boolean isInstall = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
                if (isInstall) {
                    startActivity(intent);
                    finish();
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {//页面加载完成
            }

        });
        mWebView.setDownloadListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWebView.loadUrl(clickUrl);
    }

    @Override
    protected void onDestroy() {
        if (mWebView !=  null) {
            ViewParent parent = mWebView.getParent();
            if (parent != null) {
                ((ViewGroup) parent).removeView(mWebView);
            }
            mWebView.stopLoading();
            // 3.0以上4.4以下系统，如果不隐藏WebView会出现崩溃
            mWebView.setVisibility(View.GONE);
            // 退出时调用此方法，移除绑定的服务，否则某些特定系统会报错
            mWebView.getSettings().setJavaScriptEnabled(false);
            mWebView.clearHistory();
            mWebView.loadUrl("about:blank");
            mWebView.removeAllViews();
            try {
                // NOTE: This can occasionally cause a segfault below API 17 (4.2)
                mWebView.destroy();
            } catch (Throwable tr) {
                Log.e(TAG, "mWebView.destroy(): exception occurred", tr);
            }

            mWebView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
        try {
            if (NetworkUtils.isWifiOnline(getApplicationContext())) {
                downLoadFile(getApplicationContext(), url, userAgent, mimeType);
            } else if (NetworkUtils.isMobileOnline(this)) {

                final String downloadUrl = url;
                final String downloadMimeType = mimeType;
                final String downloadUserAgent = userAgent;
                Resources res = this.getResources();
                new AlertDialog.Builder(this).setTitle(res.getString(R.string.sdk_mobile_network_download_tip))
                        .setCancelable(false)
                        .setPositiveButton(res.getString(R.string.sdk_dialog_bt_ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                downLoadFile(getApplicationContext(), downloadUrl, downloadUserAgent, downloadMimeType);
                            }
                        }).setNegativeButton(res.getString(R.string.sdk_dialog_bt_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "catch stop download model exception");
            e.printStackTrace();
        }
    }

    public void downLoadFile(Context context, String url, String userAgent, String mimeType) {
        Log.d(TAG, "downloadFile = " + url);
        if (mDownloadEnqueueUrlMap.containsKey(Uri.parse(url).getPath())) {
            int status = getDownloadStatus(mDownloadEnqueueUrlMap.get(Uri.parse(url).getPath()).longValue());
            Log.d(TAG + "download", "");
            if (status != DownloadManager.STATUS_FAILED && status != DownloadManager.STATUS_SUCCESSFUL) {
                Toast.makeText(context, R.string.sdk_download_running, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(context, R.string.sdk_download_pending, Toast.LENGTH_LONG).show();
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            Log.i(TAG, "onDownloadStart mimeType=" + mimeType);
            request.setMimeType(mimeType);
            request.addRequestHeader("User-Agent", userAgent);
            String fileType = url.substring(url.indexOf("."));
            request.setDestinationInExternalFilesDir(context, null, System.currentTimeMillis() + fileType);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            long enqueueId = downloadManager.enqueue(request);
            if (!mDownloadEnqueueIdList.contains(enqueueId)) {
                if (!mDownloadEnqueueUrlMap.containsKey(Uri.parse(url).getPath())) {
                    mDownloadEnqueueUrlMap.put(Uri.parse(url).getPath(), enqueueId);
                }
                mDownloadEnqueueIdList.add(enqueueId);
            }
            Log.i(TAG + "download", "startDownload EnqueueId=" + enqueueId);
        }
    }

    public static void openFile(Context context, Uri fileUri) {
        Log.i(TAG, "getPackageName="+context.getPackageName()+";fileUri.getPath()="+fileUri.getPath());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".wppai.fileProvider", new File(fileUri.getPath()));
            Log.i(TAG, "FileProvider.Uri="+uri);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(fileUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        context.startActivity(intent);
    }

    public int getDownloadStatus(long id) {
        DownloadManager downloadManager = (DownloadManager) this.getSystemService(Context.DOWNLOAD_SERVICE);

        DownloadManager.Query query = new DownloadManager.Query().setFilterById(id);

        int status = -1;

        try (Cursor cursor = downloadManager.query(query)) {
            if (cursor != null && cursor.moveToFirst()) {
                status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            }
        }catch (IllegalArgumentException e) {
            Log.w(TAG, e.getMessage(), e);
        }

        return status;
    }

    public static String getKey(HashMap<String, Long> map, Long value) {
        String key = null;
        //Map,HashMap并没有实现Iteratable接口.不能用于增强for循环.
        for (String getKey : map.keySet()) {
            if (map.get(getKey).equals(value)) {
                key = getKey;
            }
        }
        return key;
        //这个key肯定是最后一个满足该条件的key.
    }

    public static class DownloadReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == intent.getAction()) {

                DownloadManager.Query query = new DownloadManager.Query();
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                Log.i(TAG, "ACTION_DOWNLOAD_COMPLETE id="+id);
                if (mDownloadEnqueueIdList.contains(id)) {
                    mDownloadEnqueueIdList.remove(id);
                    if (mDownloadEnqueueUrlMap.containsValue(id)) {
                        mDownloadEnqueueUrlMap.remove(getKey(mDownloadEnqueueUrlMap,id));
                    }
                    query.setFilterById(id);
                    Cursor c = downloadManager.query(query);
                    if(c != null && c.moveToFirst()) {
                        String fileUri = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                        if(fileUri != null && fileUri.endsWith(".apk")){
                            Log.i(TAG, "fileUri="+fileUri);
                            openFile(context, Uri.parse(fileUri));
                        }
                    }
                    if (c != null) c.close();
                }
            }
        }
    }
}
