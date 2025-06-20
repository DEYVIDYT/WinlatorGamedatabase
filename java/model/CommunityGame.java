package com.winlator.Download.model;

public class CommunityGame {
    private String name;
    private double sizeInGB; // Changed from String to double
    private String url;

    public CommunityGame(String name, double sizeInGB, String url) {
        this.name = name;
        this.sizeInGB = sizeInGB;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getSizeInGB() { // Corrected return type and name
        return sizeInGB;
    }

    public void setSizeInGB(double sizeInGB) {
        this.sizeInGB = sizeInGB;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

