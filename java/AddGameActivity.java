package com.winlator.Download;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;
import com.winlator.Download.model.GameEntry;
import com.winlator.Download.db.GamesDatabaseHelper;
import com.winlator.Download.utils.FilePathUtil; // Import the new utility

    private TextInputEditText etGameTitle;
    private Button btnSelectDesktopFile;
    private TextView tvSelectedDesktopFile;
    private ImageView ivBannerPreview;
    private Button btnSelectBannerImage;
    private TextView tvSelectedBannerImage;
    private Button btnSaveGame;

    private String realDesktopPath; // Changed from Uri to String
    private String realBannerPath;  // Changed from Uri to String

    // ActivityResultLaunchers for file pickers
    private ActivityResultLauncher<Intent> desktopFilePickerLauncher;
    private ActivityResultLauncher<Intent> bannerImagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_game);

        etGameTitle = findViewById(R.id.et_game_title);
        btnSelectDesktopFile = findViewById(R.id.btn_select_desktop_file);
        tvSelectedDesktopFile = findViewById(R.id.tv_selected_desktop_file);
        ivBannerPreview = findViewById(R.id.iv_banner_preview);
        btnSelectBannerImage = findViewById(R.id.btn_select_banner_image);
        tvSelectedBannerImage = findViewById(R.id.tv_selected_banner_image);
        btnSaveGame = findViewById(R.id.btn_save_game);

        setupLaunchers();

        btnSelectDesktopFile.setOnClickListener(v -> openDesktopFilePicker());
        btnSelectBannerImage.setOnClickListener(v -> openBannerImagePicker());
        btnSaveGame.setOnClickListener(v -> saveGame());
    }

    private void setupLaunchers() {
        desktopFilePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        realDesktopPath = FilePathUtil.getPathFromUri(this, uri);
                        if (realDesktopPath != null) {
                            tvSelectedDesktopFile.setText(realDesktopPath);
                            String fileName = PathUtils.getFileName(this, uri); // Use original URI for filename
                            if (fileName != null) {
                                int dotIndex = fileName.lastIndexOf('.');
                                if (dotIndex > 0) {
                                    etGameTitle.setText(fileName.substring(0, dotIndex));
                                } else {
                                    etGameTitle.setText(fileName);
                                }
                            }
                        } else {
                            tvSelectedDesktopFile.setText("Error: Could not get file path.");
                            Toast.makeText(this, "Could not get file path. Please select a file from local storage or a different file manager.", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });

        bannerImagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        realBannerPath = FilePathUtil.getPathFromUri(this, uri);
                        if (realBannerPath != null) {
                            ivBannerPreview.setImageURI(Uri.fromFile(new java.io.File(realBannerPath))); //setImageURI needs a file URI for local paths
                            tvSelectedBannerImage.setText(realBannerPath);
                        } else {
                            // If path conversion fails, try setting URI directly (might work for some URIs in ImageView)
                            ivBannerPreview.setImageURI(uri);
                            tvSelectedBannerImage.setText("Warning: Could not get direct file path. Displaying from URI.");
                            // Store the original URI string if path conversion fails but we want to save something.
                            // However, for consistency and if Winlator *needs* a path, this might be an issue.
                            // For now, we prioritize real path. If it's null, banner path will be null.
                            realBannerPath = null; // Explicitly set to null if path conversion failed
                             Toast.makeText(this, "Could not get banner file path. Preview might work, but saving might use URI or fail if path is strictly needed.", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });
    }

    private void openDesktopFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // MIME type for .desktop files can be tricky.
        // You might want to add multiple types if known: intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/x-desktop", "text/plain"});
        desktopFilePickerLauncher.launch(intent);
    }

    private void openBannerImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT); // ACTION_PICK is also an option
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        bannerImagePickerLauncher.launch(intent);
    }

    private void saveGame() {
        String title = etGameTitle.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            etGameTitle.setError("Title cannot be empty");
            etGameTitle.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(realDesktopPath)) {
            Toast.makeText(this, "Please select a .desktop file and ensure its path can be resolved.", Toast.LENGTH_LONG).show();
            return;
        }

        // realBannerPath can be null or empty if not selected or path couldn't be resolved.
        String finalBannerPath = (realBannerPath != null) ? realBannerPath : "";


        // Create GameEntry object
        GameEntry newGame = new GameEntry(0, title, realDesktopPath, finalBannerPath); // ID will be auto-generated

        // Get instance of GamesDatabaseHelper
        GamesDatabaseHelper dbHelper = new GamesDatabaseHelper(this);

        // Call addGame()
        long newRowId = dbHelper.addGame(newGame);

        if (newRowId != -1) {
            Toast.makeText(this, "Game saved successfully! Path: " + realDesktopPath, Toast.LENGTH_LONG).show();
            finish(); // Close activity after saving
        } else {
            Toast.makeText(this, "Error saving game.", Toast.LENGTH_SHORT).show();
        }
    }
}

// Utility class (can be moved to a separate file if needed)
class PathUtils {
    public static String getFileName(android.content.Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                         result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}
