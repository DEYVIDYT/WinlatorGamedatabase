package com.winlator.Download;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.DynamicColors;
import com.winlator.Download.db.GamesDatabaseHelper;
import com.winlator.Download.model.GameEntry;
import com.winlator.Download.utils.FilePathUtil;

import java.io.File;

public class AddGameActivity extends AppCompatActivity {

    private EditText etGameTitle;
    private Button btnSelectDesktopFile;
    private TextView tvSelectedDesktopFile;
    private ImageView ivBannerPreview;
    private Button btnSelectBannerImage;
    private TextView tvSelectedBannerImage;
    private Button btnSaveGame;

    private String realDesktopPath;
    private String realBannerPath;

    private ActivityResultLauncher<Intent> desktopFilePickerLauncher;
    private ActivityResultLauncher<Intent> bannerImagePickerLauncher;

    private GamesDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivityIfAvailable(this);
        setContentView(R.layout.activity_add_game);

        dbHelper = new GamesDatabaseHelper(this);

        etGameTitle = findViewById(R.id.et_game_title);
        btnSelectDesktopFile = findViewById(R.id.btn_select_desktop_file);
        tvSelectedDesktopFile = findViewById(R.id.tv_selected_desktop_file);
        ivBannerPreview = findViewById(R.id.iv_banner_preview);
        btnSelectBannerImage = findViewById(R.id.btn_select_banner_image);
        tvSelectedBannerImage = findViewById(R.id.tv_selected_banner_image);
        btnSaveGame = findViewById(R.id.btn_save_game);

        desktopFilePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            realDesktopPath = FilePathUtil.getPathFromUri(this, uri);
                            if (realDesktopPath != null) {
                                tvSelectedDesktopFile.setText(realDesktopPath);
                                File desktopFile = new File(realDesktopPath);
                                String fileName = desktopFile.getName();
                                int lastDot = fileName.lastIndexOf('.');
                                if (lastDot > 0) {
                                    etGameTitle.setText(fileName.substring(0, lastDot));
                                } else {
                                    etGameTitle.setText(fileName);
                                }
                            } else {
                                tvSelectedDesktopFile.setText("Could not resolve file path.");
                                Toast.makeText(this, "Could not get file path for .desktop file.", Toast.LENGTH_SHORT).show();
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
                                tvSelectedBannerImage.setText(realBannerPath);
                                ivBannerPreview.setImageURI(Uri.fromFile(new File(realBannerPath)));
                            } else {
                                tvSelectedBannerImage.setText("Could not resolve image path.");
                                Toast.makeText(this, "Could not get file path for banner image.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

        btnSelectDesktopFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            try {
                desktopFilePickerLauncher.launch(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Error launching file picker: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        btnSelectBannerImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            try {
                bannerImagePickerLauncher.launch(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Error launching image picker: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        btnSaveGame.setOnClickListener(v -> saveGame());
    }

    private void saveGame() {
        String title = etGameTitle.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "Game title is required.", Toast.LENGTH_SHORT).show();
            etGameTitle.setError("Title required");
            return;
        }

        if (TextUtils.isEmpty(realDesktopPath)) {
            Toast.makeText(this, ".desktop file path is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        String bannerPathToSave = TextUtils.isEmpty(realBannerPath) ? null : realBannerPath;

        GameEntry newGame = new GameEntry(0, title, realDesktopPath, bannerPathToSave);
        long id = dbHelper.addGame(newGame);

        if (id != -1) {
            Toast.makeText(this, "Game saved successfully!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error saving game.", Toast.LENGTH_SHORT).show();
        }
    }
}
