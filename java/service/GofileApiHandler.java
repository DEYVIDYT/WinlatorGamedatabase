package com.winlator.Download.service;

import android.util.Log;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.BufferedReader;
// DataOutputStream or OutputStream are not needed here if we send no body
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class GofileApiHandler {

    private static final String TAG = "GofileApiHandler";
    private static final String API_BASE_URL = "https://api.gofile.io";
    private static final String USER_AGENT = "Mozilla/5.0";

    public static class GofileEntry {
        // ... (GofileEntry class remains the same)
        public String id;
        public String type;
        public String name;
        public String directLink;
        public long size;
        public String code;
        public String parentFolderId;
        public List<GofileEntry> children;

        public GofileEntry() {
            this.children = new ArrayList<>();
        }

        @Override
        public String toString() {
            return "GofileEntry{" +
                    "id='" + id + '\'' +
                    ", type='" + type + '\'' +
                    ", name='" + name + '\'' +
                    (directLink != null ? ", directLink='" + directLink + '\'' : "") +
                    (size > 0 ? ", size=" + size : "") +
                    (code != null ? ", code='" + code + '\'' : "") +
                    (parentFolderId != null ? ", parentFolderId='" + parentFolderId + '\'' : "") +
                    (!children.isEmpty() ? ", childrenCount=" + children.size() : "") +
                    '}';
        }
    }

    public String getGuestToken() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(API_BASE_URL + "/accounts");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
            conn.setRequestProperty("Connection", "keep-alive");

            // Modifications for a "bodiless" POST with Content-Length: 0
            conn.setDoOutput(true); // Still needed to indicate a body *could* be sent, allowing Content-Length
            conn.setRequestProperty("Content-Length", "0");
            // REMOVED: conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            // NO DATA is written to conn.getOutputStream()

            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            // Explicitly call connect() before getResponseCode() can be good practice,
            // though getResponseCode() usually implicitly connects.
            // conn.connect(); // Optional: some prefer to explicitly call it.

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "getGuestToken: Response Code: " + responseCode);
            Log.d(TAG, "getGuestToken: Response Message: " + conn.getResponseMessage());

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                Log.d(TAG, "getGuestToken: OK Response Body: " + response.toString());

                JSONObject jsonResponse = new JSONObject(response.toString());
                if ("ok".equals(jsonResponse.getString("status"))) {
                    return jsonResponse.getJSONObject("data").getString("token");
                } else {
                    Log.e(TAG, "getGuestToken: API status not OK. Response: " + response.toString());
                }
            } else {
                Log.e(TAG, "getGuestToken: HTTP Error. Code: " + responseCode + " Message: " + conn.getResponseMessage());
                if (conn.getErrorStream() != null) {
                    BufferedReader errorStream = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    String errorLine;
                    StringBuilder errorResponse = new StringBuilder();
                    while ((errorLine = errorStream.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    errorStream.close();
                    Log.e(TAG, "getGuestToken: Error stream response: " + errorResponse.toString());
                } else {
                    Log.e(TAG, "getGuestToken: No error stream available, but response code was not OK.");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getGuestToken: Exception", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    private String sha256(final String base) {
        // ... (sha256 implementation remains the same)
        try{
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            final StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < hash.length; i++) {
                final String hex = Integer.toHexString(0xff & hash[i]);
                if(hex.length() == 1)
                  hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch(NoSuchAlgorithmException e){
           Log.e(TAG, "SHA-256 Algorithm not found", e);
           return null;
        }
    }

    public GofileEntry getContentDetails(String contentId, String token, String password) {
        // ... (getContentDetails implementation remains the same from previous correction)
        if (contentId == null || token == null) {
            Log.e(TAG, "getContentDetails: Content ID or Token is null.");
            return null;
        }

        HttpURLConnection conn = null;
        try {
            String urlString = API_BASE_URL + "/contents/" + contentId +
                               "?wt=4fd6sg89d7s6&cache=true&sortField=createTime&sortDirection=1";
            if (password != null && !password.isEmpty()) {
                String hashedPassword = sha256(password);
                if (hashedPassword == null) {
                    Log.e(TAG, "getContentDetails: Failed to hash password.");
                } else {
                     urlString += "&password=" + hashedPassword;
                }
            }

            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
            conn.setRequestProperty("Connection", "keep-alive");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "getContentDetails: Response Code: " + responseCode + " for URL: " + urlString);
            Log.d(TAG, "getContentDetails: Response Message: " + conn.getResponseMessage());

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                Log.d(TAG, "getContentDetails: OK Response Body: " + response.toString());

                JSONObject jsonResponse = new JSONObject(response.toString());
                if ("ok".equals(jsonResponse.getString("status"))) {
                    JSONObject data = jsonResponse.getJSONObject("data");
                     if (data.has("passwordStatus") && !"passwordOk".equals(data.getString("passwordStatus"))) {
                        Log.w(TAG, "getContentDetails: Password required or incorrect. Status: " + data.getString("passwordStatus"));
                        return null;
                    }
                    return parseGofileEntry(data, null);
                } else {
                    Log.e(TAG, "getContentDetails: API status not OK. Response: " + response.toString());
                }
            } else {
                Log.e(TAG, "getContentDetails: HTTP Error. Code: " + responseCode + " Message: " + conn.getResponseMessage());
                 if (conn.getErrorStream() != null) {
                    BufferedReader errorStream = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    String errorLine;
                    StringBuilder errorResponse = new StringBuilder();
                    while ((errorLine = errorStream.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    errorStream.close();
                    Log.e(TAG, "getContentDetails: Error stream response: " + errorResponse.toString());
                } else {
                    Log.e(TAG, "getContentDetails: No error stream available, but response code was not OK.");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getContentDetails: Exception", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    private GofileEntry parseGofileEntry(JSONObject data, String parentId) throws JSONException {
        // ... (parseGofileEntry implementation remains the same)
        GofileEntry entry = new GofileEntry();
        entry.id = data.getString("id");
        entry.type = data.getString("type");
        entry.name = data.getString("name");
        entry.parentFolderId = parentId;

        if ("file".equals(entry.type)) {
            entry.directLink = data.optString("link", null);
            entry.size = data.optLong("size", 0);
            entry.code = data.optString("code", null);
            if (entry.directLink == null) {
                 Log.w(TAG, "File entry " + entry.name + " has no direct link. Data: " + data.toString());
            }
        } else if ("folder".equals(entry.type)) {
            if (data.has("children")) {
                JSONObject childrenObject = data.getJSONObject("children");
                Iterator<String> keys = childrenObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONObject childData = childrenObject.getJSONObject(key);
                    GofileEntry childEntry = parseGofileEntry(childData, entry.id);
                    if (childEntry != null) {
                         childEntry.code = key;
                         entry.children.add(childEntry);
                    }
                }
            }
        }
        Log.d(TAG, "Parsed GofileEntry: " + entry.toString());
        return entry;
    }
}
