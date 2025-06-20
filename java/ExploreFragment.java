package com.winlator.Download;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.winlator.Download.adapter.ExploreCommunityGamesAdapter;
import com.winlator.Download.model.CommunityGame;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.os.AsyncTask;
import android.util.Log;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import com.winlator.Download.adapter.ReleasesAdapter;
import com.winlator.Download.model.Release;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExploreFragment extends Fragment implements ReleasesAdapter.OnReleaseClickListener {

    private RecyclerView rvCommunityGames;
    private ExploreCommunityGamesAdapter communityGamesAdapter;
    private List<CommunityGame> communityGamesList;
    private ExecutorService executor;

    // New RecyclerViews and Adapters
    private RecyclerView rvTestReleasesExplore;
    private RecyclerView rvFixesExplore;
    private ReleasesAdapter testReleasesAdapter;
    private ReleasesAdapter fixesAdapter;

    private static final String GITHUB_JSON_URL = "https://raw.githubusercontent.com/DEYVIDYT/WINLATOR-DOWNLOAD/refs/heads/main/WINLATOR.json";
    // These will be used to filter from the "Winlator" and "Components" main categories from JSON
    private static final String SOURCE_AFEI = "Afei";
    private static final String SOURCE_AJAY = "Ajay";
    private static final String CATEGORY_COMPONENTS = "Components";


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_explore, container, false);
        executor = Executors.newSingleThreadExecutor(); // For community games
        // No need for a separate executor for AsyncTask, it manages its own.
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Community Games Setup
        rvCommunityGames = view.findViewById(R.id.rv_community_games_explore);
        communityGamesList = new ArrayList<>();
        communityGamesAdapter = new ExploreCommunityGamesAdapter(getContext(), communityGamesList);
        rvCommunityGames.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCommunityGames.setAdapter(communityGamesAdapter);
        loadCommunityGames();

        // Test Releases Setup
        rvTestReleasesExplore = view.findViewById(R.id.rv_test_releases_explore);
        rvTestReleasesExplore.setLayoutManager(new LinearLayoutManager(getContext()));
        testReleasesAdapter = new ReleasesAdapter(getContext(), this); // Pass 'this' as listener
        rvTestReleasesExplore.setAdapter(testReleasesAdapter);

        // Fixes Setup
        rvFixesExplore = view.findViewById(R.id.rv_fixes_explore);
        rvFixesExplore.setLayoutManager(new LinearLayoutManager(getContext()));
        fixesAdapter = new ReleasesAdapter(getContext(), this); // Pass 'this' as listener
        rvFixesExplore.setAdapter(fixesAdapter);

        new FetchReleasesDataTask().execute(GITHUB_JSON_URL);
    }

    private void loadCommunityGames() {
        executor.execute(() -> {
            try {
                URL url = new URL("https://ldgames.x10.mx/list_games.php"); // Community games URL
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000); // 10 seconds
                connection.setReadTimeout(10000);  // 10 seconds

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    parseGamesJson(response.toString()); // For community games
                } else {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Error loading community games: " + responseCode, Toast.LENGTH_SHORT).show());
                    }
                }
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Community games connection error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void parseGamesJson(String jsonString) { // This is for Community Games (ldgames.x10.mx)
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            List<CommunityGame> newGamesList = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject gameObject = jsonArray.getJSONObject(i);
                String name = gameObject.getString("name");
                String sizeStr = gameObject.getString("size"); // "1.2 GB" or "500 MB"
                String url = gameObject.getString("url");

                double sizeInGB = parseSizeToGB(sizeStr);

                CommunityGame game = new CommunityGame(name, sizeInGB, url);
                newGamesList.add(game);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (newGamesList != null) {
                        Collections.reverse(newGamesList);
                    }
                    if (communityGamesAdapter != null) { // Ensure correct adapter is updated
                        communityGamesAdapter.setGames(newGamesList);
                    }
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error processing community games data.", Toast.LENGTH_SHORT).show());
            }
        }
    }

    private double parseSizeToGB(String sizeStr) { // For community games
        if (sizeStr == null || sizeStr.trim().isEmpty()) {
            return 0.0;
        }
        sizeStr = sizeStr.trim().toLowerCase();
        Pattern pattern = Pattern.compile("([\\d.]+)\\s*(gb|mb)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sizeStr);

        if (matcher.find()) {
            try {
                double value = Double.parseDouble(matcher.group(1));
                String unit = matcher.group(2).toLowerCase();
                if ("mb".equals(unit)) {
                    return value / 1024.0;
                } else if ("gb".equals(unit)) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // Log error or handle
                return 0.0;
            }
        }
        return 0.0; // Default or error
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Nullify views and adapter to prevent memory leaks
        rvCommunityGames = null;
        communityGamesAdapter = null;
        communityGamesList = null;

        rvTestReleasesExplore = null;
        testReleasesAdapter = null;
        rvFixesExplore = null;
        fixesAdapter = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown(); // For community games loader
        }
        // AsyncTask manages its own lifecycle mostly, but good practice if it held direct fragment refs.
    }

    // Implementation for ReleasesAdapter.OnReleaseClickListener
    @Override
    public void onReleaseDownloadClick(Release release) {
        // Handle download click - maybe start a download service or show a dialog
        Toast.makeText(getContext(), "Download: " + release.getAssetName(), Toast.LENGTH_SHORT).show();
        // Example: Intent intent = new Intent(getContext(), DownloadService.class);
        // intent.putExtra("url", release.getDownloadUrl());
        // intent.putExtra("fileName", release.getAssetName());
        // getContext().startService(intent);
    }

    @Override
    public void onReleaseItemClick(Release release) {
        // Handle item click - maybe show more details
        Toast.makeText(getContext(), "Details for: " + release.getName(), Toast.LENGTH_SHORT).show();
        // Example: new MaterialAlertDialogBuilder(requireContext())
        // .setTitle(release.getName())
        // .setMessage(release.getBody())
        // .setPositiveButton("OK", null)
        // .show();
    }


    // AsyncTask to fetch API data for Releases (Test and Fixes)
    private class FetchReleasesDataTask extends AsyncTask<String, Void, Map<String, List<Release>>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Show ProgressBar if available
        }

        @Override
        protected Map<String, List<Release>> doInBackground(String... urls) {
            if (urls.length == 0) return null;
            String urlString = urls[0];
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            Map<String, List<Release>> allReleasesData = new LinkedHashMap<>();

            try {
                URL url = new URL(urlString);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setConnectTimeout(15000);
                urlConnection.setReadTimeout(15000);
                urlConnection.connect();

                if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.e("FetchReleasesDataTask", "HTTP error: " + urlConnection.getResponseCode());
                    return null;
                }

                StringBuilder buffer = new StringBuilder();
                reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) buffer.append(line).append("\n");

                if (buffer.length() == 0) return null;

                JSONObject jsonObject = new JSONObject(buffer.toString());
                Iterator<String> categories = jsonObject.keys(); // e.g., "Winlator", "Components"
                while (categories.hasNext()) {
                    String categoryKey = categories.next();
                    JSONObject categoryObject = jsonObject.getJSONObject(categoryKey);
                    List<Release> releasesForCurrentCategory = new ArrayList<>();

                    Iterator<String> repoKeys = categoryObject.keys(); // e.g., "Oficial", "Afei", "Brasil"
                    while (repoKeys.hasNext()) {
                        String repoNameKey = repoKeys.next(); // This is "Oficial", "Afei", etc.
                        String repoUrl = categoryObject.getString(repoNameKey);
                        String apiUrl = convertToApiUrl(repoUrl);
                        if (apiUrl != null) {
                            // Pass repoNameKey to be stored with the release or used for categorization
                            Release latestRelease = fetchLatestRelease(apiUrl, repoNameKey, repoUrl);
                            if (latestRelease != null) {
                                // Store the source key (Afei, Ajay, Components) with the release if needed,
                                // or use it directly for categorization later. For now, just add.
                                releasesForCurrentCategory.add(latestRelease);
                            }
                        }
                    }
                    allReleasesData.put(categoryKey, releasesForCurrentCategory); // Keyed by "Winlator", "Components"
                }
                return allReleasesData;
            } catch (Exception e) {
                Log.e("FetchReleasesDataTask", "Error fetching/parsing API data", e);
                return null;
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
                if (reader != null) try { reader.close(); } catch (IOException e) { Log.e("FetchReleasesDataTask", "Error closing reader", e); }
            }
        }

        @Override
        protected void onPostExecute(Map<String, List<Release>> result) {
            super.onPostExecute(result);
            // Hide ProgressBar

            if (result != null && !result.isEmpty()) {
                List<Release> winlatorReleases = result.get("Winlator");
                List<Release> componentReleases = result.get(CATEGORY_COMPONENTS);

                List<Release> finalTestReleases = new ArrayList<>();
                List<Release> finalFixesReleases = new ArrayList<>();

                if (winlatorReleases != null) {
                    for (Release rel : winlatorReleases) {
                        // Assuming repoName was stored in Release.getName() or similar if needed,
                        // or we rely on the source_key from fetchLatestRelease if we modified Release model.
                        // For now, using the name passed to fetchLatestRelease (which was repoNameKey e.g. "Afei")
                        // We need to ensure this repoNameKey is available with the Release object.
                        // Let's assume Release.getName() was set to repoNameKey during fetch.
                        if (rel.getName().equals(SOURCE_AFEI) || rel.getName().equals(SOURCE_AJAY)) {
                            finalTestReleases.add(rel);
                        }
                    }
                }

                if (componentReleases != null) {
                    finalFixesReleases.addAll(componentReleases);
                }

                if (!finalTestReleases.isEmpty()) {
                    testReleasesAdapter.updateData(finalTestReleases);
                } else {
                    Log.w("ExploreFragment", "No releases matched for Test Releases from 'Winlator' category sources: " + SOURCE_AFEI + ", " + SOURCE_AJAY);
                     if (getContext() != null) Toast.makeText(getContext(), "No Test Releases found.", Toast.LENGTH_SHORT).show();
                    testReleasesAdapter.updateData(new ArrayList<>()); // Clear if previously populated
                }

                if (!finalFixesReleases.isEmpty()) {
                    fixesAdapter.updateData(finalFixesReleases);
                } else {
                    Log.w("ExploreFragment", "No releases found for category: " + CATEGORY_COMPONENTS);
                    if (getContext() != null) Toast.makeText(getContext(), "No Fixes found.", Toast.LENGTH_SHORT).show();
                    fixesAdapter.updateData(new ArrayList<>()); // Clear
                }

            } else {
                Log.e("ExploreFragment", "Failed to fetch or parse releases data.");
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load Test/Fix releases.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // Helper method to convert GitHub repo URL to API URL for latest release
    private String convertToApiUrl(String githubUrl) {
        if (githubUrl == null || !githubUrl.contains("github.com")) {
            return null;
        }
        Pattern pattern = Pattern.compile("https://github\\.com/([^/]+)/([^/]+)(?:/releases)?");
        Matcher matcher = pattern.matcher(githubUrl);
        if (matcher.find()) {
            return "https://api.github.com/repos/" + matcher.group(1) + "/" + matcher.group(2) + "/releases/latest";
        }
        return null; // Return null if pattern doesn't match
    }

    // Helper method to fetch the latest release details from a GitHub API URL
    private Release fetchLatestRelease(String apiUrl, String repoNameKey, String htmlUrl) { // repoNameKey is "Afei", "Oficial" etc.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(apiUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            // GitHub API recommends setting an explicit Accept header
            urlConnection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            urlConnection.setConnectTimeout(10000); // 10 seconds
            urlConnection.setReadTimeout(10000);  // 10 seconds
            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e("fetchLatestRelease", "HTTP error " + responseCode + " for " + apiUrl);
                // You might want to read the error stream here for more details from GitHub API
                return null;
            }

            StringBuilder buffer = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }

            if (buffer.length() == 0) {
                Log.w("fetchLatestRelease", "Empty response from " + apiUrl);
                return null;
            }

            JSONObject releaseJson = new JSONObject(buffer.toString());
            String tagName = releaseJson.optString("tag_name", "");
            // Use repoNameKey for the release "name" if actual release name is empty, or to identify source
            String releaseName = releaseJson.optString("name", tagName);
            if (releaseName.isEmpty() && !tagName.isEmpty()) releaseName = tagName;
            else if (releaseName.isEmpty() && tagName.isEmpty()) releaseName = repoNameKey; // Fallback to repo key

            String body = releaseJson.optString("body", "");
            String publishedAt = releaseJson.optString("published_at", "");
            // HTML URL is passed, but if the JSON has a better one, prefer that.
            String releaseHtmlUrl = releaseJson.optString("html_url", htmlUrl);


            JSONArray assets = releaseJson.optJSONArray("assets");
            String downloadUrl = "";
            String assetName = "";
            long assetSize = 0;

            if (assets != null && assets.length() > 0) {
                // Attempt to find .apk or .obb, prioritize .apk
                JSONObject primaryAsset = null;
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.getJSONObject(i);
                    String currentAssetName = asset.optString("name", "").toLowerCase();
                    if (currentAssetName.endsWith(".apk")) {
                        primaryAsset = asset;
                        break;
                    }
                    if (primaryAsset == null && currentAssetName.endsWith(".obb")) { // If no apk, take first obb
                        primaryAsset = asset;
                    }
                }
                if (primaryAsset == null) primaryAsset = assets.getJSONObject(0); // Fallback to first asset

                downloadUrl = primaryAsset.optString("browser_download_url", "");
                assetName = primaryAsset.optString("name", "");
                assetSize = primaryAsset.optLong("size", 0);
            } else {
                 // Fallback if no assets array: try zipball/tarball (less ideal for apps)
                downloadUrl = releaseJson.optString("zipball_url", "");
                assetName = repoNameKey + "-" + tagName + ".zip"; // Construct a name
                // Size is not directly available for zipball_url from this endpoint
            }

            // To ensure we can categorize based on original repo key (Afei, Ajay, Components)
            // we will use the repoNameKey as the Release object's "name".
            // The actual release title from GitHub can be in `tagName` or a new field if needed.
            return new Release(repoNameKey, tagName, publishedAt, body, releaseHtmlUrl, downloadUrl, assetName, assetSize);

        } catch (MalformedURLException e) {
            Log.e("fetchLatestRelease", "Malformed API URL: " + apiUrl, e);
            return null;
        } catch (IOException e) {
            Log.e("fetchLatestRelease", "IOException for " + apiUrl, e);
            return null;
        } catch (JSONException e) {
            Log.e("fetchLatestRelease", "JSON parsing error for " + apiUrl, e);
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e("fetchLatestRelease", "Error closing reader", e);
                }
            }
        }
    }
}
