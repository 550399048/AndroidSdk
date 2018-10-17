package com.wppai.adsdk;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;

import com.wppai.adsdk.base.DownloadInfo;
import com.wppai.adsdk.comm.DownloadUtil;

public class AdDialogActivity extends Activity implements DialogInterface.OnKeyListener, DialogInterface.OnDismissListener {
    private AlertDialog.Builder builder;
    private DownloadInfo downloadInfo;
    private AlertDialog alert;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        downloadInfo = (DownloadInfo) intent.getSerializableExtra("download_info");
        showDialog(downloadInfo);
    }

    private void showDialog(final DownloadInfo downloadInfo) {
        if (builder == null) {
            builder =
                    new AlertDialog.Builder(this);
            builder.setTitle(R.string.sdk_dialog_4g_title);
            builder.setMessage(R.string.sdk_dialog_4g_content);
            builder.setPositiveButton(R.string.sdk_dialog_bt_ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            DownloadUtil.get().download(getApplicationContext(), downloadInfo.getDownloadLink(), downloadInfo.getTitle(), downloadInfo.getIconUrl(), downloadInfo.getFileName());
                            finish();
                        }
                    });
            builder.setNegativeButton(R.string.sdk_dialog_bt_cancel,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            finish();
                        }
                    });
        }
        if (alert == null) {
            alert = builder.create();
            alert.setOnKeyListener(this);
        }
        alert.setCancelable(true);
        alert.setCanceledOnTouchOutside(true);
        alert.setOnDismissListener(this);
        alert.show();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissDialog();
    }

    private void dismissDialog() {
        if (alert != null) {
            alert.dismiss();
            alert = null;
            AdDialogActivity.this.finish();
        }
    }

    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            Log.d(AdDialogActivity.class.getSimpleName(),"event=="+keyCode);
            alert.dismiss();
            AdDialogActivity.this.finish();
        }
        return false;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        dialog.dismiss();
        finish();
    }
}
