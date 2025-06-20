package com.winlator.Download.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.winlator.Download.R;
import com.winlator.Download.model.CommunityGame; // Assuming CommunityGame model exists
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

public class ExploreCommunityGamesAdapter extends RecyclerView.Adapter<ExploreCommunityGamesAdapter.ViewHolder> {

    private Context context;
    private List<CommunityGame> communityGames;

    public ExploreCommunityGamesAdapter(Context context, List<CommunityGame> communityGames) {
        this.context = context;
        this.communityGames = communityGames != null ? communityGames : new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_explore_game, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommunityGame game = communityGames.get(position);

        holder.tvGameName.setText(game.getName());
        holder.tvGameSize.setText(String.format(Locale.getDefault(), "Size: %.2f GB", game.getSizeInGB()));

        holder.btnGameDetails.setOnClickListener(v -> {
            String details = "Name: " + game.getName() +
                             "\nSize: " + String.format(Locale.getDefault(), "%.2f GB", game.getSizeInGB()) +
                             "\nURL: " + game.getUrl();
            Toast.makeText(context, details, Toast.LENGTH_LONG).show();
            // Future: Implement a proper details screen/dialog
        });
    }

    @Override
    public int getItemCount() {
        return communityGames.size();
    }

    public void setGames(List<CommunityGame> games) {
        this.communityGames.clear();
        if (games != null) {
            this.communityGames.addAll(games);
        }
        notifyDataSetChanged(); // Consider more efficient updates like DiffUtil for larger lists
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvGameName;
        TextView tvGameSize;
        Button btnGameDetails;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGameName = itemView.findViewById(R.id.tv_explore_game_name);
            tvGameSize = itemView.findViewById(R.id.tv_explore_game_size);
            btnGameDetails = itemView.findViewById(R.id.btn_explore_game_details);
        }
    }
}
