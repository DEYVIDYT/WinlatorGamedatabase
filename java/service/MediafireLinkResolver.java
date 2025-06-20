package com.winlator.Download.service;

import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URLDecoder; // For decoding filename

public class MediafireLinkResolver {

    private static final String TAG = "MediafireLinkResolver";
    // User-Agent from the Python script
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.5481.178 Safari/537.36";
    // Regex to find the download link in HTML
    private static final Pattern DOWNLOAD_LINK_PATTERN = Pattern.compile("href=\"((http|https)://download[^\"]+)\"");
    // Regex to extract filename from Content-Disposition header
    // Handles filename="filename.ext" and filename*=UTF-8''filename.ext
    private static final Pattern FILENAME_PATTERN = Pattern.compile("filename\\*?=['\"]?(?:UTF-8''|MarkDialogue)?([^'\"]+)['\"]?", Pattern.CASE_INSENSITIVE);


    public DownloadItem resolveMediafireUrl(String pageUrl) {
        Log.d(TAG, "Attempting to resolve MediaFire URL: " + pageUrl);
        HttpURLConnection connection = null;
        String currentUrl = pageUrl;
        int maxRedirects = 5; // Prevent infinite loops

        try {
            for (int redirectCount = 0; redirectCount < maxRedirects; redirectCount++) {
                Log.d(TAG, "Fetching URL: " + currentUrl + " (Attempt " + (redirectCount + 1) + ")");
                URL url = new URL(currentUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", USER_AGENT);
                connection.setInstanceFollowRedirects(true); // Default, but good to be explicit for clarity
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.connect();

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode + " for " + currentUrl);

                String contentDisposition = connection.getHeaderField("Content-Disposition");
                if (contentDisposition != null && !contentDisposition.isEmpty()) {
                    Log.d(TAG, "Found Content-Disposition: " + contentDisposition);
                    String fileName = extractFileName(contentDisposition);
                    long fileSize = -1;
                    String contentLengthHeader = connection.getHeaderField("Content-Length");
                    if (contentLengthHeader != null) {
                        try {
                            fileSize = Long.parseLong(contentLengthHeader);
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Could not parse Content-Length: " + contentLengthHeader);
                        }
                    }
                    if (fileName != null) {
                        // The URL that gave Content-Disposition is the direct download link
                        String directDownloadUrl = connection.getURL().toString(); // This gets the final URL after redirects
                        Log.i(TAG, "MediaFire link resolved. Filename: " + fileName + ", URL: " + directDownloadUrl + ", Size: " + fileSize);
                        return new DownloadItem(fileName, directDownloadUrl, fileSize, null); // GofileContentId is null
                    } else {
                        Log.w(TAG, "Content-Disposition found, but could not extract filename.");
                        // Fall through to try parsing HTML if filename extraction failed but link might still be there
                    }
                }

                // If no Content-Disposition, or if filename extraction failed from it, try parsing HTML body
                // (but only if it's likely an HTML page)
                String contentType = connection.getContentType();
                if (contentType == null || !contentType.toLowerCase().contains("text/html")) {
                    Log.w(TAG, "Not an HTML page or Content-Disposition not found, stopping. Content-Type: " + contentType);
                    if (responseCode == HttpURLConnection.HTTP_OK) { // If it's OK but not what we want, it's a dead end for this logic
                         Log.e(TAG, "Received OK but not the file content or a page with a link. URL: " + currentUrl);
                    }
                    return null; // Cannot proceed
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder htmlContent = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    htmlContent.append(line);
                }
                reader.close();
                connection.disconnect(); // Disconnect before next loop iteration or return

                Matcher matcher = DOWNLOAD_LINK_PATTERN.matcher(htmlContent.toString());
                if (matcher.find()) {
                    currentUrl = matcher.group(1); // Get the first captured group (the URL)
                    Log.d(TAG, "Found potential download link in HTML: " + currentUrl);
                    // Loop to fetch this new URL
                } else {
                    Log.w(TAG, "No download link pattern found in HTML for: " + pageUrl);
                    return null; // No further links to follow
                }
            }
            Log.e(TAG, "Max redirects reached, failing resolution for: " + pageUrl);

        } catch (Exception e) {
            Log.e(TAG, "Error resolving MediaFire URL: " + pageUrl, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null; // Resolution failed
    }

    private String extractFileName(String contentDisposition) {
        if (contentDisposition == null) {
            return null;
        }
        Log.d(TAG, "Extracting filename from Content-Disposition: " + contentDisposition);
        Matcher matcher = FILENAME_PATTERN.matcher(contentDisposition);
        if (matcher.find()) {
            String fileName = matcher.group(1);
            // Try to decode URL-encoded filename (common in filename*=UTF-8''...)
            try {
                // Filename might be URL encoded (e.g. %20 for space)
                // Standard URL decoding should handle this.
                // If it's from filename*=UTF-8''..., it's often already in a readable form or needs specific RFC 5987 decoding.
                // For simplicity, standard URLDecoder.decode is a good first step.
                fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8.name());
            } catch (Exception e) {
                Log.w(TAG, "Failed to URL decode filename: " + fileName, e);
                // Use the raw filename if decoding fails
            }
             Log.d(TAG, "Extracted filename: " + fileName);
            return fileName;
        }
        Log.w(TAG, "Filename pattern not found in Content-Disposition.");
        return null;
    }
}
