package com.winlator.Download.service;

import java.util.List;

public class GofileResolvedResult {
    private final List<DownloadItem> items;
    private final String authToken; // The guest token used for resolving and to be used for downloading

    public GofileResolvedResult(List<DownloadItem> items, String authToken) {
        this.items = items;
        this.authToken = authToken;
    }

    public List<DownloadItem> getItems() {
        return items;
    }

    public String getAuthToken() {
        return authToken;
    }

    public boolean hasItems() {
        return items != null && !items.isEmpty();
    }
}
