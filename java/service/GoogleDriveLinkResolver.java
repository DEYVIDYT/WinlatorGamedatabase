package com.winlator.Download.service;

import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URLDecoder;

public class GoogleDriveLinkResolver {

    private static final String TAG = "GoogleDriveResolver";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";
    private static final String BASE_FILE_URL = "https://drive.usercontent.google.com/download?export=download&authuser=0&id=";

    private static final Pattern CONFIRM_TOKEN_PATTERN_1 = Pattern.compile("confirm=([0-9A-Za-z_-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONFIRM_TOKEN_PATTERN_2 = Pattern.compile("name=\"confirm\"\\s+value=\"([0-9A-Za-z_-]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern UUID_TOKEN_PATTERN = Pattern.compile("name=\"uuid\"\\s+value=\"([0-9A-Za-z_-]+)\"", Pattern.CASE_INSENSITIVE);

    static {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);
        Log.d(TAG, "Default CookieManager initialized.");
    }

    public DownloadItem resolveDriveUrl(String pageUrl) {
        Log.d(TAG, "Attempting to resolve Google Drive URL: " + pageUrl);
        String fileId = GoogleDriveUtils.extractDriveId(pageUrl);
        if (fileId == null) {
            Log.e(TAG, "Could not extract File ID from URL: " + pageUrl);
            return null;
        }
        Log.d(TAG, "Extracted File ID: " + fileId);
        String initialDownloadUrl = BASE_FILE_URL + fileId;
        return attemptDownload(initialDownloadUrl, fileId, 0);
    }

    private DownloadItem attemptDownload(String downloadUrl, String fileId, int attemptDepth) {
        Log.d(TAG, "Attempting download from URL: " + downloadUrl + " (Attempt depth: " + attemptDepth + ")");
        if (attemptDepth > 2) {
            Log.e(TAG, "Max download attempts reached for ID: " + fileId);
            return null;
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(downloadUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.connect();

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response Code: " + responseCode + " for " + downloadUrl);
            Log.d(TAG, "Response Message: " + connection.getResponseMessage());
            Log.d(TAG, "Final URL after redirects: " + connection.getURL().toString());

            String contentDisposition = connection.getHeaderField("Content-Disposition");
            if (contentDisposition != null && !contentDisposition.isEmpty()) {
                Log.i(TAG, "Direct download link found. Content-Disposition: " + contentDisposition);
                String filename = GoogleDriveUtils.sanitizeFilename(extractFilenameFromContentDisposition(contentDisposition));
                long fileSize = -1;
                String contentLengthHeader = connection.getHeaderField("Content-Length");
                if (contentLengthHeader != null) {
                    try {
                        fileSize = Long.parseLong(contentLengthHeader);
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Could not parse Content-Length: " + contentLengthHeader);
                    }
                }
                if (filename == null || filename.isEmpty()) {
                    Log.w(TAG, "Filename extracted as null or empty, using file ID as fallback name.");
                    filename = fileId;
                }
                Log.i(TAG, "Resolved GDrive File. Name: " + filename + ", URL: " + connection.getURL() + ", Size: " + fileSize);
                return new DownloadItem(filename, connection.getURL().toString(), fileSize, null);
            }

            String contentType = connection.getContentType();
            if (contentType == null || !contentType.toLowerCase().contains("text/html")) {
                Log.w(TAG, "Content-Type is not HTML (" + contentType + "). Cannot parse for confirmation. URL: " + downloadUrl);
                if (responseCode != HttpURLConnection.HTTP_OK && connection.getErrorStream() != null) {
                    String errorPageContent = readStreamToString(new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8)));
                    if (errorPageContent.contains("Google Drive - Quota exceeded") || errorPageContent.contains("downloadQuotaExceeded")) {
                        Log.e(TAG, "Google Drive Quota Exceeded for ID: " + fileId);
                    }
                }
                return null;
            }

            String pageContent;
            if (responseCode == HttpURLConnection.HTTP_OK) {
                pageContent = readStreamToString(new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)));
            } else if (connection.getErrorStream() != null) {
                pageContent = readStreamToString(new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8)));
            } else {
                Log.e(TAG, "No content to read for confirmation/error. URL: " + downloadUrl);
                return null;
            }
            connection.disconnect();

            if (pageContent.contains("Google Drive - Quota exceeded") || pageContent.contains("downloadQuotaExceeded")) {
                Log.e(TAG, "Quota exceeded.");
                return null;
            }
            if (pageContent.contains("Too many users have viewed or downloaded this file recently")) {
                Log.e(TAG, "Too many users error.");
                return null;
            }

            String confirmToken = null;
            Matcher confirmMatcher1 = CONFIRM_TOKEN_PATTERN_1.matcher(pageContent);
            if (confirmMatcher1.find()) {
                confirmToken = confirmMatcher1.group(1);
            } else {
                Matcher confirmMatcher2 = CONFIRM_TOKEN_PATTERN_2.matcher(pageContent);
                if (confirmMatcher2.find()) {
                    confirmToken = confirmMatcher2.group(1);
                }
            }

            String uuidToken = null;
            Matcher uuidMatcher = UUID_TOKEN_PATTERN.matcher(pageContent);
            if (uuidMatcher.find()) {
                uuidToken = uuidMatcher.group(1);
            }

            if (confirmToken != null) {
                String confirmationUrl = BASE_FILE_URL + fileId + "&confirm=" + confirmToken;
                if (uuidToken != null) {
                    confirmationUrl += "&uuid=" + uuidToken;
                }
                return attemptDownload(confirmationUrl, fileId, attemptDepth + 1);
            } else {
                Log.w(TAG, "No confirmation token found.");
                return null;
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception during Google Drive resolution", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    private String readStreamToString(BufferedReader reader) throws java.io.IOException {
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
        reader.close();
        return stringBuilder.toString();
    }

    private String extractFilenameFromContentDisposition(String contentDisposition) {
        if (contentDisposition == null) return null;
        Pattern pattern = Pattern.compile("filename\\*?=['\\\"]?(?:UTF-8''|UTF-8%27%27)?([^;\\\"']+)[\\\"']?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(contentDisposition);
        if (matcher.find()) {
            try {
                String filename = matcher.group(1);
                return URLDecoder.decode(filename, StandardCharsets.UTF_8.name());
            } catch (Exception e) {
                Log.w(TAG, "Failed to decode filename from Content-Disposition", e);
                return matcher.group(1);
            }
        }
        return null;
    }
}
