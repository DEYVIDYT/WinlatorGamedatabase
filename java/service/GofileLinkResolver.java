package com.winlator.Download.service;

import android.content.Context;
import android.util.Log;

import com.winlator.Download.utils.AppSettings; // Import AppSettings
// Assuming DownloadItem is in this package based on previous subtask.
// import com.winlator.Download.model.DownloadItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder; // For password encoding
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GofileLinkResolver {

    private static final String TAG = "GofileLinkResolver";
    private final Context context;

    public GofileLinkResolver(Context context) {
        this.context = context.getApplicationContext();
    }

    private static String sha256(final String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            Log.e(TAG, "SHA-256 hashing failed", e);
            return null; // Or throw a runtime exception
        }
    }

    private String extractContentId(String gofilePageUrl) {
        if (gofilePageUrl == null) return null;
        // Updated pattern to be more inclusive of potential Gofile URL variations
        // e.g., gofile.io/d/contentId, gofile.io/download/contentId, gofile.io/w/contentId, gofile.io/edit/contentId
        Pattern pattern = Pattern.compile("gofile\\.io/(?:d|download|w|edit)/([a-zA-Z0-9]+(?:-[a-zA-Z0-9]+)*)");
        Matcher matcher = pattern.matcher(gofilePageUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        Log.w(TAG, "Could not extract content ID from URL: " + gofilePageUrl);
        return null;
    }

    public GofileResolvedResult resolveGofileUrl(String gofilePageUrl, String password) {
        Log.i(TAG, "Attempting to resolve Gofile URL: " + gofilePageUrl + (password != null ? " with password" : ""));
        List<DownloadItem> downloadItems = new ArrayList<>();

        String contentId = extractContentId(gofilePageUrl);
        if (contentId == null) {
            Log.e(TAG, "Failed to extract content ID from URL: " + gofilePageUrl);
            return new GofileResolvedResult(downloadItems, null); // Return empty result, no token to return yet
        }

        String accountToken = AppSettings.getGofileAccountToken(context);
        if (accountToken == null || accountToken.isEmpty()) {
            Log.e(TAG, "Failed to obtain Gofile account token. Cannot proceed with API call.");
            return new GofileResolvedResult(downloadItems, null); // No token, can't make authenticated call
        }
        Log.d(TAG, "Using Gofile account token for API call.");

        String dynamicWt = AppSettings.getDynamicGofileWt(context);
        if (dynamicWt == null || dynamicWt.isEmpty()) {
            Log.e(TAG, "Failed to obtain dynamic Gofile WT. Cannot proceed.");
            return new GofileResolvedResult(new ArrayList<>(), accountToken); // Return empty result, but with accountToken
        }
        Log.d(TAG, "Using dynamic Gofile WT: " + dynamicWt);

        String hashedPassword = null;
        if (password != null && !password.isEmpty()) {
            hashedPassword = sha256(password);
            if (hashedPassword == null) {
                Log.e(TAG, "Password hashing failed. Proceeding without password.");
            } else {
                 Log.d(TAG, "Password provided and hashed.");
            }
        }

        // Construct API URL using dynamicWt and removing sort parameters
        String apiUrl = "https://api.gofile.io/contents/" + contentId + "?wt=" + dynamicWt + "&cache=true";

        if (hashedPassword != null) {
            try {
                // URL encode the hashed password to be safe for query parameter
                apiUrl += "&password=" + URLEncoder.encode(hashedPassword, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "UTF-8 not supported for password encoding, should not happen.", e);
                // Might proceed without password or return error
            }
        }
        Log.d(TAG, "Constructed API URL: " + apiUrl);

        HttpURLConnection connection = null;
        try {
            URL url = new URL(apiUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + accountToken);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Gofile API response code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                inputStream.close();

                String jsonResponseStr = response.toString();
                Log.d(TAG, "Gofile API Response JSON: " + jsonResponseStr);
                JSONObject jsonResponse = new JSONObject(jsonResponseStr);

                String status = jsonResponse.optString("status");
                if ("ok".equals(status)) {
                    JSONObject responseData = jsonResponse.optJSONObject("data");
                    if (responseData != null) {
                        // The root folder name for downloads should be the Gofile contentId itself
                        // to ensure uniqueness and grouping.
                        String rootFolderNameForDownload = contentId;

                        // The 'contents' field in the new API is a direct map of entries, not a nested 'rootFolder' object
                        // We need to iterate through the 'contents' map if it's the primary structure,
                        // or find the root entry if there's one (e.g. if 'id' of responseData matches contentId).
                        // The API for /contents/{contentId} returns the details of that specific content.
                        // If it's a folder, its children are in responseData.children.
                        // If it's a file, its details are directly in responseData.

                        // The responseData itself is the root entry.
                        // We pass responseData as the entry to parse.
                        // The rootFolderName for the DownloadItem's grouping is the contentId.
                        // The initial relative path is empty.
                        parseGofileEntry(responseData, downloadItems, rootFolderNameForDownload, "", accountToken);

                    } else {
                        Log.e(TAG, "'data' field missing in Gofile API response.");
                    }
                } else {
                    // Handle specific Gofile error statuses
                    if ("error-passwordRequired".equals(status) || "error-wrongPassword".equals(status)) {
                        Log.e(TAG, "Gofile API error: Password required or wrong. Status: " + status);
                        // May need to inform user about password issue.
                    } else if ("error-notFound".equals(status)){
                        Log.e(TAG, "Gofile API error: Content not found. Status: " + status);
                    } else if ("error-notPublic".equals(status)){
                         Log.e(TAG, "Gofile API error: Content not public. Status: " + status);
                    } else {
                        Log.e(TAG, "Gofile API request failed. Status: " + status + ", Description: " + jsonResponse.optString("description", "N/A"));
                    }
                }
            } else {
                Log.e(TAG, "Gofile API request failed. HTTP Code: " + responseCode + ", Message: " + connection.getResponseMessage());
                // Attempt to read error stream for more details if available
                try (InputStream errorStream = connection.getErrorStream()) {
                    if (errorStream != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                        StringBuilder errorResponse = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            errorResponse.append(line);
                        }
                        Log.e(TAG, "Gofile API error response: " + errorResponse.toString());
                    }
                } catch (IOException ex) {
                    Log.e(TAG, "IOException reading error stream", ex);
                }
            }
        } catch (MalformedURLException e) {
            Log.e(TAG, "Malformed API URL: " + apiUrl, e);
        } catch (IOException e) {
            Log.e(TAG, "IOException during Gofile API request", e);
        } catch (JSONException e) {
            Log.e(TAG, "JSONException parsing Gofile API response", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        Log.i(TAG, "Resolved " + downloadItems.size() + " items from Gofile URL " + gofilePageUrl);
        return new GofileResolvedResult(downloadItems, accountToken); // Return items and the token used
    }

    private void parseGofileEntry(JSONObject entryJson, List<DownloadItem> downloadItems, String rootFolderName, String currentRelativePath, String accountToken) throws JSONException {
        if (entryJson == null) {
            Log.w(TAG, "parseGofileEntry called with null entryJson.");
            return;
        }

        String type = entryJson.optString("type");
        String name = entryJson.optString("name"); // Name of the current file or folder
        // String entryId = entryJson.optString("id"); // ID of this specific entry

        // Log.d(TAG, "Parsing entry: '" + name + "', type: '" + type + "', currentRelativePath: '" + currentRelativePath + "' within root: '" + rootFolderName + "'");

        String itemPathForDownloadItem;
        if (currentRelativePath.isEmpty()) {
            itemPathForDownloadItem = name;
        } else {
            itemPathForDownloadItem = currentRelativePath + "/" + name;
        }

        if ("file".equals(type)) {
            String directLink = entryJson.optString("link");
            long size = entryJson.optLong("size", -1);
            // String md5 = entryJson.optString("md5"); // Available if needed

            if (directLink != null && !directLink.isEmpty()) {
                Log.d(TAG, "Found file: '" + itemPathForDownloadItem + "' (size: " + size + ") link: " + directLink);
                downloadItems.add(new DownloadItem(itemPathForDownloadItem, directLink, size, rootFolderName));
            } else {
                Log.w(TAG, "File entry '" + name + "' is missing a direct download link. Skipping.");
            }
        } else if ("folder".equals(type)) {
            Log.d(TAG, "Processing folder: '" + itemPathForDownloadItem + "'");
            JSONObject childrenMap = entryJson.optJSONObject("children"); // 'children' is a map (JSONObject)
            if (childrenMap != null) {
                Iterator<String> childIds = childrenMap.keys();
                while (childIds.hasNext()) {
                    String childId = childIds.next();
                    JSONObject childJson = childrenMap.getJSONObject(childId);
                    // The name of the child entry is inside childJson.getString("name")
                    // The new relative path for children will be the current itemPathForDownloadItem
                    parseGofileEntry(childJson, downloadItems, rootFolderName, itemPathForDownloadItem, accountToken);
                }
            } else {
                Log.d(TAG, "Folder '" + name + "' has no 'children' attribute or it's empty.");
            }
        } else {
            Log.w(TAG, "Unknown entry type: '" + type + "' for entry: '" + name + "'. Skipping.");
        }
    }
}
