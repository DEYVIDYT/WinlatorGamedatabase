package com.winlator.Download.adapter;

import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.Download.R;
import com.winlator.Download.model.WinlatorVersion;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VersionsAdapter extends RecyclerView.Adapter<VersionsAdapter.ViewHolder> {

    private List<WinlatorVersion> versions;
    private Context context;
    private OnVersionClickListener versionClickListener;

    public interface OnVersionClickListener {
        void onVersionDownloadClick(WinlatorVersion version);
    }

    public VersionsAdapter(Context context, OnVersionClickListener listener) {
        this.context = context;
        this.versions = new ArrayList<>();
        this.versionClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_version, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WinlatorVersion version = versions.get(position);
        holder.versionNameTextView.setText(version.getName());
        
        // Exibir informações adicionais se disponíveis
        StringBuilder details = new StringBuilder();
        
        if (version.getAssetSize() > 0) {
            String formattedSize = Formatter.formatFileSize(context, version.getAssetSize());
            details.append("Tamanho: ").append(formattedSize);
        }
        
        if (version.getPublishedAt() != null && !version.getPublishedAt().isEmpty()) {
            String formattedDate = formatDate(version.getPublishedAt());
            if (details.length() > 0) {
                details.append(" • ");
            }
            details.append("Publicado: ").append(formattedDate);
        }
        
        if (details.length() > 0) {
            holder.versionDetailsTextView.setText(details.toString());
            holder.versionDetailsTextView.setVisibility(View.VISIBLE);
        } else {
            holder.versionDetailsTextView.setVisibility(View.GONE);
        }

        holder.downloadButton.setOnClickListener(v -> {
            if (versionClickListener != null) {
                versionClickListener.onVersionDownloadClick(version);
            }
        });
    }

    @Override
    public int getItemCount() {
        return versions != null ? versions.size() : 0;
    }

    public void updateData(List<WinlatorVersion> newVersions) {
        this.versions.clear();
        if (newVersions != null) {
            this.versions.addAll(newVersions);
        }
        notifyDataSetChanged();
    }

    private String formatDate(String isoDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = inputFormat.parse(isoDate);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return isoDate; // Retorna a data original se não conseguir fazer o parse
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView versionNameTextView;
        TextView versionDetailsTextView;
        Button downloadButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            versionNameTextView = itemView.findViewById(R.id.textViewVersionName);
            versionDetailsTextView = itemView.findViewById(R.id.textViewVersionDetails);
            downloadButton = itemView.findViewById(R.id.buttonDownload);
        }
    }
}

