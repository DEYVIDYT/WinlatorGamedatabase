package com.winlator.Download.adapter;

import android.content.Context;
import android.content.Intent;
// SharedPreferences import removed as it's now encapsulated in AppSettings
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.Download.R;
import com.winlator.Download.model.CommunityGame;
import com.winlator.Download.service.DownloadService;
import com.winlator.Download.DownloadManagerActivity;
import com.winlator.Download.utils.AppSettings; // Added import

import java.util.ArrayList;
import java.util.List;

public class CommunityGamesAdapter extends RecyclerView.Adapter<CommunityGamesAdapter.ViewHolder> implements Filterable {

    // SharedPreferences keys are now in AppSettings.java

    private List<CommunityGame> communityGamesList;
    private List<CommunityGame> communityGamesListFull;
    private Context context; // Keep context

    public CommunityGamesAdapter(List<CommunityGame> communityGamesList, Context context) {
        this.communityGamesList = new ArrayList<>(communityGamesList);
        this.communityGamesListFull = new ArrayList<>(communityGamesList);
        this.context = context; // Keep context
    }

    public void setGamesList(List<CommunityGame> games) {
        this.communityGamesList = new ArrayList<>(games);
        this.communityGamesListFull = new ArrayList<>(games);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_community_game, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommunityGame game = communityGamesList.get(position);
        
        holder.tvGameName.setText(game.getName());
        holder.tvGameSize.setText(game.getSize());
        
        holder.btnDownload.setOnClickListener(v -> {
            String gameUrl = game.getUrl();
            String gameName = game.getName();
            Context itemContext = holder.itemView.getContext(); // Use context from holder.itemView

            if (gameUrl == null || gameUrl.isEmpty()) {
                Log.w("CommunityGamesAdapter", "Game URL is null or empty for: " + gameName);
                Toast.makeText(itemContext, "URL de download inválida para: " + gameName, Toast.LENGTH_SHORT).show();
                return;
            }

            // Use AppSettings to get the preference
            boolean disableDirectDownloads = AppSettings.getDisableDirectDownloads(itemContext);

            if (disableDirectDownloads) {
                Log.i("CommunityGamesAdapter", "Direct community downloads disabled. Opening URL in browser for: " + gameName);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(gameUrl));
                try {
                    itemContext.startActivity(browserIntent);
                } catch (Exception e) {
                    Log.e("CommunityGamesAdapter", "Failed to open URL in browser: " + gameUrl, e);
                    Toast.makeText(itemContext, "Não foi possível abrir o link no navegador.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.i("CommunityGamesAdapter", "Direct community downloads enabled. Starting DownloadService for: " + gameName);
                Intent serviceIntent = new Intent(itemContext, DownloadService.class);

                // Existing logic to determine action based on URL type
                if (gameUrl.contains("gofile.io/d/") || gameUrl.contains("gofile.io/download/") || gameUrl.contains("gofile.io/w/") || gameUrl.contains("gofile.io/edit/")) {
                    Log.d("CommunityGamesAdapter", "Gofile URL detected for game: '" + gameName + "'. URL: '" + gameUrl + "'");
                    serviceIntent.setAction(DownloadService.ACTION_RESOLVE_AND_START_GOFILE_DOWNLOAD);
                    serviceIntent.putExtra(DownloadService.EXTRA_GOFILE_URL, gameUrl);
                    serviceIntent.putExtra(DownloadService.EXTRA_FILE_NAME, gameName); // gameName is used as a fallback by resolver if needed
                } else if (gameUrl.contains("www.mediafire.com/file/")) {
                    Log.d("CommunityGamesAdapter", "MediaFire URL detected for game: '" + gameName + "'. URL: '" + gameUrl + "'");
                    serviceIntent.setAction(DownloadService.ACTION_RESOLVE_AND_START_MEDIAFIRE_DOWNLOAD);
                    serviceIntent.putExtra(DownloadService.EXTRA_MEDIAFIRE_URL, gameUrl);
                    serviceIntent.putExtra(DownloadService.EXTRA_FILE_NAME, gameName);
                } else if (gameUrl.contains("drive.google.com")) {
                    Log.d("CommunityGamesAdapter", "Google Drive URL detected for game: '" + gameName + "'. URL: '" + gameUrl + "'");
                    serviceIntent.setAction(DownloadService.ACTION_RESOLVE_AND_START_GOOGLE_DRIVE_DOWNLOAD);
                    serviceIntent.putExtra(DownloadService.EXTRA_GOOGLE_DRIVE_URL, gameUrl);
                    serviceIntent.putExtra(DownloadService.EXTRA_FILE_NAME, gameName);
                } else if (gameUrl.contains("pixeldrain.com/u/") || gameUrl.contains("pixeldrain.com/l/")) {
                    Log.d("CommunityGamesAdapter", "Pixeldrain URL detected for game: '" + gameName + "'. URL: '" + gameUrl + "'");
                    serviceIntent.setAction(DownloadService.ACTION_RESOLVE_AND_START_PIXELDRAIN_DOWNLOAD);
                    serviceIntent.putExtra(DownloadService.EXTRA_PIXELDRAIN_URL, gameUrl);
                    serviceIntent.putExtra(DownloadService.EXTRA_FILE_NAME, gameName);
                } else {
                    Log.d("CommunityGamesAdapter", "Standard URL detected for game: '" + gameName + "'. URL: '" + gameUrl + "'");
                    serviceIntent.setAction(DownloadService.ACTION_START_DOWNLOAD); // setAction instead of putExtra for action
                    serviceIntent.putExtra(DownloadService.EXTRA_URL, gameUrl);
                    serviceIntent.putExtra(DownloadService.EXTRA_FILE_NAME, gameName);
                }
                // Common code for starting service and showing toast can be outside the if/else if chain if serviceIntent is always initialized.
                // However, the toast message might need to be specific. For now, keeping it simple.

                // Check if serviceIntent action is set before starting
                if (serviceIntent.getAction() != null) {
                    itemContext.startService(serviceIntent);
                    // Make toast message more specific if desired, or keep generic
                    String toastMessage = "Download iniciado: " + gameName;
                    if (DownloadService.ACTION_RESOLVE_AND_START_PIXELDRAIN_DOWNLOAD.equals(serviceIntent.getAction())) {
                        toastMessage = "Iniciando download do Pixeldrain: " + gameName;
                    }
                    Toast.makeText(itemContext, toastMessage, Toast.LENGTH_SHORT).show();
                } else {
                    // This case should ideally not be reached if all URL types are handled or have a default
                    Log.e("CommunityGamesAdapter", "No action set for serviceIntent. URL: " + gameUrl);
                    Toast.makeText(itemContext, "Não foi possível iniciar o download: tipo de URL não suportado.", Toast.LENGTH_LONG).show();
                }


                // Optionally, still navigate to DownloadManagerActivity
                Intent activityIntent = new Intent(itemContext, DownloadManagerActivity.class);
                itemContext.startActivity(activityIntent);
            }
        });
    }

    @Override
    public int getItemCount() {
        // ... (remains the same)
        return communityGamesList.size();
    }

    @Override
    public Filter getFilter() {
        // ... (remains the same)
        return communityGamesFilter;
    }

    private Filter communityGamesFilter = new Filter() {
        // ... (remains the same)
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<CommunityGame> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(communityGamesListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (CommunityGame item : communityGamesListFull) {
                    if (item.getName().toLowerCase().contains(filterPattern)) {
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
            communityGamesList.clear();
            if (results.values != null) {
                communityGamesList.addAll((List) results.values);
            }
            notifyDataSetChanged();
        }
    };

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // ... (remains the same)
        TextView tvGameName;
        TextView tvGameSize;
        Button btnDownload;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGameName = itemView.findViewById(R.id.tv_game_name);
            tvGameSize = itemView.findViewById(R.id.tv_game_size);
            btnDownload = itemView.findViewById(R.id.btn_download);
        }
    }
}
