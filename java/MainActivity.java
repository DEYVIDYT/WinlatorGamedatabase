package com.winlator.Download;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;
// import androidx.appcompat.app.AlertDialog; // Using MaterialAlertDialogBuilder
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.winlator.Download.model.Release;

// Remove direct imports of individual community fragments from here,
// as they will be managed by CommunityHubFragment.
// import com.winlator.Download.CommunityGamesFragment; // No longer directly used by this adapter
// import com.winlator.Download.CommunityTestFragment;  // No longer directly used by this adapter
// import com.winlator.Download.CommunityFixFragment;   // No longer directly used by this adapter

// Import for the new CommunityHubFragment (will be created in a subsequent step)
// For now, this will cause a compile error until CommunityHubFragment is created.
// We will proceed assuming it will be created.
// import com.winlator.Download.CommunityHubFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private MyPagerAdapter pagerAdapter;
    private Map<String, List<Release>> apiData = new LinkedHashMap<>();
    private ProgressBar progressBar;
    private TextView errorTextView;
    private com.google.android.material.tabs.TabLayoutMediator tabLayoutMediator;

    private static final String API_URL = "https://raw.githubusercontent.com/DEYVIDYT/WINLATOR-DOWNLOAD/refs/heads/main/WINLATOR.json";
    private static final int STORAGE_PERMISSION_CODE = 101;

    // Updated Tab Position Constants
    private static final int COMMUNITY_HUB_POS = 0; // Single "Community" tab
    private static final int DYNAMIC_TABS_START_POS = 1; // Dynamic tabs start after "Community"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivityIfAvailable(this);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        progressBar = findViewById(R.id.progressBar);
        errorTextView = findViewById(R.id.errorTextView);

        apiData = new LinkedHashMap<>();

        pagerAdapter = new MyPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        this.tabLayoutMediator = new TabLayoutMediator(tabLayout, viewPager,
            (tab, position) -> {
                if (pagerAdapter != null) {
                    tab.setText(pagerAdapter.getPageTitle(position));
                }
            }
        );
        this.tabLayoutMediator.attach();

        new FetchApiDataTask().execute(API_URL);

        if (!checkStoragePermissions()) {
            requestStoragePermissions();
        } else {
            Log.d("MainActivity", "Storage permissions already granted.");
        }
    }

    // checkStoragePermissions, requestStoragePermissions, onRequestPermissionsResult, showPermissionDeniedDialog,
    // onCreateOptionsMenu, onOptionsItemSelected remain the same.

        private boolean checkStoragePermissions() {
        boolean readGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
            boolean writeGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            return readGranted && writeGranted;
        } else {
            return readGranted;
        }
    }

    private void requestStoragePermissions() {
         ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                STORAGE_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (checkStoragePermissions()) {
                Toast.makeText(this, "Permissões de armazenamento concedidas!", Toast.LENGTH_SHORT).show();
            } else {
                boolean canRequestAgain = false;
                for (String permission : permissions) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                        canRequestAgain = true;
                        break;
                    }
                }
                if (canRequestAgain) {
                    showPermissionDeniedDialog(false);
                } else {
                    showPermissionDeniedDialog(true);
                }
            }
        }
    }

    private void showPermissionDeniedDialog(boolean goToSettings) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Permissão Necessária");
        builder.setMessage("Esta aplicação precisa da permissão de armazenamento para funcionar corretamente. Por favor, conceda a permissão.");
        builder.setCancelable(false);

        if (goToSettings) {
            builder.setPositiveButton("Abrir Configurações", (dialog, which) -> {
                dialog.dismiss();
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            });
            builder.setNegativeButton("Sair", (dialog, which) -> {
                dialog.dismiss();
                finish();
            });
        } else {
            builder.setPositiveButton("Conceder Permissão", (dialog, which) -> {
                dialog.dismiss();
                requestStoragePermissions();
            });
            builder.setNegativeButton("Sair", (dialog, which) -> {
                dialog.dismiss();
                finish();
            });
        }
        builder.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_downloads) {
            startActivity(new Intent(this, DownloadManagerActivity.class));
            return true;
        } else if (itemId == R.id.action_community_games) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (itemId == R.id.action_upload_monitor) {
            startActivity(new Intent(this, UploadMonitorActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class MyPagerAdapter extends FragmentStateAdapter {
        private Map<String, List<Release>> dynamicData;
        private List<String> dynamicCategories;

        private static final String TITLE_COMMUNITY_HUB = "Comunidade";

        public MyPagerAdapter(AppCompatActivity activity) {
            super(activity);
            this.dynamicData = new LinkedHashMap<>();
            this.dynamicCategories = new ArrayList<>();
        }

        public void updateData(Map<String, List<Release>> newDynamicData) {
            this.dynamicData.clear();
            this.dynamicCategories.clear();
            if (newDynamicData != null) {
                this.dynamicData.putAll(newDynamicData);
                this.dynamicCategories.addAll(newDynamicData.keySet());
            }
            notifyDataSetChanged();
        }

        public CharSequence getPageTitle(int position) {
            if (position == COMMUNITY_HUB_POS) {
                return TITLE_COMMUNITY_HUB;
            } else {
                int dynamicIndex = position - DYNAMIC_TABS_START_POS;
                if (dynamicIndex >= 0 && dynamicIndex < dynamicCategories.size()) {
                    return dynamicCategories.get(dynamicIndex);
                }
                return "";
            }
        }

        @Override
        public Fragment createFragment(int position) {
            if (position == COMMUNITY_HUB_POS) {
                // This will require CommunityHubFragment to be created.
                // If it's not available, this line will cause a compilation error.
                // For the purpose of this subtask, we assume it will be created.
                return new com.winlator.Download.CommunityHubFragment();
            } else {
                int dynamicIndex = position - DYNAMIC_TABS_START_POS;
                if (dynamicIndex >= 0 && dynamicIndex < dynamicCategories.size()) {
                    String category = dynamicCategories.get(dynamicIndex);
                    List<Release> categoryReleases = dynamicData.get(category);
                    if (categoryReleases == null) {
                        categoryReleases = new ArrayList<>();
                    }
                    return ReleasesFragment.newInstance(category, categoryReleases);
                }
                return new Fragment(); // Fallback
            }
        }

        @Override
        public int getItemCount() {
            // 1 static "Community" tab + number of dynamic categories
            return DYNAMIC_TABS_START_POS + dynamicCategories.size();
        }
    }

    // FetchApiDataTask remains largely the same, its onPostExecute will correctly
    // update the adapter and the TabLayoutMediator will refresh with the new structure.
    private class FetchApiDataTask extends AsyncTask<String, Void, Map<String, List<Release>>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            if (errorTextView != null) errorTextView.setVisibility(View.GONE);
            if (tabLayout != null) tabLayout.setVisibility(View.GONE);
            if (viewPager != null) viewPager.setVisibility(View.GONE);
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
                    Log.e("MainActivity", "HTTP error: " + urlConnection.getResponseCode());
                    return null;
                }

                StringBuilder buffer = new StringBuilder();
                reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) buffer.append(line).append("\n");

                if (buffer.length() == 0) return null;

                JSONObject jsonObject = new JSONObject(buffer.toString());
                Iterator<String> categories = jsonObject.keys();
                while (categories.hasNext()) {
                    String category = categories.next();
                    JSONObject categoryObject = jsonObject.getJSONObject(category);
                    List<Release> releasesForCategory = new ArrayList<>();
                    Iterator<String> repos = categoryObject.keys();
                    while (repos.hasNext()) {
                        String repoName = repos.next();
                        String repoUrl = categoryObject.getString(repoName);
                        String apiUrl = convertToApiUrl(repoUrl);
                        if (apiUrl != null) {
                            Release latestRelease = fetchLatestRelease(apiUrl, repoName, repoUrl);
                            if (latestRelease != null) releasesForCategory.add(latestRelease);
                        }
                    }
                    allReleasesData.put(category, releasesForCategory);
                }
                return allReleasesData;
            } catch (Exception e) {
                Log.e("MainActivity", "Error fetching/parsing API data", e);
                return null;
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
                if (reader != null) try { reader.close(); } catch (Exception e) { /* ignore */ }
            }
        }

        @Override
        protected void onPostExecute(Map<String, List<Release>> result) {
            super.onPostExecute(result);
            if (isDestroyed() || isFinishing()) return;

            if (progressBar != null) progressBar.setVisibility(View.GONE);

            if (pagerAdapter != null) {
                pagerAdapter.updateData(result != null ? result : new LinkedHashMap<>());
            }

            if (tabLayout != null && viewPager != null && pagerAdapter != null && MainActivity.this.tabLayoutMediator != null) {
                MainActivity.this.tabLayoutMediator.detach();
                MainActivity.this.tabLayoutMediator = new TabLayoutMediator(tabLayout, viewPager,
                    (tab, position) -> {
                        if (pagerAdapter != null) {
                            tab.setText(pagerAdapter.getPageTitle(position));
                        }
                    }
                );
                MainActivity.this.tabLayoutMediator.attach();
            }
            
            if (tabLayout != null) tabLayout.setVisibility(View.VISIBLE);
            if (viewPager != null) viewPager.setVisibility(View.VISIBLE);

            if (result == null && (pagerAdapter == null || pagerAdapter.dynamicCategories.isEmpty())) {
                 if (errorTextView != null) {
                    errorTextView.setText("Erro ao carregar dados da API. A aba Comunidade ainda está disponível.");
                    errorTextView.setVisibility(View.VISIBLE);
                }
            } else if (errorTextView != null) {
                errorTextView.setVisibility(View.GONE);
            }
        }

        private String convertToApiUrl(String githubUrl) {
            if (githubUrl != null && githubUrl.contains("github.com")) {
                Pattern pattern = Pattern.compile("https://github\\.com/([^/]+)/([^/]+)");
                Matcher matcher = pattern.matcher(githubUrl);
                if (matcher.find()) {
                    return "https://api.github.com/repos/" + matcher.group(1) + "/" + matcher.group(2) + "/releases/latest";
                }
            }
            return null;
        }

        private Release fetchLatestRelease(String apiUrl, String repoName, String htmlUrl) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            try {
                URL url = new URL(apiUrl);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                urlConnection.setConnectTimeout(15000);
                urlConnection.setReadTimeout(15000);
                urlConnection.connect();

                if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.e("MainActivity", "HTTP error " + urlConnection.getResponseCode() + " for " + apiUrl);
                    return null;
                }

                StringBuilder buffer = new StringBuilder();
                reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) buffer.append(line).append("\n");

                JSONObject releaseJson = new JSONObject(buffer.toString());
                String tagName = releaseJson.optString("tag_name", "");
                String releaseName = releaseJson.optString("name", tagName);
                String body = releaseJson.optString("body", "");

                JSONArray assets = releaseJson.optJSONArray("assets");
                String downloadUrl = "";
                String assetName = "";
                long assetSize = 0;

                if (assets != null && assets.length() > 0) {
                    JSONObject firstAsset = assets.getJSONObject(0);
                    downloadUrl = firstAsset.optString("browser_download_url", "");
                    assetName = firstAsset.optString("name", "");
                    assetSize = firstAsset.optLong("size", 0);
                }
                if (downloadUrl.isEmpty()) {
                    downloadUrl = releaseJson.optString("zipball_url", "");
                    assetName = repoName + "-" + tagName + ".zip";
                }
                return new Release(repoName, tagName, releaseName, body, downloadUrl, htmlUrl, assetName, assetSize);
            } catch (Exception e) {
                Log.e("MainActivity", "Error fetching release from " + apiUrl, e);
                return null;
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
                if (reader != null) try { reader.close(); } catch (Exception e) { /* ignore */ }
            }
        }
    }
}
