package com.winlator.Download.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.Download.DownloadManagerActivity;
import com.winlator.Download.R;
import com.winlator.Download.model.CommunityFix;
import com.winlator.Download.service.DownloadService;
import com.winlator.Download.utils.AppSettings;


import java.util.ArrayList;
import java.util.List;

public class CommunityFixAdapter extends RecyclerView.Adapter<CommunityFixAdapter.ViewHolder> implements Filterable {

    private List<CommunityFix> fixList;
    private List<CommunityFix> fixListFull;
    private Context context;

    public CommunityFixAdapter(List<CommunityFix> fixList, Context context) {
        this.fixList = new ArrayList<>(fixList);
        this.fixListFull = new ArrayList<>(fixList);
        this.context = context;
    }

    public void setFixList(List<CommunityFix> fixes) {
        this.fixList = new ArrayList<>(fixes);
        this.fixListFull = new ArrayList<>(fixes);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_community_fix, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommunityFix fix = fixList.get(position);
        holder.tvFixName.setText(fix.getFixName());
        holder.tvDescription.setText(fix.getDescription());

        holder.btnDownload.setOnClickListener(v -> {
            String downloadUrl = fix.getDownloadUrl();
            String fixName = fix.getFixName();

            if (downloadUrl == null || downloadUrl.isEmpty()) {
                Log.w("CommunityFixAdapter", "Download URL is null or empty for: " + fixName);
                Toast.makeText(context, "Invalid download URL for: " + fixName, Toast.LENGTH_SHORT).show();
                return;
            }

            boolean disableDirectDownloads = AppSettings.getDisableDirectDownloads(context);

            if (disableDirectDownloads) {
                Log.i("CommunityFixAdapter", "Direct community downloads disabled. Opening URL in browser for: " + fixName);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                try {
                    context.startActivity(browserIntent);
                } catch (Exception e) {
                    Log.e("CommunityFixAdapter", "Failed to open URL in browser: " + downloadUrl, e);
                    Toast.makeText(context, "Could not open link in browser.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.i("CommunityFixAdapter", "Direct community downloads enabled. Starting DownloadService for: " + fixName);
                Intent serviceIntent = new Intent(context, DownloadService.class);
                 // Logic from CommunityGamesAdapter for different URL types
                if (downloadUrl.contains("gofile.io/d/") || downloadUrl.contains("gofile.io/download/") || downloadUrl.contains("gofile.io/w/") || downloadUrl.contains("gofile.io/edit/")) {
                    serviceIntent.setAction(DownloadService.ACTION_RESOLVE_AND_START_GOFILE_DOWNLOAD);
                    serviceIntent.putExtra(DownloadService.EXTRA_GOFILE_URL, downloadUrl);
                    serviceIntent.putExtra(DownloadService.EXTRA_FILE_NAME, fixName);
                } else if (downloadUrl.contains("www.mediafire.com/file/")) {
                    serviceIntent.setAction(DownloadService.ACTION_RESOLVE_AND_START_MEDIAFIRE_DOWNLOAD);
                    serviceIntent.putExtra(DownloadService.EXTRA_MEDIAFIRE_URL, downloadUrl);
                    serviceIntent.putExtra(DownloadService.EXTRA_FILE_NAME, fixName);
                } else if (downloadUrl.contains("drive.google.com")) {
                    serviceIntent.setAction(DownloadService.ACTION_RESOLVE_AND_START_GOOGLE_DRIVE_DOWNLOAD);
                    serviceIntent.putExtra(DownloadService.EXTRA_GOOGLE_DRIVE_URL, downloadUrl);
                    serviceIntent.putExtra(DownloadService.EXTRA_FILE_NAME, fixName);
                } else if (downloadUrl.contains("pixeldrain.com/u/") || downloadUrl.contains("pixeldrain.com/l/")) {
                    serviceIntent.setAction(DownloadService.ACTION_RESOLVE_AND_START_PIXELDRAIN_DOWNLOAD);
                    serviceIntent.putExtra(DownloadService.EXTRA_PIXELDRAIN_URL, downloadUrl);
                    serviceIntent.putExtra(DownloadService.EXTRA_FILE_NAME, fixName);
                } else {
                    serviceIntent.setAction(DownloadService.ACTION_START_DOWNLOAD);
                    serviceIntent.putExtra(DownloadService.EXTRA_URL, downloadUrl);
                    serviceIntent.putExtra(DownloadService.EXTRA_FILE_NAME, fixName);
                }

                if (serviceIntent.getAction() != null) {
                    context.startService(serviceIntent);
                    String toastMessage = "Download started: " + fixName;
                     if (DownloadService.ACTION_RESOLVE_AND_START_PIXELDRAIN_DOWNLOAD.equals(serviceIntent.getAction())) {
                        toastMessage = "Starting Pixeldrain download: " + fixName;
                    }
                    Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();

                    Intent activityIntent = new Intent(context, DownloadManagerActivity.class);
                    context.startActivity(activityIntent);
                } else {
                    Log.e("CommunityFixAdapter", "No action set for serviceIntent. URL: " + downloadUrl);
                    Toast.makeText(context, "Could not start download: unsupported URL type.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return fixList.size();
    }

    @Override
    public Filter getFilter() {
        return fixFilter;
    }

    private Filter fixFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<CommunityFix> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(fixListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (CommunityFix item : fixListFull) {
                    if (item.getFixName().toLowerCase().contains(filterPattern) ||
                        item.getDescription().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            fixList.clear();
            if (results.values != null) {
                fixList.addAll((List) results.values);
            }
            notifyDataSetChanged();
        }
    };

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFixName;
        TextView tvDescription;
        Button btnDownload;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFixName = itemView.findViewById(R.id.tv_fix_name);
            tvDescription = itemView.findViewById(R.id.tv_fix_description);
            btnDownload = itemView.findViewById(R.id.btn_fix_download);
        }
    }
}
