package com.winlator.Download.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Release implements Parcelable {
    private String name;
    private String tagName;
    private String publishedAt;
    private String body;
    private String htmlUrl;
    private String downloadUrl;
    private String assetName;
    private long assetSize;

    public Release(String name, String tagName, String publishedAt, String body, String htmlUrl, String downloadUrl, String assetName, long assetSize) {
        this.name = name;
        this.tagName = tagName;
        this.publishedAt = publishedAt;
        this.body = body;
        this.htmlUrl = htmlUrl;
        this.downloadUrl = downloadUrl;
        this.assetName = assetName;
        this.assetSize = assetSize;
    }

    public String getName() {
        return name;
    }

    public String getTagName() {
        return tagName;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public String getBody() {
        return body;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getAssetName() {
        return assetName;
    }

    public long getAssetSize() {
        return assetSize;
    }

    // Método para formatar o tamanho do arquivo em KB, MB, etc.
    public String getFormattedSize() {
        if (assetSize < 1024) {
            return assetSize + " B";
        } else if (assetSize < 1024 * 1024) {
            return String.format("%.1f KB", assetSize / 1024.0);
        } else if (assetSize < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", assetSize / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", assetSize / (1024.0 * 1024 * 1024));
        }
    }

    // Parcelable implementation
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.tagName);
        dest.writeString(this.publishedAt);
        dest.writeString(this.body);
        dest.writeString(this.htmlUrl);
        dest.writeString(this.downloadUrl);
        dest.writeString(this.assetName);
        dest.writeLong(this.assetSize);
    }

    protected Release(Parcel in) {
        this.name = in.readString();
        this.tagName = in.readString();
        this.publishedAt = in.readString();
        this.body = in.readString();
        this.htmlUrl = in.readString();
        this.downloadUrl = in.readString();
        this.assetName = in.readString();
        this.assetSize = in.readLong();
    }

    public static final Parcelable.Creator<Release> CREATOR = new Parcelable.Creator<Release>() {
        @Override
        public Release createFromParcel(Parcel source) {
            return new Release(source);
        }

        @Override
        public Release[] newArray(int size) {
            return new Release[size];
        }
    };
}


