package com.winlator.Download;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.winlator.Download.adapter.CommunityFixAdapter;
import com.winlator.Download.model.CommunityFix;
import com.winlator.Download.service.DownloadService; // Assuming DownloadService can handle generic downloads

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommunityFixFragment extends Fragment {

    private RecyclerView recyclerView;
    private CommunityFixAdapter adapter;
    private List<CommunityFix> fixList;
    private ExecutorService executor;
    private FloatingActionButton fabAddFix;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community_fixes, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_community_fixes);
        fabAddFix = view.findViewById(R.id.fab_add_community_fix);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        fixList = new ArrayList<>();
        adapter = new CommunityFixAdapter(fixList, getContext());
        recyclerView.setAdapter(adapter);

        executor = Executors.newSingleThreadExecutor();

        setupFabClickListener();
        loadCommunityFixes();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SearchView searchView = view.findViewById(R.id.search_view_community_fixes);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) {
                    adapter.getFilter().filter(newText);
                }
                return false;
            }
        });
    }

    private void setupFabClickListener() {
        fabAddFix.setOnClickListener(v -> showAddFixDialog());
    }

    private void showAddFixDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_community_fix, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        TextInputEditText etFixName = dialogView.findViewById(R.id.et_dialog_fix_name);
        TextInputEditText etDescription = dialogView.findViewById(R.id.et_dialog_fix_description);
        TextInputEditText etDownloadUrl = dialogView.findViewById(R.id.et_dialog_fix_download_url);
        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_fix_cancel);
        Button btnAdd = dialogView.findViewById(R.id.btn_dialog_fix_add);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v -> {
            String fixName = etFixName.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String downloadUrl = etDownloadUrl.getText().toString().trim();

            if (TextUtils.isEmpty(fixName) || TextUtils.isEmpty(description) || TextUtils.isEmpty(downloadUrl)) {
                Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }
            // Basic URL validation
            try {
                new URL(downloadUrl);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Invalid Download URL", Toast.LENGTH_SHORT).show();
                return;
            }

            addCommunityFix(fixName, description, downloadUrl);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void loadCommunityFixes() {
        executor.execute(() -> {
            try {
                URL url = new URL("https://ldgames.x10.mx/list_community_fixes.php"); // Update with your actual URL
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    parseFixesJson(response.toString());
                } else {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error loading community fixes", Toast.LENGTH_SHORT).show());
                    }
                }
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Connection error", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void parseFixesJson(String jsonString) {
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            List<CommunityFix> newFixesList = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject fixObject = jsonArray.getJSONObject(i);
                String fixName = fixObject.getString("fixName");
                String description = fixObject.getString("description");
                String downloadUrl = fixObject.getString("downloadUrl");
                CommunityFix fix = new CommunityFix(fixName, description, downloadUrl);
                newFixesList.add(fix);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Collections.reverse(newFixesList);
                    fixList.clear();
                    fixList.addAll(newFixesList);
                    adapter.notifyDataSetChanged();
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error processing data", Toast.LENGTH_SHORT).show());
            }
        }
    }

    private void addCommunityFix(String fixName, String description, String downloadUrl) {
        executor.execute(() -> {
            try {
                URL url = new URL("https://ldgames.x10.mx/add_community_fix.php"); // Update with your actual URL
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("fixName", fixName);
                jsonParam.put("description", description);
                jsonParam.put("downloadUrl", downloadUrl);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Community fix added", Toast.LENGTH_SHORT).show();
                            loadCommunityFixes(); // Refresh list
                        });
                    }
                } else {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error adding fix", Toast.LENGTH_SHORT).show());
                    }
                }
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Connection error", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
