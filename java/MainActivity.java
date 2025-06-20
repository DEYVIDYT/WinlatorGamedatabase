package com.winlator.Download;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;

    private static final int STORAGE_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivityIfAvailable(this);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);

        // Set up navigation view listener
        navigationView.setNavigationItemSelectedListener(this);

        // Set up ActionBarDrawerToggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        if (savedInstanceState == null) {
            // Display MyGamesFragment by default
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new MyGamesFragment())
                    .commit();
            navigationView.setCheckedItem(R.id.nav_my_games);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("My Games");
            }
        }

        if (!checkStoragePermissions()) {
            requestStoragePermissions();
        } else {
            Log.d("MainActivity", "Storage permissions already granted.");
        }
    }

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

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        Fragment fragment = null;
        String title = getString(R.string.app_name); // Default title

        int id = item.getItemId();

        if (id == R.id.nav_my_games) {
            fragment = new MyGamesFragment();
            title = "My Games";
        } else if (id == R.id.nav_explore) {
            fragment = new ExploreFragment();
            title = "Explore";
        } else if (id == R.id.nav_settings) {
            fragment = new SettingsFragment();
            title = "Settings";
        }

        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.content_frame, fragment);
            fragmentTransaction.commit();

            // Set toolbar title
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
