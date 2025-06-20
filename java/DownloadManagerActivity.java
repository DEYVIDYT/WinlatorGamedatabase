package com.winlator.Download;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.winlator.Download.adapter.DownloadAdapter;
import com.winlator.Download.model.Download;
import com.winlator.Download.service.DownloadService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadManagerActivity extends AppCompatActivity implements DownloadAdapter.OnDownloadActionListener {

    private RecyclerView recyclerView;
    private DownloadAdapter adapter;
    private TextView noDownloadsTextView;
    private FloatingActionButton fabClearCompleted;
    private DownloadService downloadService;
    private boolean isBound = false;
    private Handler handler;
    private Runnable refreshRunnable;
    private ActionMode actionMode;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DownloadService.DownloadBinder binder = (DownloadService.DownloadBinder) service;
            downloadService = binder.getService();
            isBound = true;
            loadDownloads();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            downloadService = null;
        }
    };

    private final BroadcastReceiver downloadProgressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DownloadService.ACTION_DOWNLOAD_PROGRESS.equals(intent.getAction())) {
                long downloadId = intent.getLongExtra(DownloadService.EXTRA_DOWNLOAD_ID, -1);
                int progress = intent.getIntExtra("progress", 0);
                long downloadedBytes = intent.getLongExtra("downloadedBytes", 0);
                long totalBytes = intent.getLongExtra("totalBytes", 0);
                double speed = intent.getDoubleExtra("speed", 0);
                
                if (adapter != null) {
                    adapter.updateDownloadProgress(downloadId, progress, downloadedBytes, totalBytes, speed);
                }
            } else if (DownloadService.ACTION_DOWNLOAD_STATUS_CHANGED.equals(intent.getAction())) {
                loadDownloads();
            }
        }
    };

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.download_action_mode_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete_selected:
                    deleteSelectedDownloads();
                    return true;
                case R.id.action_select_all:
                    selectAllDownloads();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            adapter.setSelectionMode(false);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Aplicar Dynamic Colors
        DynamicColors.applyToActivityIfAvailable(this);
        setContentView(R.layout.activity_download_manager);

        // Configurar a toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar_download_manager);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.download_manager_title);

        // Inicializar views
        recyclerView = findViewById(R.id.recyclerViewDownloads);
        noDownloadsTextView = findViewById(R.id.textViewNoDownloads);
        fabClearCompleted = findViewById(R.id.fabClearCompleted);

        // Configurar RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DownloadAdapter(this, this);
        recyclerView.setAdapter(adapter);

        // Configurar o botão de limpar downloads concluídos
        fabClearCompleted.setOnClickListener(v -> showClearCompletedDialog());

        // Inicializar o handler para atualizações periódicas
        handler = new Handler(Looper.getMainLooper());
        // refreshRunnable = this::loadDownloads;

        // Registrar o receiver para atualizações de progresso
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadService.ACTION_DOWNLOAD_PROGRESS);
        filter.addAction(DownloadService.ACTION_DOWNLOAD_STATUS_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(downloadProgressReceiver, filter);

        // Vincular ao serviço de download
        Intent serviceIntent = new Intent(this, DownloadService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // if (isBound) {
        //     loadDownloads();
        // }
        // Iniciar atualizações periódicas
        // handler.postDelayed(refreshRunnable, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Parar atualizações periódicas
        handler.removeCallbacks(refreshRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Desregistrar o receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(downloadProgressReceiver);
        
        // Desvincular do serviço
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadDownloads() {
        if (isBound && downloadService != null) {
            List<Download> downloads = downloadService.getAllDownloads();
            adapter.updateData(downloads);
            
            // Atualizar visibilidade do texto "Nenhum download"
            if (downloads.isEmpty()) {
                noDownloadsTextView.setVisibility(View.VISIBLE);
                fabClearCompleted.setVisibility(View.GONE);
            } else {
                noDownloadsTextView.setVisibility(View.GONE);
                
                // Verificar se há downloads concluídos para mostrar o botão de limpar
                boolean hasCompleted = false;
                for (Download download : downloads) {
                    if (download.getStatus() == Download.STATUS_COMPLETED) {
                        hasCompleted = true;
                        break;
                    }
                }
                fabClearCompleted.setVisibility(hasCompleted ? View.VISIBLE : View.GONE);
            }
            
            // Agendar próxima atualização
            // handler.removeCallbacks(refreshRunnable);
            // handler.postDelayed(refreshRunnable, 1000);
        }
    }

    @Override
    public void onPauseDownload(Download download) {
        if (isBound && downloadService != null) {
            downloadService.handlePauseDownload(download.getId());
        }
    }

    @Override
    public void onResumeDownload(Download download) {
        if (isBound && downloadService != null) {
            downloadService.handleResumeDownload(download.getId());
        }
    }

    @Override
    public void onCancelDownload(Download download) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cancel_download)
                .setMessage(R.string.confirm_cancel_download)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    if (isBound && downloadService != null) {
                        downloadService.handleCancelDownload(download.getId());
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    @Override
    public void onInstallDownload(Download download) {
        File file = new File(download.getLocalPath());
        if (file.exists()) {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        }
    }

    @Override
    public void onItemSelected(Download download, boolean isSelected) {
        int selectedCount = adapter.getSelectedCount();
        
        if (selectedCount == 0 && actionMode != null) {
            actionMode.finish();
        } else if (selectedCount > 0 && actionMode != null) {
            actionMode.setTitle(getString(R.string.selection_count, selectedCount));
        }
    }

    @Override
    public void onLongClick(Download download, int position) {
        if (actionMode == null) {
            actionMode = startActionMode(actionModeCallback);
            adapter.setSelectionMode(true);
            adapter.toggleSelection(position);
            actionMode.setTitle(getString(R.string.selection_count, 1));
        } else {
            // Se o modo de seleção já estiver ativo, apenas alterna a seleção do item
            adapter.toggleSelection(position);
        }
    }

    private void showClearCompletedDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.clear_completed_downloads)
                .setMessage(R.string.confirm_clear_completed)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    if (isBound && downloadService != null) {
                        downloadService.clearCompletedDownloads();
                        loadDownloads();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void deleteSelectedDownloads() {
        List<Download> selectedDownloads = adapter.getSelectedDownloads();
        if (!selectedDownloads.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.confirm_delete_selected_title)
                    .setMessage(getString(R.string.confirm_delete_selected_message, selectedDownloads.size()))
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        if (isBound && downloadService != null) {
                            List<Long> downloadIds = new ArrayList<>();
                            for (Download download : selectedDownloads) {
                                downloadIds.add(download.getId());
                            }
                            downloadService.deleteDownloads(downloadIds);
                            if (actionMode != null) {
                                actionMode.finish();
                            }
                            loadDownloads();
                        }
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        }
    }

    private void selectAllDownloads() {
        adapter.selectAll();
        if (actionMode != null) {
            actionMode.setTitle(getString(R.string.selection_count, adapter.getSelectedCount()));
        }
    }
}

