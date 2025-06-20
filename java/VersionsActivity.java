package com.winlator.Download;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.DynamicColors; // Importar DynamicColors
import com.winlator.Download.adapter.VersionsAdapter;
import com.winlator.Download.model.Release;
import com.winlator.Download.model.WinlatorVersion;
import com.winlator.Download.service.DownloadService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionsActivity extends AppCompatActivity implements VersionsAdapter.OnVersionClickListener {

    private static final String TAG = "VersionsActivity";
    public static final String EXTRA_RELEASE = "extra_release";
    public static final String EXTRA_REPOSITORY_NAME = "extra_repository_name";
    public static final String EXTRA_REPOSITORY_URL = "extra_repository_url";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView errorTextView;
    private VersionsAdapter adapter;
    private Release currentRelease;
    private String repositoryName;
    private String repositoryUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivityIfAvailable(this); // Aplicar Dynamic Colors
        setContentView(R.layout.activity_versions);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.recyclerViewVersions);
        progressBar = findViewById(R.id.progressBarVersions);
        errorTextView = findViewById(R.id.errorTextViewVersions);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VersionsAdapter(this, this);
        recyclerView.setAdapter(adapter);

        if (getIntent() != null) {
            if (getIntent().hasExtra(EXTRA_RELEASE)) {
                currentRelease = (Release) getIntent().getSerializableExtra(EXTRA_RELEASE);
            }
            
            // Obter informações específicas do repositório
            repositoryName = getIntent().getStringExtra(EXTRA_REPOSITORY_NAME);
            repositoryUrl = getIntent().getStringExtra(EXTRA_REPOSITORY_URL);
            
            if (repositoryName != null && !repositoryName.isEmpty()) {
                setTitle("Versões - " + repositoryName);
                fetchVersionsForSpecificRepository();
            } else if (currentRelease != null) {
                setTitle(currentRelease.getName() != null && !currentRelease.getName().isEmpty() ? currentRelease.getName() : currentRelease.getTagName());
                fetchVersionsForSpecificRepository();
            } else {
                showError("Erro: Nenhuma informação de repositório fornecida.");
            }
        } else {
            showError("Erro: Nenhuma informação fornecida.");
        }
    }

    private void fetchVersionsForSpecificRepository() {
        new FetchVersionsTask().execute();
    }

    @Override
    public void onVersionDownloadClick(WinlatorVersion version) {
        if (version.getDownloadUrl() != null && !version.getDownloadUrl().isEmpty()) {
            Toast.makeText(this, "Iniciando download: " + version.getAssetName(), Toast.LENGTH_SHORT).show();

            Intent serviceIntent = new Intent(this, DownloadService.class);
            serviceIntent.putExtra(DownloadService.EXTRA_ACTION, DownloadService.ACTION_START_DOWNLOAD);
            serviceIntent.putExtra(DownloadService.EXTRA_URL, version.getDownloadUrl());
            serviceIntent.putExtra(DownloadService.EXTRA_FILE_NAME, version.getAssetName());
            startService(serviceIntent);

            Intent activityIntent = new Intent(this, DownloadManagerActivity.class);
            startActivity(activityIntent);
        } else {
            Toast.makeText(this, "URL de download inválida", Toast.LENGTH_SHORT).show();
        }
    }

    private void showError(String message) {
        if (errorTextView != null) {
            errorTextView.setText(message);
            errorTextView.setVisibility(View.VISIBLE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }
        Log.e(TAG, message);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class FetchVersionsTask extends AsyncTask<Void, Void, List<WinlatorVersion>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            errorTextView.setVisibility(View.GONE);
        }

        @Override
        protected List<WinlatorVersion> doInBackground(Void... voids) {
            List<WinlatorVersion> versions = new ArrayList<>();
            
            // Usar a URL do repositório específico para buscar apenas os releases daquele repositório
            String targetUrl = repositoryUrl;
            if (targetUrl == null && currentRelease != null) {
                targetUrl = currentRelease.getHtmlUrl();
            }
            
            if (targetUrl == null) {
                return versions;
            }

            try {
                // Converter URL do GitHub para API URL para buscar todos os releases do repositório específico
                String apiUrl = convertToApiUrl(targetUrl);
                if (apiUrl != null) {
                    versions = fetchAllReleasesFromRepository(apiUrl);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao buscar versões do repositório " + repositoryName, e);
            }
            
            return versions;
        }

        @Override
        protected void onPostExecute(List<WinlatorVersion> result) {
            super.onPostExecute(result);
            progressBar.setVisibility(View.GONE);
            if (result != null && !result.isEmpty()) {
                adapter.updateData(result);
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                showError("Nenhuma versão encontrada para " + (repositoryName != null ? repositoryName : "este repositório") + ".");
            }
        }

        private String convertToApiUrl(String githubUrl) {
            // Converter https://github.com/user/repo/releases para https://api.github.com/repos/user/repo/releases
            if (githubUrl != null && githubUrl.contains("github.com")) {
                Pattern pattern = Pattern.compile("https://github\\.com/([^/]+)/([^/]+)");
                Matcher matcher = pattern.matcher(githubUrl);
                if (matcher.find()) {
                    String user = matcher.group(1);
                    String repo = matcher.group(2);
                    return "https://api.github.com/repos/" + user + "/" + repo + "/releases";
                }
            }
            return null;
        }

        private List<WinlatorVersion> fetchAllReleasesFromRepository(String apiUrl) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            List<WinlatorVersion> versions = new ArrayList<>();
            
            try {
                URL url = new URL(apiUrl);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                urlConnection.setConnectTimeout(15000);
                urlConnection.setReadTimeout(15000);
                urlConnection.connect();

                int responseCode = urlConnection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "HTTP error code: " + responseCode + " for " + apiUrl);
                    return versions;
                }

                StringBuilder buffer = new StringBuilder();
                reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append("\n");
                }

                String jsonString = buffer.toString();
                JSONArray releasesArray = new JSONArray(jsonString);
                
                for (int i = 0; i < releasesArray.length(); i++) {
                    JSONObject releaseJson = releasesArray.getJSONObject(i);
                    
                    String tagName = releaseJson.optString("tag_name", "");
                    String releaseName = releaseJson.optString("name", tagName);
                    String publishedAt = releaseJson.optString("published_at", "");
                    
                    // Buscar todos os assets deste release
                    JSONArray assets = releaseJson.optJSONArray("assets");
                    
                    if (assets != null && assets.length() > 0) {
                        // Adicionar cada asset como uma versão separada
                        for (int j = 0; j < assets.length(); j++) {
                            JSONObject asset = assets.getJSONObject(j);
                            String downloadUrl = asset.optString("browser_download_url", "");
                            String assetName = asset.optString("name", "");
                            long assetSize = asset.optLong("size", 0);
                            
                            if (!downloadUrl.isEmpty() && !assetName.isEmpty()) {
                                String versionName = releaseName + " - " + assetName;
                                versions.add(new WinlatorVersion(versionName, assetName, downloadUrl, assetSize, publishedAt));
                            }
                        }
                    } else {
                        // Se não houver assets, usar o zipball_url
                        String zipballUrl = releaseJson.optString("zipball_url", "");
                        if (!zipballUrl.isEmpty()) {
                            String assetName = releaseName + "-" + tagName + ".zip";
                            versions.add(new WinlatorVersion(releaseName, assetName, zipballUrl, 0, publishedAt));
                        }
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Erro ao buscar releases de " + apiUrl, e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception e) {
                        Log.e(TAG, "Erro ao fechar stream", e);
                    }
                }
            }
            
            return versions;
        }
    }
}


