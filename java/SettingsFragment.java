package com.winlator.Download;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.winlator.Download.utils.AppSettings;

public class SettingsFragment extends Fragment {

    private TextView tvAppVersion;
    private MaterialButton btnSelectDownloadFolder;
    private TextView tvSelectedDownloadFolder;
    private SwitchMaterial switchDirectCommunityDownloads;
    private MaterialButton btnLinkCryptorTool;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        loadSettings();
        setupClickListeners();
        displayAppVersion();
    }

    private void initViews(View view) {
        tvAppVersion = view.findViewById(R.id.tv_app_version);
        btnSelectDownloadFolder = view.findViewById(R.id.btn_select_download_folder);
        tvSelectedDownloadFolder = view.findViewById(R.id.tv_selected_download_folder);
        switchDirectCommunityDownloads = view.findViewById(R.id.switch_direct_community_downloads);
        btnLinkCryptorTool = view.findViewById(R.id.btn_link_cryptor_tool);
    }

    private void loadSettings() {
        // Use requireContext() for non-nullable Context, assuming fragment is attached.
        tvSelectedDownloadFolder.setText(AppSettings.getDownloadPath(requireContext()));
        switchDirectCommunityDownloads.setChecked(AppSettings.getDisableDirectDownloads(requireContext()));
    }

    private void setupClickListeners() {
        btnSelectDownloadFolder.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Set Download Path");

            final EditText input = new EditText(requireContext());
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            String currentPath = AppSettings.getDownloadPath(requireContext());
            if (currentPath.equals(AppSettings.DEFAULT_DOWNLOAD_PATH)) {
                input.setText(""); // Show placeholder or empty if it's the default
                input.setHint("Default: " + AppSettings.DEFAULT_DOWNLOAD_PATH);
            } else {
                input.setText(currentPath);
            }
            builder.setView(input);

            builder.setPositiveButton("OK", (dialog, which) -> {
                String path = input.getText().toString().trim();
                if (path.isEmpty()) {
                    path = AppSettings.DEFAULT_DOWNLOAD_PATH;
                }
                AppSettings.setDownloadPath(requireContext(), path);
                tvSelectedDownloadFolder.setText(path);
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        });

        switchDirectCommunityDownloads.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppSettings.setDisableDirectDownloads(requireContext(), isChecked);
        });

        btnLinkCryptorTool.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), LinkCryptorActivity.class);
            startActivity(intent);
        });
    }

    private void displayAppVersion() {
        try {
            PackageInfo pInfo = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0);
            String version = pInfo.versionName;
            tvAppVersion.setText("App Version: " + version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            tvAppVersion.setText("App Version: Not available");
        }
    }
}
