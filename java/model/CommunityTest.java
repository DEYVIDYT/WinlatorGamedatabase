package com.winlator.Download.model;

public class CommunityTest {
    private String gameName;
    private String youtubeUrl;
    private String description;

    public CommunityTest(String gameName, String youtubeUrl, String description) {
        this.gameName = gameName;
        this.youtubeUrl = youtubeUrl;
        this.description = description;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public String getYoutubeUrl() {
        return youtubeUrl;
    }

    public void setYoutubeUrl(String youtubeUrl) {
        this.youtubeUrl = youtubeUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
