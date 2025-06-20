package com.winlator.Download;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.Download.adapter.ReleasesAdapter;
import com.winlator.Download.model.Release;
import com.winlator.Download.service.DownloadService;

import java.util.ArrayList;
import java.util.List;

public class ReleasesFragment extends Fragment implements ReleasesAdapter.OnReleaseClickListener {

    private static final String TAG = "ReleasesFragment";
    private static final String ARG_CATEGORY = "category";
    private static final String ARG_RELEASES_LIST = "releases_list";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView errorTextView;
    private ReleasesAdapter adapter;
    private Context mContext;
    private String category;
    private List<Release> releasesList;

    public static ReleasesFragment newInstance(String category, List<Release> releases) {
        ReleasesFragment fragment = new ReleasesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY, category);
        args.putSerializable(ARG_RELEASES_LIST, new ArrayList<>(releases));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            category = getArguments().getString(ARG_CATEGORY);
            releasesList = (List<Release>) getArguments().getSerializable(ARG_RELEASES_LIST);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_releases_fragment, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewReleases);
        progressBar = view.findViewById(R.id.progressBar);
        errorTextView = view.findViewById(R.id.errorTextView);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        if (mContext != null) {
            adapter = new ReleasesAdapter(mContext, this);
            recyclerView.setAdapter(adapter);
        } else {
            Log.e(TAG, "mContext is null in onCreateView, cannot initialize adapter.");
        }
        if (getActivity() != null) {
            getActivity().setTitle(category);
        }
        displayReleases();
        return view;
    }

    private void displayReleases() {
        if (releasesList != null && !releasesList.isEmpty()) {
            if (adapter != null) {
                 adapter.updateData(releasesList);
            }
            if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            if (errorTextView != null) errorTextView.setVisibility(View.GONE);
        } else {
            showError("Nenhum release encontrado para esta categoria.");
        }
    }

    private void showError(String message) {
        if (errorTextView != null) {
            errorTextView.setText(message);
            errorTextView.setVisibility(View.VISIBLE);
        }
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        Log.e(TAG, message);
    }

    @Override
    public void onReleaseDownloadClick(Release release) {
        if (mContext == null) {
            Log.e(TAG, "Context is null in onReleaseDownloadClick. Cannot start download.");
            Toast.makeText(getActivity(), "Error: Context not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        String downloadUrl = release.getDownloadUrl();
        String assetName = release.getAssetName();

        if (downloadUrl != null && !downloadUrl.isEmpty()) {
            Toast.makeText(mContext, "Iniciando download: " + assetName, Toast.LENGTH_SHORT).show();
            Intent serviceIntent = new Intent(mContext, DownloadService.class);

            if (downloadUrl.contains("gofile.io/d/") || downloadUrl.contains("gofile.io/download/")) {
                Log.d(TAG, "Gofile URL detected: " + downloadUrl);
                serviceIntent.putExtra(DownloadService.EXTRA_ACTION, DownloadService.ACTION_RESOLVE_AND_START_GOFILE_DOWNLOAD);
                serviceIntent.putExtra(DownloadService.EXTRA_GOFILE_URL, downloadUrl);
                serviceIntent.putExtra(DownloadService.EXTRA_FILE_NAME, assetName);
            } else if (downloadUrl.contains("www.mediafire.com/file/")) {
                Log.d(TAG, "MediaFire URL detected: " + downloadUrl);
                serviceIntent.putExtra(DownloadService.EXTRA_ACTION, DownloadService.ACTION_RESOLVE_AND_START_MEDIAFIRE_DOWNLOAD);
                serviceIntent.putExtra(DownloadService.EXTRA_MEDIAFIRE_URL, downloadUrl);
                serviceIntent.putExtra(DownloadService.EXTRA_FILE_NAME, assetName);
            } else if (downloadUrl.contains("drive.google.com")) { // Added Google Drive check
                Log.d(TAG, "Google Drive URL detected: " + downloadUrl);
                serviceIntent.putExtra(DownloadService.EXTRA_ACTION, DownloadService.ACTION_RESOLVE_AND_START_GOOGLE_DRIVE_DOWNLOAD);
                serviceIntent.putExtra(DownloadService.EXTRA_GOOGLE_DRIVE_URL, downloadUrl);
                serviceIntent.putExtra(DownloadService.EXTRA_FILE_NAME, assetName); // Placeholder
            } else {
                Log.d(TAG, "Standard URL detected: " + downloadUrl);
                serviceIntent.putExtra(DownloadService.EXTRA_ACTION, DownloadService.ACTION_START_DOWNLOAD);
                serviceIntent.putExtra(DownloadService.EXTRA_URL, downloadUrl);
                serviceIntent.putExtra(DownloadService.EXTRA_FILE_NAME, assetName);
            }

            mContext.startService(serviceIntent);
            
            Intent activityIntent = new Intent(mContext, DownloadManagerActivity.class);
            startActivity(activityIntent);
        } else {
            Toast.makeText(mContext, "URL de download inválida", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onReleaseItemClick(Release release) {
        // ... (remains the same)
        if (mContext != null) {
            Intent intent = new Intent(mContext, VersionsActivity.class);
            intent.putExtra(VersionsActivity.EXTRA_RELEASE, release);
            intent.putExtra(VersionsActivity.EXTRA_REPOSITORY_NAME, release.getName());
            intent.putExtra(VersionsActivity.EXTRA_REPOSITORY_URL, release.getHtmlUrl());
            startActivity(intent);
        } else {
            Log.e(TAG, "mContext is null in onReleaseItemClick. Cannot start VersionsActivity.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
    }
}
