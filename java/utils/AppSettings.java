package com.winlator.Download.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log; // Added import for android.util.Log

import org.json.JSONObject; // Added import
import org.json.JSONException; // Added import

import java.io.BufferedReader; // Added import
import java.io.IOException; // Added import
import java.io.InputStream; // Added import
import java.io.InputStreamReader; // Added import
import java.net.HttpURLConnection; // Added import
import java.net.MalformedURLException; // Added import
import java.net.URL; // Added import

public class AppSettings {

    public static final String PREFS_NAME = "download_settings";
    public static final String KEY_DOWNLOAD_PATH = "download_path";
    // Using "Default: Downloads folder" as the actual default string stored if the user desires the default.
    // This matches the behavior in SettingsActivity where an empty input resets to this string.
    public static final String DEFAULT_DOWNLOAD_PATH = "Default: Downloads folder";
    public static final String KEY_DISABLE_DIRECT_DOWNLOADS = "disable_direct_downloads";
    public static final String KEY_GOFILE_ACCOUNT_TOKEN = "gofile_account_token";
    private static final String GOFILE_CREATE_ACCOUNT_URL = "https://api.gofile.io/accounts";

    public static final String KEY_GOFILE_WT_TOKEN = "gofile_wt_token";
    public static final String KEY_GOFILE_WT_TIMESTAMP = "gofile_wt_timestamp";
    private static final String GOFILE_GLOBAL_JS_URL = "https://gofile.io/dist/js/global.js";
    private static final String GOFILE_WT_MARKER_PREFIX = "appdata.wt = \""; // Escaped quote
    private static final String GOFILE_WT_MARKER_SUFFIX = "\""; // Escaped quote
    private static final long GOFILE_WT_CACHE_DURATION_MS = 1 * 60 * 60 * 1000; // 1 hour

    // Getter for Download Path
    public static String getDownloadPath(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_DOWNLOAD_PATH, DEFAULT_DOWNLOAD_PATH);
    }

    // Setter for Download Path
    public static void setDownloadPath(Context context, String path) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_DOWNLOAD_PATH, path);
        editor.apply();
    }

    // Getter for Disable Direct Downloads
    public static boolean getDisableDirectDownloads(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_DISABLE_DIRECT_DOWNLOADS, false);
    }

    // Setter for Disable Direct Downloads
    public static void setDisableDirectDownloads(Context context, boolean isDisabled) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KEY_DISABLE_DIRECT_DOWNLOADS, isDisabled);
        editor.apply();
    }

    // Getter for Gofile Account Token
    public static String getGofileAccountToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String token = prefs.getString(KEY_GOFILE_ACCOUNT_TOKEN, null);
        if (token == null || token.isEmpty()) {
            // Perform network operation on a background thread if called from main thread
            // For simplicity in this subtask, direct call. In real app, ensure background execution.
            // However, this method might be called from DownloadService's background executor.
            try {
                Log.d("AppSettings.Gofile", "No Gofile token found, attempting to fetch a new one.");
                token = fetchNewGofileAccountToken(context);
            } catch (IOException e) {
                Log.e("AppSettings.Gofile", "IOException fetching new Gofile token", e);
                // In a real app, you might want to propagate this error or handle it more gracefully
                return null;
            }
        }
        return token;
    }

    // Setter for Gofile Account Token
    public static void setGofileAccountToken(Context context, String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_GOFILE_ACCOUNT_TOKEN, token);
        editor.apply();
        Log.i("AppSettings.Gofile", "Gofile account token saved.");
    }

    // Fetch new Gofile Account Token (private static)
    private static String fetchNewGofileAccountToken(Context context) throws IOException {
        // This method performs network operations and should ideally be called from a background thread.
        // The caller (getGofileAccountToken) should manage threading if necessary.
        HttpURLConnection connection = null;
        String token = null;
        try {
            URL url = new URL(GOFILE_CREATE_ACCOUNT_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            // Gofile's createAccount endpoint seems to be a POST, but might not require a body.
            // It might also prefer specific headers like 'Origin' or 'Referer' if it's web-oriented.
            // The provided Python script example did not include a body for this specific POST.
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
            connection.setRequestProperty("Connection", "keep-alive");
            // connection.setDoOutput(true); // Not setting DoOutput to true if no body is sent
            connection.setConnectTimeout(15000); // 15 seconds
            connection.setReadTimeout(15000);    // 15 seconds

            int responseCode = connection.getResponseCode();
            Log.d("AppSettings.Gofile", "Create account response code: " + responseCode);

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

                String jsonResponse = response.toString();
                Log.d("AppSettings.Gofile", "Create account response JSON: " + jsonResponse);
                // Example: {"status":"ok","data":{"token":"someTokenValue",...}}
                // More robust parsing:
                try {
                    JSONObject jsonObj = new JSONObject(jsonResponse);
                    if ("ok".equals(jsonObj.optString("status"))) {
                        JSONObject dataObj = jsonObj.optJSONObject("data");
                        if (dataObj != null) {
                            token = dataObj.optString("token", null);
                            if (token != null && !token.isEmpty()) {
                                setGofileAccountToken(context, token); // Save the fetched token
                                Log.i("AppSettings.Gofile", "New Gofile token fetched and saved: " + token);
                            } else {
                                Log.e("AppSettings.Gofile", "Fetched Gofile token is null or empty from JSON data.");
                            }
                        } else {
                            Log.e("AppSettings.Gofile", "Gofile 'data' object is missing in JSON response.");
                        }
                    } else {
                         Log.e("AppSettings.Gofile", "Gofile account creation status not OK. Status: " + jsonObj.optString("status"));
                    }
                } catch (JSONException e) {
                    Log.e("AppSettings.Gofile", "JSONException while parsing Gofile token response: " + jsonResponse, e);
                }
            } else {
                Log.e("AppSettings.Gofile", "Failed to fetch Gofile token. HTTP Code: " + responseCode + ", Message: " + connection.getResponseMessage());
            }
        } catch (MalformedURLException e) {
            Log.e("AppSettings.Gofile", "MalformedURLException for Gofile account URL: " + GOFILE_CREATE_ACCOUNT_URL, e);
            throw e;
        } catch (IOException e) {
            Log.e("AppSettings.Gofile", "IOException during Gofile token fetch", e);
            throw e;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return token;
    }

    // New constant for concurrent downloads limit
    public static final String PREF_KEY_MAX_CONCURRENT_DOWNLOADS = "max_concurrent_downloads";
    private static final int DEFAULT_MAX_CONCURRENT_DOWNLOADS = 3;

    // New method to get max concurrent downloads
    public static int getMaxConcurrentDownloads(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Preference might be stored as a string if using EditTextPreference, parse carefully
        try {
            String value = prefs.getString(PREF_KEY_MAX_CONCURRENT_DOWNLOADS, String.valueOf(DEFAULT_MAX_CONCURRENT_DOWNLOADS));
            int intValue = Integer.parseInt(value);
            return Math.max(1, Math.min(intValue, 10)); // Ensure it's within a reasonable range (e.g., 1-10)
        } catch (NumberFormatException e) {
            return DEFAULT_MAX_CONCURRENT_DOWNLOADS; // Fallback to default if parsing fails
        }
    }

    // Example setter if you were to add UI for it (not part of this subtask to call it)
    public static void setMaxConcurrentDownloads(Context context, int limit) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_KEY_MAX_CONCURRENT_DOWNLOADS, String.valueOf(limit)).apply();
    }

    public static String getDynamicGofileWt(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String cachedWt = prefs.getString(KEY_GOFILE_WT_TOKEN, null);
        long lastFetchTime = prefs.getLong(KEY_GOFILE_WT_TIMESTAMP, 0);

        if (cachedWt != null && !cachedWt.isEmpty() && (System.currentTimeMillis() - lastFetchTime < GOFILE_WT_CACHE_DURATION_MS)) {
            android.util.Log.d("AppSettings.GofileWT", "Using cached Gofile WT: " + cachedWt);
            return cachedWt;
        }

        android.util.Log.d("AppSettings.GofileWT", "Cached Gofile WT is null, empty, or expired. Fetching new one.");
        try {
            return fetchAndCacheDynamicGofileWt(context);
        } catch (IOException e) {
            android.util.Log.e("AppSettings.GofileWT", "IOException fetching new Gofile WT", e);
            return null; // Fallback to null if fetch fails
        }
    }

    private static String fetchAndCacheDynamicGofileWt(Context context) throws IOException {
        HttpURLConnection connection = null;
        String wtToken = null;
        try {
            URL url = new URL(GOFILE_GLOBAL_JS_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000); // 10 seconds
            connection.setReadTimeout(10000);    // 10 seconds

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                // Read only a certain amount to avoid OOM if the file is unexpectedly large
                // and to speed up finding the token which is usually near the top.
                // However, for this specific implementation, we'll read line by line
                // and break if the token is found or if too many lines are read.
                // A simpler approach for now is to read the relevant part.
                // The token is usually within the first few KB.
                char[] buffer = new char[4096]; // Read in chunks
                int charsRead;
                while ((charsRead = reader.read(buffer, 0, buffer.length)) != -1) {
                    response.append(buffer, 0, charsRead);
                    // Check if the marker is present to potentially stop early
                    if (response.indexOf(GOFILE_WT_MARKER_PREFIX) != -1 && response.indexOf(GOFILE_WT_MARKER_SUFFIX, response.indexOf(GOFILE_WT_MARKER_PREFIX)) != -1) {
                        break;
                    }
                     // Basic protection against extremely large unexpected responses
                    if (response.length() > 2 * 1024 * 1024) { // 2MB limit
                        Log.e("AppSettings.GofileWT", "Response from global.js is too large, aborting parse.");
                        break;
                    }
                }
                reader.close();
                inputStream.close();

                String jsContent = response.toString();
                int startIndex = jsContent.indexOf(GOFILE_WT_MARKER_PREFIX);
                if (startIndex != -1) {
                    startIndex += GOFILE_WT_MARKER_PREFIX.length();
                    int endIndex = jsContent.indexOf(GOFILE_WT_MARKER_SUFFIX, startIndex);
                    if (endIndex != -1) {
                        wtToken = jsContent.substring(startIndex, endIndex);
                        if (wtToken != null && !wtToken.isEmpty()) {
                            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString(KEY_GOFILE_WT_TOKEN, wtToken);
                            editor.putLong(KEY_GOFILE_WT_TIMESTAMP, System.currentTimeMillis());
                            editor.apply();
                            android.util.Log.i("AppSettings.GofileWT", "Fetched and cached new Gofile WT: " + wtToken);
                        } else {
                            android.util.Log.e("AppSettings.GofileWT", "Extracted Gofile WT is null or empty.");
                        }
                    } else {
                        android.util.Log.e("AppSettings.GofileWT", "Gofile WT suffix marker not found in JS content.");
                    }
                } else {
                    android.util.Log.e("AppSettings.GofileWT", "Gofile WT prefix marker not found in JS content.");
                }
            } else {
                android.util.Log.e("AppSettings.GofileWT", "Failed to fetch Gofile global.js. HTTP Code: " + responseCode);
            }
        } catch (MalformedURLException e) {
            android.util.Log.e("AppSettings.GofileWT", "MalformedURLException for Gofile global.js URL", e);
            throw e;
        } catch (IOException e) {
            android.util.Log.e("AppSettings.GofileWT", "IOException during Gofile WT fetch", e);
            throw e;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return wtToken;
    }
}
