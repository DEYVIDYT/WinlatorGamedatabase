package com.winlator.Download.model;

public class GameEntry {
    private long id;
    private String title;
    private String desktopFilePath;
    private String bannerImagePath; // Can be a local path or a URL

    public GameEntry(long id, String title, String desktopFilePath, String bannerImagePath) {
        this.id = id;
        this.title = title;
        this.desktopFilePath = desktopFilePath;
        this.bannerImagePath = bannerImagePath;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDesktopFilePath() {
        return desktopFilePath;
    }

    public void setDesktopFilePath(String desktopFilePath) {
        this.desktopFilePath = desktopFilePath;
    }

    public String getBannerImagePath() {
        return bannerImagePath;
    }

    public void setBannerImagePath(String bannerImagePath) {
        this.bannerImagePath = bannerImagePath;
    }
}
