package com.winlator.Download.adapter;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.Download.R;
import com.winlator.Download.model.Release;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReleasesAdapter extends RecyclerView.Adapter<ReleasesAdapter.ViewHolder> {

    private List<Release> releases;
    private Context context;
    private OnReleaseClickListener releaseClickListener;

    // Interface para eventos de clique no release
    public interface OnReleaseClickListener {
        void onReleaseDownloadClick(Release release);
        void onReleaseItemClick(Release release);
    }

    public ReleasesAdapter(Context context, OnReleaseClickListener listener) {
        this.context = context;
        this.releases = new ArrayList<>();
        this.releaseClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_release, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Release release = releases.get(position);
        
        // Configurar o título do release
        String title = release.getName();
        if (title == null || title.isEmpty()) {
            title = release.getTagName();
        }
        holder.titleTextView.setText(title);
        
        // Configurar a data de publicação formatada
        holder.dateTextView.setText(formatDate(release.getPublishedAt()));
        
        // Configurar o nome e tamanho do arquivo
        String assetInfo = release.getAssetName() + " (" + release.getFormattedSize() + ")";
        holder.assetInfoTextView.setText(assetInfo);
        
        // Configurar a descrição do release (primeiros 100 caracteres)
        String description = release.getBody();
        if (description != null && !description.isEmpty()) {
            if (description.length() > 100) {
                description = description.substring(0, 100) + "...";
            }
            holder.descriptionTextView.setText(Html.fromHtml(description, Html.FROM_HTML_MODE_COMPACT));
            holder.descriptionTextView.setVisibility(View.VISIBLE);
        } else {
            holder.descriptionTextView.setVisibility(View.GONE);
        }

        // Configurar o botão de download
        holder.downloadButton.setOnClickListener(v -> {
            if (releaseClickListener != null) {
                releaseClickListener.onReleaseDownloadClick(release);
            }
        });

        // Configurar o clique no item completo do release
        holder.itemView.setOnClickListener(v -> {
            if (releaseClickListener != null) {
                releaseClickListener.onReleaseItemClick(release);
            }
        });
    }

    @Override
    public int getItemCount() {
        return releases != null ? releases.size() : 0;
    }

    public void updateData(List<Release> newReleases) {
        this.releases.clear();
        if (newReleases != null) {
            this.releases.addAll(newReleases);
        }
        notifyDataSetChanged();
    }

    // Método para formatar a data
    private String formatDate(String dateString) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ss\'Z\'", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        try {
            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return dateString; // Retorna a string original em caso de erro
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView dateTextView;
        TextView assetInfoTextView;
        TextView descriptionTextView;
        Button downloadButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.textViewReleaseTitle);
            dateTextView = itemView.findViewById(R.id.textViewReleaseDate);
            assetInfoTextView = itemView.findViewById(R.id.textViewAssetInfo);
            descriptionTextView = itemView.findViewById(R.id.textViewReleaseDescription);
            downloadButton = itemView.findViewById(R.id.buttonDownloadRelease);
        }
    }
}


