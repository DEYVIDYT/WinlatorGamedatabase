package com.winlator.Download.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.winlator.Download.R;
import com.winlator.Download.model.Download;

import java.util.ArrayList;
import java.util.List;

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.ViewHolder> {

    private List<Download> downloads;
    private Context context;
    private OnDownloadActionListener actionListener;
    private boolean isSelectionMode = false;

    // Interface para eventos de ação nos downloads
    public interface OnDownloadActionListener {
        void onPauseDownload(Download download);
        void onResumeDownload(Download download);
        void onCancelDownload(Download download);
        void onInstallDownload(Download download);
        void onItemSelected(Download download, boolean isSelected);
        void onLongClick(Download download, int position);
    }

    public DownloadAdapter(Context context, OnDownloadActionListener listener) {
        this.context = context;
        this.downloads = new ArrayList<>();
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_download, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Download download = downloads.get(position);
        
        // Configurar o nome do arquivo
        holder.fileNameTextView.setText(download.getFileName());
        
        // Configurar o tamanho do arquivo
        String fileSize = download.getFormattedDownloadedSize() + " / " + download.getFormattedTotalSize();
        holder.fileSizeTextView.setText(fileSize);
        
        // Configurar o status
        holder.statusTextView.setText(getStatusText(download.getStatus()));
        
        // Configurar o progresso
        holder.progressIndicator.setProgress(download.getProgress());
        
        // Configurar a velocidade (apenas para downloads ativos)
        if (download.getStatus() == Download.STATUS_DOWNLOADING && download.getSpeed() > 0) {
            holder.speedTextView.setText(download.getFormattedSpeed());
            holder.speedTextView.setVisibility(View.VISIBLE);
        } else {
            holder.speedTextView.setVisibility(View.GONE);
        }
        
        // Configurar os botões com base no status
        configureButtons(holder, download);

        // Configurar seleção
        holder.cardView.setChecked(download.isSelected());
        
        // Configurar visibilidade dos botões baseado no modo de seleção
        if (isSelectionMode) {
            holder.buttonsLayout.setVisibility(View.GONE);
        } else {
            holder.buttonsLayout.setVisibility(View.VISIBLE);
        }

        // Configurar clique no item
        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(position);
            }
        });

        // Configurar clique longo no item
        holder.itemView.setOnLongClickListener(v -> {
            if (actionListener != null) {
                actionListener.onLongClick(download, position);
                return true;
            }
            return false;
        });
    }

    private void configureButtons(ViewHolder holder, Download download) {
        // Resetar a visibilidade de todos os botões
        holder.pauseButton.setVisibility(View.GONE);
        holder.resumeButton.setVisibility(View.GONE);
        holder.cancelButton.setVisibility(View.GONE);
        holder.installButton.setVisibility(View.GONE);
        
        switch (download.getStatus()) {
            case Download.STATUS_DOWNLOADING:
                holder.pauseButton.setVisibility(View.VISIBLE);
                holder.cancelButton.setVisibility(View.VISIBLE);
                
                holder.pauseButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onPauseDownload(download);
                    }
                });
                
                holder.cancelButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onCancelDownload(download);
                    }
                });
                break;
                
            case Download.STATUS_PAUSED:
                holder.resumeButton.setVisibility(View.VISIBLE);
                holder.cancelButton.setVisibility(View.VISIBLE);
                
                holder.resumeButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onResumeDownload(download);
                    }
                });
                
                holder.cancelButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onCancelDownload(download);
                    }
                });
                break;
                
            case Download.STATUS_COMPLETED:
                holder.installButton.setVisibility(View.VISIBLE);
                
                holder.installButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onInstallDownload(download);
                    }
                });
                break;
                
            case Download.STATUS_FAILED:
                holder.resumeButton.setVisibility(View.VISIBLE);
                holder.cancelButton.setVisibility(View.VISIBLE);
                
                holder.resumeButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onResumeDownload(download);
                    }
                });
                
                holder.cancelButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onCancelDownload(download);
                    }
                });
                break;
                
            case Download.STATUS_PENDING:
                holder.cancelButton.setVisibility(View.VISIBLE);
                
                holder.cancelButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onCancelDownload(download);
                    }
                });
                break;
        }
    }

    private String getStatusText(int status) {
        switch (status) {
            case Download.STATUS_PENDING:
                return context.getString(R.string.status_pending);
            case Download.STATUS_DOWNLOADING:
                return context.getString(R.string.status_downloading);
            case Download.STATUS_PAUSED:
                return context.getString(R.string.status_paused);
            case Download.STATUS_COMPLETED:
                return context.getString(R.string.status_completed);
            case Download.STATUS_FAILED:
                return context.getString(R.string.status_failed);
            default:
                return "Desconhecido";
        }
    }

    @Override
    public int getItemCount() {
        return downloads != null ? downloads.size() : 0;
    }

    public void updateData(List<Download> newDownloads) {
        this.downloads.clear();
        if (newDownloads != null) {
            this.downloads.addAll(newDownloads);
        }
        notifyDataSetChanged();
    }

    public void updateDownload(Download download) {
        for (int i = 0; i < downloads.size(); i++) {
            if (downloads.get(i).getId() == download.getId()) {
                downloads.set(i, download);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void updateDownloadProgress(long downloadId, int progress, long downloadedBytes, long totalBytes, double speed) {
        for (int i = 0; i < downloads.size(); i++) {
            Download download = downloads.get(i);
            if (download.getId() == downloadId) {
                // Criar um novo objeto Download com os valores atualizados
                Download updatedDownload = new Download(
                    download.getId(),
                    download.getUrl(),
                    download.getFileName(),
                    download.getLocalPath(),
                    totalBytes > 0 ? totalBytes : download.getTotalBytes(),
                    downloadedBytes,
                    download.getStatus(),
                    download.getTimestamp()
                );
                updatedDownload.setSpeed(speed);
                updatedDownload.setSelected(download.isSelected());
                
                downloads.set(i, updatedDownload);
                notifyItemChanged(i);
                break;
            }
        }
    }

    // Métodos para gerenciar seleção
    public void setSelectionMode(boolean selectionMode) {
        if (this.isSelectionMode != selectionMode) {
            this.isSelectionMode = selectionMode;
            if (!selectionMode) {
                clearSelections();
            }
            notifyDataSetChanged();
        }
    }

    public boolean isInSelectionMode() {
        return isSelectionMode;
    }

    public void toggleSelection(int position) {
        if (position >= 0 && position < downloads.size()) {
            Download download = downloads.get(position);
            download.setSelected(!download.isSelected());
            notifyItemChanged(position);
            
            if (actionListener != null) {
                actionListener.onItemSelected(download, download.isSelected());
            }
        }
    }

    public void selectAll() {
        for (int i = 0; i < downloads.size(); i++) {
            Download download = downloads.get(i);
            if (!download.isSelected()) {
                download.setSelected(true);
                notifyItemChanged(i);
                
                if (actionListener != null) {
                    actionListener.onItemSelected(download, true);
                }
            }
        }
    }

    public void clearSelections() {
        for (int i = 0; i < downloads.size(); i++) {
            Download download = downloads.get(i);
            if (download.isSelected()) {
                download.setSelected(false);
                notifyItemChanged(i);
            }
        }
    }

    public List<Download> getSelectedDownloads() {
        List<Download> selectedDownloads = new ArrayList<>();
        for (Download download : downloads) {
            if (download.isSelected()) {
                selectedDownloads.add(download);
            }
        }
        return selectedDownloads;
    }

    public int getSelectedCount() {
        int count = 0;
        for (Download download : downloads) {
            if (download.isSelected()) {
                count++;
            }
        }
        return count;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView fileNameTextView;
        TextView fileSizeTextView;
        TextView statusTextView;
        TextView speedTextView;
        LinearProgressIndicator progressIndicator;
        Button pauseButton;
        Button resumeButton;
        Button cancelButton;
        Button installButton;
        View buttonsLayout;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            fileNameTextView = itemView.findViewById(R.id.textViewFileName);
            fileSizeTextView = itemView.findViewById(R.id.textViewFileSize);
            statusTextView = itemView.findViewById(R.id.textViewStatus);
            speedTextView = itemView.findViewById(R.id.textViewSpeed);
            progressIndicator = itemView.findViewById(R.id.progressIndicator);
            pauseButton = itemView.findViewById(R.id.buttonPause);
            resumeButton = itemView.findViewById(R.id.buttonResume);
            cancelButton = itemView.findViewById(R.id.buttonCancel);
            installButton = itemView.findViewById(R.id.buttonInstall);
            buttonsLayout = itemView.findViewById(R.id.layoutButtons);
        }
    }
}

