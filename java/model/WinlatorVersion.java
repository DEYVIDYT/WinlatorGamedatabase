package com.winlator.Download.model;

import java.io.Serializable;

public class WinlatorVersion implements Serializable {
    private String name;
    private String assetName;
    private String downloadUrl;
    private long assetSize;
    private String publishedAt;

    public WinlatorVersion(String name, String assetName, String downloadUrl) {
        this.name = name;
        this.assetName = assetName;
        this.downloadUrl = downloadUrl;
        this.assetSize = 0;
        this.publishedAt = "";
    }

    public WinlatorVersion(String name, String assetName, String downloadUrl, long assetSize, String publishedAt) {
        this.name = name;
        this.assetName = assetName;
        this.downloadUrl = downloadUrl;
        this.assetSize = assetSize;
        this.publishedAt = publishedAt;
    }

    public String getName() {
        return name;
    }

    public String getAssetName() {
        return assetName;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public long getAssetSize() {
        return assetSize;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public void setAssetSize(long assetSize) {
        this.assetSize = assetSize;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }
}

