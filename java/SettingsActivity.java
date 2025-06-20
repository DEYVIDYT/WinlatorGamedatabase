package com.winlator.Download;

import android.content.Intent;
// SharedPreferences import removed as it's now encapsulated in AppSettings
import android.os.Bundle;
import android.text.InputType;
// Button import will be replaced by MaterialButton if not used by other elements
import android.widget.EditText;
import android.widget.TextView;
// import android.widget.Toast; // Not strictly needed if no toasts are shown

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton; // Added import
import com.google.android.material.color.DynamicColors;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.winlator.Download.utils.AppSettings; // Added import

public class SettingsActivity extends AppCompatActivity {

    // SharedPreferences keys are now in AppSettings.java

    // private Button btnConfigureUploadApi; // Removed
    private MaterialButton btnLinkCryptorTool; // Changed type
    private MaterialButton btn_select_download_folder; // Changed type
    private TextView tv_selected_download_folder;
    private SwitchMaterial switch_direct_community_downloads;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivityIfAvailable(this);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Configurações");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();
        loadSettings(); // Load settings after initializing views
        setupClickListeners();
    }

    private void initViews() {
        // btnConfigureUploadApi = findViewById(R.id.btn_configure_upload_api); // Remains Removed
        btnLinkCryptorTool = findViewById(R.id.btn_link_cryptor_tool);
        btn_select_download_folder = findViewById(R.id.btn_select_download_folder);
        tv_selected_download_folder = findViewById(R.id.tv_selected_download_folder);
        switch_direct_community_downloads = findViewById(R.id.switch_direct_community_downloads);
    }

    private void loadSettings() {
        // Use AppSettings to get values
        String downloadPath = AppSettings.getDownloadPath(this);
        tv_selected_download_folder.setText(downloadPath);
        boolean disableDirectDownloads = AppSettings.getDisableDirectDownloads(this);
        switch_direct_community_downloads.setChecked(disableDirectDownloads);
    }

    private void setupClickListeners() {
        // if (btnConfigureUploadApi != null) { // Remains Removed
            // btnConfigureUploadApi.setOnClickListener(v -> { // Remains Removed
                // Intent intent = new Intent(SettingsActivity.this, UploadApiSettingsHostActivity.class); // Remains Removed
                // startActivity(intent); // Remains Removed
            // }); // Remains Removed
        // } // Remains Removed
        if (btnLinkCryptorTool != null) {
            btnLinkCryptorTool.setOnClickListener(v -> {
                Intent intent = new Intent(SettingsActivity.this, LinkCryptorActivity.class);
                startActivity(intent);
            });
        }

        if (btn_select_download_folder != null) {
            btn_select_download_folder.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Set Download Path");

                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                // Pre-fill EditText: if current path is the default, show empty, otherwise show current path.
                String currentPath = AppSettings.getDownloadPath(this);
                if (currentPath.equals(AppSettings.DEFAULT_DOWNLOAD_PATH)) {
                    input.setText("");
                } else {
                    input.setText(currentPath);
                }
                builder.setView(input);

                builder.setPositiveButton("OK", (dialog, which) -> {
                    String path = input.getText().toString().trim();
                    if (path.isEmpty()) {
                        // If user clears input, reset to the defined default path
                        path = AppSettings.DEFAULT_DOWNLOAD_PATH;
                    }
                    AppSettings.setDownloadPath(this, path); // Use AppSettings to set value
                    tv_selected_download_folder.setText(path);
                });
                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
                builder.show();
            });
        }

        if (switch_direct_community_downloads != null) {
            switch_direct_community_downloads.setOnCheckedChangeListener((buttonView, isChecked) -> {
                AppSettings.setDisableDirectDownloads(this, isChecked); // Use AppSettings to set value
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
