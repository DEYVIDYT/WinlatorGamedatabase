package com.winlator.Download.service;

import android.util.Log;

import com.winlator.Download.service.DownloadItem; // Changed import

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PixeldrainLinkResolver {

    private static final String TAG = "PixeldrainLinkResolver";
    private static final String API_DOWNLOAD_URL_BASE = "https://pixeldrain.com/api/file/";

    // Pattern to extract file ID from URLs like:
    // https://pixeldrain.com/u/FILE_ID
    // https://pixeldrain.com/l/FILE_ID (though list/album might need different handling for multiple files)
    // For now, focusing on single file '/u/' links primarily.
    private static final Pattern PIXELDRAIN_URL_PATTERN = Pattern.compile("pixeldrain\\.com/(?:u|l)/([a-zA-Z0-9]+)");

    public DownloadItem resolvePixeldrainUrl(String pageUrl) {
        if (pageUrl == null || pageUrl.trim().isEmpty()) {
            Log.e(TAG, "Page URL is null or empty.");
            return null;
        }

        Matcher matcher = PIXELDRAIN_URL_PATTERN.matcher(pageUrl);
        if (!matcher.find()) {
            Log.e(TAG, "Could not extract FILE_ID from URL: " + pageUrl);
            return null;
        }

        String fileId = matcher.group(1);
        if (fileId == null || fileId.trim().isEmpty()) {
            Log.e(TAG, "Extracted FILE_ID is null or empty from URL: " + pageUrl);
            return null;
        }

        String directDownloadUrl = API_DOWNLOAD_URL_BASE + fileId;
        HttpURLConnection connection = null;
        String fileName = fileId; // Default filename if not found in header
        long fileSize = -1;

        try {
            URL url = new URL(directDownloadUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setInstanceFollowRedirects(false); // Pixeldrain API seems to redirect for actual download
            connection.setConnectTimeout(10000); // 10 seconds
            connection.setReadTimeout(10000);    // 10 seconds

            // Log request headers (optional, for debugging)
            // Log.d(TAG, "Requesting HEAD for: " + directDownloadUrl);

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response Code for HEAD request: " + responseCode);


            // Pixeldrain's /api/file/{id} for HEAD might directly give 200 OK with info,
            // or it might give a redirect (301/302/307) to the actual storage location.
            // If it's a redirect, the headers we want (Content-Disposition, Content-Length)
            // might be on the redirect response OR on the target of the redirect.
            // For simplicity with HttpURLConnection and HEAD, we'll check current response.
            // If it's a redirect and headers are missing, a GET request (and then aborting)
            // might be needed, but let's try with HEAD first.

            if (responseCode == HttpURLConnection.HTTP_OK ||
                (responseCode >= HttpURLConnection.HTTP_MOVED_PERM && responseCode <= HttpURLConnection.HTTP_SEE_OTHER) || // 301, 302, 303
                 responseCode == 307 || responseCode == 308) { // HTTP_TEMP_REDIRECT , HTTP_PERM_REDIRECT

                String contentDisposition = connection.getHeaderField("Content-Disposition");
                Log.d(TAG, "Content-Disposition: " + contentDisposition);
                if (contentDisposition != null) {
                    Pattern fileNamePattern = Pattern.compile("filename\\s*=\\s*\"?([^\"]+)\"?");
                    Matcher fileNameMatcher = fileNamePattern.matcher(contentDisposition);
                    if (fileNameMatcher.find()) {
                        String extractedName = fileNameMatcher.group(1);
                        if (extractedName != null && !extractedName.trim().isEmpty()) {
                            fileName = extractedName.trim();
                            Log.i(TAG, "Extracted filename: " + fileName);
                        }
                    }
                } else {
                    // If no Content-Disposition, try to get name from the URL path if it's a redirect
                    if (responseCode != HttpURLConnection.HTTP_OK && connection.getHeaderField("Location") != null) {
                        String locationUrl = connection.getHeaderField("Location");
                        try {
                            URL actualFileUrl = new URL(locationUrl);
                            String path = actualFileUrl.getPath();
                            if (path != null && path.contains("/")) {
                                String potentialName = path.substring(path.lastIndexOf('/') + 1);
                                if (!potentialName.isEmpty()) {
                                    fileName = potentialName;
                                     Log.i(TAG, "Extracted filename from redirect location path: " + fileName);
                                }
                            }
                        } catch (MalformedURLException e) {
                            Log.w(TAG, "Redirect location URL is malformed: " + locationUrl);
                        }
                    }
                }


                String contentLengthStr = connection.getHeaderField("Content-Length");
                if (contentLengthStr != null) {
                    try {
                        fileSize = Long.parseLong(contentLengthStr);
                        Log.i(TAG, "File size: " + fileSize);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Failed to parse Content-Length: " + contentLengthStr, e);
                    }
                }

                // If it was a redirect and we want to use the original API URL for the DownloadItem
                // (as DownloadService might handle redirects better with GET)
                // or if we want the final location. For now, let's stick to the directDownloadUrl (API URL).
                // The DownloadService's DownloadTask should handle redirects during GET.

                return new DownloadItem(fileName, directDownloadUrl, fileSize, null);

            } else {
                Log.e(TAG, "Failed to get file info. HTTP Response Code: " + responseCode + " for URL: " + directDownloadUrl);
                return null;
            }

        } catch (MalformedURLException e) {
            Log.e(TAG, "Malformed URL: " + directDownloadUrl, e);
            return null;
        } catch (IOException e) {
            Log.e(TAG, "IOException while connecting to " + directDownloadUrl, e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
