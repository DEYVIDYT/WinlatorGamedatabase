package com.winlator.Download.model;

public class CommunityFix {
    private String fixName;
    private String description;
    private String downloadUrl;

    public CommunityFix(String fixName, String description, String downloadUrl) {
        this.fixName = fixName;
        this.description = description;
        this.downloadUrl = downloadUrl;
    }

    public String getFixName() {
        return fixName;
    }

    public void setFixName(String fixName) {
        this.fixName = fixName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}
