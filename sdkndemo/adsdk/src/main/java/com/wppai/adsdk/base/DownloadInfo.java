package com.wppai.adsdk.base;

import android.os.Build;

import java.io.Serializable;

public class DownloadInfo implements Serializable {
    private String mDownloadLink;
    private String mTitle;
    private String mIconUrl;
    private String mFileName;

    public DownloadInfo(String downloadLink, String title, String iconUrl, String fileName) {
        this.mDownloadLink = downloadLink;
        this.mTitle = title;
        this.mIconUrl = iconUrl;
        this.mFileName = fileName;
    }

    public String getDownloadLink() {
        return mDownloadLink;
    }

    public void setDownloadLink(String downloadLink) {
        this.mDownloadLink = downloadLink;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public String getIconUrl() {
        return mIconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.mIconUrl = iconUrl;
    }

    public String getFileName() {
        return mFileName;
    }

    public void setFileName(String fileName) {
        this.mFileName = fileName;
    }

    public static class Builder {
        private String mLocalFilename;
        private String mIconUrl;
        private String mTitle;
        private String mDownloadLink;

        public Builder setLocalFilename(String localFilename) {
            mLocalFilename = localFilename;
            return this;
        }

        public Builder setDownloadLink(String downloadLink) {
            mDownloadLink = downloadLink;
            return this;
        }

        public Builder setDownloadTitle(String title) {
            mTitle = title;
            return this;
        }

        public Builder setDowloadIconUrl(String iconUrl) {
            mIconUrl = iconUrl;
            return this;
        }

        public DownloadInfo build() {
            return new DownloadInfo(mDownloadLink, mTitle, mIconUrl,mLocalFilename);
        }
    }
}
