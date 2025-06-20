package com.winlator.Download.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.winlator.Download.R;
import com.winlator.Download.model.GameEntry;
import java.util.List;
import java.util.ArrayList;

public class MyGamesAdapter extends RecyclerView.Adapter<MyGamesAdapter.ViewHolder> {

    private Context context;
    private List<GameEntry> gameEntries;

    public MyGamesAdapter(Context context, List<GameEntry> gameEntries) {
        this.context = context;
        this.gameEntries = gameEntries != null ? gameEntries : new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_my_game, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GameEntry game = gameEntries.get(position);

        holder.tvGameTitle.setText(game.getTitle());
        // For now, using a placeholder for the banner
        // In a real app, you'd load this using Glide or Picasso, e.g., from game.getBannerImagePath()
        // Consider using a real image loading library here in the future
        if (game.getBannerImagePath() != null && !game.getBannerImagePath().isEmpty()) {
            // Example: Glide.with(context).load(game.getBannerImagePath()).placeholder(android.R.drawable.screen_background_dark).into(holder.ivGameBanner);
            // For now, still using a placeholder if path is present, or a default one if not.
            holder.ivGameBanner.setImageResource(android.R.drawable.screen_background_dark); // Placeholder - replace with actual loading
        } else {
            holder.ivGameBanner.setImageResource(android.R.drawable.ic_menu_gallery); // Default if no banner
        }


        holder.btnPlayGame.setOnClickListener(v -> {
            String desktopFilePath = game.getDesktopFilePath();
            if (desktopFilePath != null && !desktopFilePath.isEmpty()) {
                try {
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setClassName("com.winlator", "com.winlator.XServerDisplayActivity");
                    // The path stored from ACTION_OPEN_DOCUMENT is a content URI.
                    // Winlator likely expects a direct file path.
                    // This conversion is crucial and might need adjustment based on how Winlator handles intents.
                    // For now, we'll pass the URI string. If Winlator can't handle content URIs,
                    // this part will need a more robust solution (e.g., copying to a temp file, or if Winlator has a content provider interface).
                    intent.putExtra("shortcut_path", desktopFilePath); // Passing URI string directly
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

                    holder.itemView.getContext().startActivity(intent);
                } catch (android.content.ActivityNotFoundException e) {
                    android.util.Log.e("MyGamesAdapter", "Winlator not found: " + e.getMessage());
                    Toast.makeText(holder.itemView.getContext(), "Winlator application not found. Please install Winlator.", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    android.util.Log.e("MyGamesAdapter", "Error launching Winlator: " + e.getMessage());
                    Toast.makeText(holder.itemView.getContext(), "Error launching Winlator: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(holder.itemView.getContext(), "Game shortcut path is missing or invalid.", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return gameEntries.size();
    }

    public void setGames(List<GameEntry> games) {
        this.gameEntries.clear();
        if (games != null) {
            this.gameEntries.addAll(games);
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivGameBanner;
        TextView tvGameTitle;
        Button btnPlayGame;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivGameBanner = itemView.findViewById(R.id.iv_game_banner);
            tvGameTitle = itemView.findViewById(R.id.tv_game_title);
            btnPlayGame = itemView.findViewById(R.id.btn_play_game);
        }
    }
}
