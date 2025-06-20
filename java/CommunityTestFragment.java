package com.winlator.Download;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.winlator.Download.adapter.CommunityTestAdapter;
import com.winlator.Download.model.CommunityTest;

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

public class CommunityTestFragment extends Fragment {

    private static final String TAG = "CommunityTestFragment";

    private RecyclerView recyclerView;
    private CommunityTestAdapter adapter;
    private List<CommunityTest> testList; // Fragment's own copy of the data
    private ExecutorService executor;
    private FloatingActionButton fabAddTest;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community_tests, container, false);
        Log.d(TAG, "onCreateView called");

        recyclerView = view.findViewById(R.id.recycler_view_community_tests);
        fabAddTest = view.findViewById(R.id.fab_add_community_test);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize list and adapter
        testList = new ArrayList<>();
        adapter = new CommunityTestAdapter(new ArrayList<>(), getContext()); // Pass a new empty list to adapter initially
        recyclerView.setAdapter(adapter);
        Log.d(TAG, "RecyclerView and Adapter initialized. Initial adapter item count: " + adapter.getItemCount());

        executor = Executors.newSingleThreadExecutor();
        setupFabClickListener();
        loadCommunityTests();

        return view;
    }

    // onViewCreated and setupFabClickListener remain the same as the logging version

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");

        SearchView searchView = view.findViewById(R.id.search_view_community_tests);
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
        fabAddTest.setOnClickListener(v -> {
            Log.d(TAG, "FAB clicked to add new test");
            showAddTestDialog();
        });
    }

    // showAddTestDialog remains the same as the logging version
    private void showAddTestDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_community_test, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        TextInputEditText etGameName = dialogView.findViewById(R.id.et_dialog_test_game_name);
        TextInputEditText etYoutubeUrl = dialogView.findViewById(R.id.et_dialog_test_youtube_url);
        TextInputEditText etDescription = dialogView.findViewById(R.id.et_dialog_test_description);
        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_test_cancel);
        Button btnAdd = dialogView.findViewById(R.id.btn_dialog_test_add);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v -> {
            String gameName = etGameName.getText().toString().trim();
            String youtubeUrl = etYoutubeUrl.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (TextUtils.isEmpty(gameName) || TextUtils.isEmpty(youtubeUrl) || TextUtils.isEmpty(description)) {
                Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d(TAG, "Adding new test: " + gameName);
            addCommunityTest(gameName, youtubeUrl, description);
            dialog.dismiss();
        });

        dialog.show();
    }


    private void loadCommunityTests() {
        Log.d(TAG, "loadCommunityTests called");
        executor.execute(() -> {
            try {
                URL url = new URL("https://ldgames.x10.mx/list_community_tests.php");
                Log.d(TAG, "Fetching from URL: " + url.toString());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    String jsonResponse = response.toString();
                    Log.d(TAG, "Raw JSON Response: " + jsonResponse);
                    parseTestsJson(jsonResponse);
                } else {
                    Log.e(TAG, "Error loading community tests. Response code: " + responseCode);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error loading community tests. Code: " + responseCode, Toast.LENGTH_SHORT).show());
                    }
                }
                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Connection error in loadCommunityTests", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Connection error", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void parseTestsJson(String jsonString) {
        Log.d(TAG, "parseTestsJson called with string: " + jsonString);
        final List<CommunityTest> newTestsList = new ArrayList<>(); // Make it final for use in lambda
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            Log.d(TAG, "Parsing JSON array with length: " + jsonArray.length());

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject testObject = jsonArray.getJSONObject(i);
                String gameName = testObject.getString("gameName");
                String youtubeUrl = testObject.getString("youtubeUrl");
                String description = testObject.getString("description");
                CommunityTest test = new CommunityTest(gameName, youtubeUrl, description);
                newTestsList.add(test); // Populate the local final list
                Log.d(TAG, "Parsed test item: " + gameName);
            }
            Log.d(TAG, "Parsed " + newTestsList.size() + " items into newTestsList.");

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Log.d(TAG, "Updating adapter on UI thread.");
                    Collections.reverse(newTestsList);

                    // Update the fragment's list (optional if adapter manages its own separate copy fully)
                    this.testList.clear();
                    this.testList.addAll(newTestsList);
                    Log.d(TAG, "Fragment's testList updated. Size: " + this.testList.size());

                    if (adapter != null) {
                        adapter.setTestList(newTestsList); // Pass the newly fetched and processed list
                        Log.d(TAG, "Called adapter.setTestList(). Adapter item count from adapter.getItemCount(): " + adapter.getItemCount());

                        // Speculative Fix: Re-set the adapter.
                        // This is generally not needed if notifyDataSetChanged() in setTestList works.
                        // recyclerView.setAdapter(adapter);
                        // Log.d(TAG, "Re-set adapter on RecyclerView as a speculative fix.");
                        // Commenting out re-set adapter as it can cause loss of state / scroll position.
                        // Proper use of notifyDataSetChanged() within setTestList is preferred.
                        // If items still don't show, the issue is likely that newTestsList is empty
                        // or the adapter's internal list isn't being populated correctly by setTestList,
                        // or RecyclerView visibility/layout issues.

                    } else {
                        Log.e(TAG, "Adapter is null when trying to update.");
                    }
                });
            } else {
                Log.e(TAG, "getActivity() is null in parseTestsJson. Cannot update UI.");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error processing JSON data in parseTestsJson", e);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error processing data", Toast.LENGTH_SHORT).show());
            }
        }
    }

    // addCommunityTest, onDestroyView, onDestroy remain the same as the logging version
    private void addCommunityTest(String gameName, String youtubeUrl, String description) {
        Log.d(TAG, "addCommunityTest called for: " + gameName);
        executor.execute(() -> {
            try {
                URL url = new URL("https://ldgames.x10.mx/add_community_test.php");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("gameName", gameName);
                jsonParam.put("youtubeUrl", youtubeUrl);
                jsonParam.put("description", description);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "addCommunityTest response code: " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Community test added", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Community test added, reloading tests...");
                            loadCommunityTests();
                        });
                    }
                } else {
                    Log.e(TAG, "Error adding test. Response code: " + responseCode);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error adding test. Code: " + responseCode, Toast.LENGTH_SHORT).show());
                    }
                }
                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Connection error in addCommunityTest", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Connection error while adding test", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
