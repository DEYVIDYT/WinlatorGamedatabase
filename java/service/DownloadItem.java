package com.winlator.Download.service;

public class DownloadItem {
    public final String fileName;
    public final String directUrl;
    public final long size;
    public final String gofileContentId; // Original Gofile content ID for reference

    public DownloadItem(String fileName, String directUrl, long size, String gofileContentId) {
        this.fileName = fileName;
        this.directUrl = directUrl;
        this.size = size;
        this.gofileContentId = gofileContentId;
    }

    @Override
    public String toString() {
        return "DownloadItem{" +
               "fileName='" + fileName + '\'' +
               ", directUrl='" + directUrl + '\'' +
               ", size=" + size +
               ", gofileContentId='" + gofileContentId + '\'' +
               '}';
    }
}
