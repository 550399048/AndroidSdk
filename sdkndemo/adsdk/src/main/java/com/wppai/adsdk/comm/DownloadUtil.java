package com.wppai.adsdk.comm;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.liulishuo.okdownload.StatusUtil;
import com.wppai.adsdk.AdDialogActivity;
import com.wppai.adsdk.R;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.SpeedCalculator;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.listener.DownloadListener4WithSpeed;
import com.liulishuo.okdownload.core.listener.assist.Listener4SpeedAssistExtend;
import com.wppai.adsdk.base.DownloadInfo;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DownloadUtil {
    public final static String TAG = "DownloadUtil";
    private static Boolean DEBUG = true;

    private static DownloadUtil downloadUtil;

    public static DownloadUtil get() {
        if (downloadUtil == null) {
            downloadUtil = new DownloadUtil();
        }
        return downloadUtil;
    }

    private DownloadUtil() {
    }

    private Context mContext;

    private DownloadTask mTask;

    private NotificationDownloadListener listener;

    public void download(Context context, DownloadInfo downloadInfo) {
        mContext = context;
        if (Utils.containUrl(downloadInfo.getFileName())) {
            if (DEBUG) {
                Log.d(TAG, "fileNamelist contain " + downloadInfo.getFileName());
            }
            showTip(context.getString(R.string.sdk_downloading_title) + downloadInfo.getTitle());
            return;
        }

        if (mTask != null && downloadInfo.getFileName().equals(formatFileNameApk(mTask))) {
            if (installApk(mTask,mContext)) {
                return;
            }
        }

        if (NetworkUtils.isMobileOnline(context)) {
            Intent intent = new Intent();
            intent.setClass(context,AdDialogActivity.class);
            intent.putExtra("download_info", downloadInfo);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK );
            context.startActivity(intent);
            return;
        }

        if (NetworkUtils.isWifiOnline(context)) {
            download(context, downloadInfo.getDownloadLink(), downloadInfo.getTitle(), downloadInfo.getIconUrl(), downloadInfo.getFileName());
        }
    }


    public String formatFileNameApk(DownloadTask task) {
        String taskFileName = task.getFilename();
        if (taskFileName == null) {
            return ".apk";
        }
        if (!taskFileName.endsWith(".apk")) {
            taskFileName = taskFileName + ".apk";
        }
        return taskFileName;
    }

    public void download(final Context ctx, final String url, final String title, final String imgUrl, String fileName) {
        if ((mTask != null && !fileName.equals(formatFileNameApk(mTask))) || mTask == null) {
            if (DEBUG) {
                Log.d(TAG, "creat new task for " + fileName);
            }
            mTask = new DownloadTask.Builder(url, getParentFile(ctx))
                    .setFilename(fileName)
                    .setFilenameFromResponse(true)
                    // the minimal interval millisecond for callback progress
                    .setMinIntervalMillisCallbackProcess(100)
                    // ignore the same task has already completed in the past.
                    .setPassIfAlreadyCompleted(true)
                    .build();
            listener = new NotificationDownloadListener(ctx);
            listener.initNotification(title, imgUrl, fileName);

            mTask.addTag(mTask.getId(), url);
            mTask.enqueue(listener);
            Utils.save(fileName);
        }
    }

    public boolean installApk(DownloadTask task,Context context) {
        if (task.getFile() == null) {
            if (DEBUG) {
                Log.d(TAG, "task File for null !");
            }
            return false;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String pkg = getPackageFomFile(mContext, task.getFile().getPath());
        if (DEBUG) {
            Log.i(TAG, "installApk file = " + task.getFile());
        }
        if (pkg != null) {
            if (DEBUG) {
                Log.d(TAG, "installApk = " + pkg);
            }
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                intent.setDataAndType(FileProvider.getUriForFile(context, context.getPackageName() + ".wppai.fileProvider", task.getFile()), "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                Uri uri = Uri.fromFile(task.getFile());
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }

            if (context.getPackageManager().queryIntentActivities(intent, 0).size() > 0) {
                mContext.startActivity(intent);
                Utils.remove(task.getFilename());
                mTask.cancel();
                return true;
            }
        }
        return false;
    }

    public void cancelDownload() {
        if (mTask != null) {
            mTask.cancel();
        }
    }

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    String content = (String) msg.obj;
                    Toast.makeText(mContext, content, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public void showTip(final String content, Context context) {
        mContext = context;
        Message message = new Message();
        message.what = 1;
        message.obj = content;
        handler.sendMessage(message);
    }

    public void showTip(final String content) {
        Message message = new Message();
        message.what = 1;
        message.obj = content;
        handler.sendMessage(message);
    }

    public String getPackageFomFile(Context context, String fileName) {
        PackageManager pm = context.getPackageManager();
        PackageInfo packageInfo = pm.getPackageArchiveInfo(fileName, 0);
        if (packageInfo != null) {
            return packageInfo.packageName;
        }
        return null;
    }

    private File getParentFile(Context context) {
        final File externalSaveDir = context.getExternalCacheDir();
        if (externalSaveDir == null) {
            return context.getCacheDir();
        } else {
            return externalSaveDir;
        }
    }

    public static String bytes2kb(long bytes) {
        BigDecimal filesize = new BigDecimal(bytes);
        BigDecimal megabyte = new BigDecimal(1024 * 1024);
        float returnValue = filesize.divide(megabyte, 2, BigDecimal.ROUND_UP)
                .floatValue();
        if (returnValue > 1) {
            return returnValue + "M";
        }
        BigDecimal kilobyte = new BigDecimal(1024);
        returnValue = filesize.divide(kilobyte, 2, BigDecimal.ROUND_UP)
                .floatValue();
        return (returnValue + "K");
    }

    public class NotificationDownloadListener extends DownloadListener4WithSpeed {
        private int totalLength;

        private NotificationCompat.Builder builder;
        public NotificationManager mNotificationManager;
        // private Runnable taskEndRunnable;
        private Context context;

        private NotificationCompat.Action action;
        private String mApkName;
        private String mTitle;
        private String mFileName;

        public NotificationDownloadListener(Context context) {
            this.context = context.getApplicationContext();
        }

        /*public void attachTaskEndRunnable(Runnable taskEndRunnable) {
            this.taskEndRunnable = taskEndRunnable;
        }

        public void releaseTaskEndRunnable() {
            taskEndRunnable = null;
        }*/

        public void setAction(NotificationCompat.Action action) {
            this.action = action;
        }

        public void initNotification( String title, String imgUrl,String fileName) {
            mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mTitle = title;
            mFileName = fileName;
            final String channelId = "okdownload";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                final NotificationChannel channel = new NotificationChannel(
                        channelId,
                        "下载",
                        NotificationManager.IMPORTANCE_HIGH);
                channel.enableLights(true); //是否在桌面icon右上角展示小红点
                channel.setLightColor(Color.GREEN); //小红点颜色
                channel.setShowBadge(true); //是否在久按桌面图标时显示此渠道的通知
                mNotificationManager.createNotificationChannel(channel);
            }

            builder = new NotificationCompat.Builder(context);

            String titleStr = mContext.getString(R.string.sdk_downloading_title);
            if (imgUrl != null) {
                new DownloadImageTask(builder).execute(imgUrl);
            }

            builder.setDefaults(Notification.DEFAULT_VIBRATE)
                    .setOnlyAlertOnce(true)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentTitle(titleStr + title)
                    .setContentText("Download a task showing on notification")
                    .setSmallIcon(R.drawable.sdk_status_download);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId(channelId);
            }

            if (action != null) {
                builder.addAction(action);
            }
        }

        @Override
        public void taskStart(@NonNull DownloadTask task) {
            Log.i(TAG, "taskStart");
            builder.setTicker("taskStart");
            builder.setOngoing(true);
            builder.setAutoCancel(false);
            builder.setContentText(context.getString(R.string.sdk_download_start_content));
            builder.setProgress(0, 0, true);
            mNotificationManager.notify(task.getId(), builder.build());

        }

        @Override
        public void connectStart(@NonNull DownloadTask task, int blockIndex,
                                 @NonNull Map<String, List<String>> requestHeaderFields) {
            builder.setTicker("connectStart");
            builder.setProgress(0, 0, true);
            mNotificationManager.notify(task.getId(), builder.build());
        }

        @Override
        public void connectEnd(@NonNull DownloadTask task, int blockIndex, int responseCode,
                               @NonNull Map<String, List<String>> responseHeaderFields) {
            builder.setTicker("connectStart");
            builder.setProgress(0, 0, true);
            mNotificationManager.notify(task.getId(), builder.build());
        }

        @Override
        public void infoReady(@NonNull DownloadTask task, @NonNull BreakpointInfo info,
                              boolean fromBreakpoint,
                              @NonNull Listener4SpeedAssistExtend.Listener4SpeedModel model) {
            Log.i(TAG, "infoReady " + info + " " + fromBreakpoint + " " + model.getTaskSpeed());

            if (fromBreakpoint) {
                builder.setTicker("fromBreakpoint");
            } else {
                builder.setTicker("fromBeginning");
            }
            builder.setContentText(
                    "This task is download fromBreakpoint[" + fromBreakpoint + "]");
            builder.setProgress((int) info.getTotalLength(), (int) info.getTotalOffset(), true);
            mNotificationManager.notify(task.getId(), builder.build());

            totalLength = (int) info.getTotalLength();
            showTip(mContext.getString(R.string.sdk_start_download_title) + mTitle);

        }

        @Override
        public void progressBlock(@NonNull DownloadTask task, int blockIndex,
                                  long currentBlockOffset,
                                  @NonNull SpeedCalculator blockSpeed) {
            //Log.i(TAG,"progressBlock task = "+task.getInfo().toString()+"\n"+blockIndex+"\n"+currentBlockOffset+"\n"+blockSpeed.speed());
        }

        @SuppressLint("StringFormatMatches")
        @Override
        public void progress(@NonNull DownloadTask task, long currentOffset,
                             @NonNull SpeedCalculator taskSpeed) {
            Log.i(TAG, "progress " + currentOffset);

            builder.setContentText(String.format(context.getString(R.string.sdk_downloading_content), bytes2kb(currentOffset), bytes2kb(totalLength)));
            builder.setProgress(totalLength, (int) currentOffset, false);
            mNotificationManager.notify(task.getId(), builder.build());
        }

        @Override
        public void blockEnd(@NonNull DownloadTask task, int blockIndex, BlockInfo info,
                             @NonNull SpeedCalculator blockSpeed) {
            //Log.d(TAG,"blockEnd task="+task.getInfo().toString()+"\n"+blockIndex+"\n"+info.toString()+"\n"+blockSpeed);
        }

        @Override
        public void taskEnd(@NonNull final DownloadTask task, @NonNull EndCause cause,
                            @android.support.annotation.Nullable Exception realCause,
                            @NonNull SpeedCalculator taskSpeed) {
            Log.i(TAG, "taskEnd " + cause + " " + realCause + ",taskSpeed=" + taskSpeed.averageSpeed());

            builder.setOngoing(false);
            builder.setAutoCancel(true);

            builder.setTicker("taskEnd " + cause);
            switch (cause) {
                case COMPLETED:
                    builder.setProgress(1, 1, false);
                    builder.setContentText(context.getString(R.string.sdk_download_complete_content));
                    mNotificationManager.notify(task.getId(), builder.build());

                    Intent intent = new Intent(Intent.ACTION_VIEW);

                    String pkgName = getPackageFomFile(context, task.getFile().getPath());

                    if (pkgName != null) {
                        Utils.remove(task.getFilename());
                        Log.d(TAG, "install pkgName = " + pkgName);

                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                            intent.setDataAndType(FileProvider.getUriForFile(context, context.getPackageName()+".wppai.fileProvider", task.getFile()), "application/vnd.android.package-archive");
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } else {
                            Uri uri = Uri.fromFile(task.getFile());
                            intent.setDataAndType(uri, "application/vnd.android.package-archive");
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        }

                        if (context.getPackageManager().queryIntentActivities(intent, 0).size() > 0) {
                            mContext.startActivity(intent);
                            mTask.cancel();
                        }
                    }
                    if (DEBUG) {
                        Log.i(TAG, "file = " + task.getFile());
                        Log.i(TAG, "taskEnd, done " + StatusUtil.isCompleted(task));
                    }
                    mNotificationManager.cancelAll();
                    break;
                case SAME_TASK_BUSY:
                    Log.i(TAG, "taskEnd SAME_TASK_BUSY ");
                    showTip(context.getString(R.string.sdk_downloading_title) + mTitle);
                    break;
                case CANCELED:
                    Log.i(TAG, "taskEnd, canceled");
                    mNotificationManager.cancelAll();
                    if (task != null) {
                        task.cancel();
                    }
                    break;
                case PRE_ALLOCATE_FAILED:
                    Log.i(TAG, "taskEnd PRE_ALLOCATE_FAILED");
                    mNotificationManager.cancelAll();
                    if (task != null) {
                        task.cancel();
                    }
                    break;
                case FILE_BUSY:
                    Log.i(TAG, "taskEnd FILE_BUSY");
                    mNotificationManager.cancelAll();
                    if (task != null) {
                        task.cancel();
                    }
                    break;
                case ERROR:
                    Utils.remove(mFileName);
                    removeDownloadFailedApk(task);
                    showTip(context.getString(R.string.sdk_downloading_failed));
                    Log.i(TAG, "taskEnd error mFileName = "+mFileName);
                    mNotificationManager.cancelAll();
                    if (task != null) {
                        task.cancel();
                    }
                    break;
            }
        }

        public void removeDownloadFailedApk(DownloadTask task) {
            if (task.getParentFile() != null && task.getFilename() != null) {
                File apkFile = new File(task.getParentFile(), task.getFilename());
                if (apkFile.exists()) {
                    Log.d(TAG, "delete download failed apk = " + task.getFilename() + ", parent = " + task.getParentFile());
                    apkFile.delete();
                    mTask = null;
                }
            }
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        NotificationCompat.Builder builder;

        public DownloadImageTask(NotificationCompat.Builder builder) {
            this.builder = builder;
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
            builder.setLargeIcon(result);
        }
    }

    public static class Utils {
        private static ArrayList<String> urlForDownloadList = new ArrayList();

        public static void save(String fileName) {
            if (!urlForDownloadList.contains(fileName)) {
                urlForDownloadList.add(fileName);
                Log.d(TAG, "save download filename = " + fileName);
            }

        }

        public static void remove(String fileName) {
            Log.d(TAG,"remove fileName = "+ fileName);
            if (!fileName.endsWith(".apk")) {
                fileName = fileName + ".apk";
            }
            if (urlForDownloadList.contains(fileName)) {
                urlForDownloadList.remove(fileName);
                Log.d(TAG, "delete download fileName = " + fileName);
            }
        }

        public static boolean containUrl(String fileName) {
            return urlForDownloadList.contains(fileName);
        }
    }
}
