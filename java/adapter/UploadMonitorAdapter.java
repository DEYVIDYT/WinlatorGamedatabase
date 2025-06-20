package com.winlator.Download.adapter;

import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.Download.R;
import com.winlator.Download.model.UploadStatus;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UploadMonitorAdapter extends RecyclerView.Adapter<UploadMonitorAdapter.ViewHolder> {

    private List<UploadStatus> uploadsList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(UploadStatus uploadStatus);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public UploadMonitorAdapter(List<UploadStatus> uploadsList) {
        this.uploadsList = uploadsList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_upload_status, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UploadStatus upload = uploadsList.get(position);
        Log.d("UploadMonitorAdapter", "Binding view for: " + upload.getGameName() + ", Progress: " + upload.getProgress() + ", Status: " + upload.getStatus() + ", Uploaded Bytes: " + upload.getUploadedBytes());
        
        holder.tvGameName.setText(upload.getGameName());
        holder.tvFileName.setText(upload.getFileName());
        holder.tvFileSize.setText(upload.getFormattedFileSize());
        holder.tvUploadedBytes.setText(upload.getFormattedUploadedBytes());
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        holder.tvStartTime.setText("Iniciado às " + dateFormat.format(new Date(upload.getStartTime())));

        if (upload.getStatus() == UploadStatus.Status.UPLOADING || upload.getStatus() == UploadStatus.Status.PAUSED) {
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.progressBar.setProgress(upload.getProgress());
            holder.tvStatus.setTextColor(getThemeColor(holder.itemView.getContext(), com.google.android.material.R.attr.colorPrimary));
        } else {
            holder.progressBar.setVisibility(View.GONE);
            if (upload.getStatus() == UploadStatus.Status.COMPLETED) {
                holder.tvStatus.setTextColor(getThemeColor(holder.itemView.getContext(), com.google.android.material.R.attr.colorTertiary));
            } else if (upload.getStatus() == UploadStatus.Status.ERROR) {
                holder.tvStatus.setTextColor(getThemeColor(holder.itemView.getContext(), com.google.android.material.R.attr.colorError));
            }
        }
        holder.tvStatus.setText(upload.getStatusText());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(upload);
            }
        });
    }

    private int getThemeColor(Context context, @AttrRes int attrRes) {
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(attrRes, typedValue, true)) {
            return typedValue.data;
        }
        return ContextCompat.getColor(context, android.R.color.black);
    }

    @Override
    public int getItemCount() {
        return uploadsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvGameName;
        TextView tvFileName;
        TextView tvFileSize;
        TextView tvUploadedBytes;
        TextView tvStatus;
        TextView tvStartTime;
        ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGameName = itemView.findViewById(R.id.tv_upload_game_name);
            tvFileName = itemView.findViewById(R.id.tv_upload_file_name);
            tvFileSize = itemView.findViewById(R.id.tv_upload_file_size);
            tvUploadedBytes = itemView.findViewById(R.id.tv_upload_uploaded_bytes);
            tvStatus = itemView.findViewById(R.id.tv_upload_status);
            tvStartTime = itemView.findViewById(R.id.tv_upload_start_time);
            progressBar = itemView.findViewById(R.id.progress_upload);
        }
    }
}

