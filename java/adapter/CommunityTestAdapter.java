package com.winlator.Download.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log; // Import Log
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.Download.R;
import com.winlator.Download.model.CommunityTest;

import java.util.ArrayList;
import java.util.List;

public class CommunityTestAdapter extends RecyclerView.Adapter<CommunityTestAdapter.ViewHolder> implements Filterable {

    private static final String TAG = "CommunityTestAdapter"; // Logging Tag

    private List<CommunityTest> testList; // This list is displayed
    private List<CommunityTest> testListFull; // Original full list for filtering
    private Context context;

    public CommunityTestAdapter(List<CommunityTest> testList, Context context) {
        this.context = context;
        this.testList = new ArrayList<>(testList); // Initialize with a copy
        this.testListFull = new ArrayList<>(testList); // Initialize with a copy
        Log.d(TAG, "Constructor called. Initial testList size: " + this.testList.size() + ", testListFull size: " + this.testListFull.size());
    }

    public void setTestList(List<CommunityTest> tests) {
        Log.d(TAG, "setTestList called with " + (tests == null ? "null" : tests.size() + " items."));
        this.testList.clear();
        this.testListFull.clear();
        if (tests != null) {
            this.testList.addAll(tests);
            this.testListFull.addAll(tests);
        }
        Log.d(TAG, "testList size after update: " + this.testList.size() + ", testListFull size: " + this.testListFull.size());
        notifyDataSetChanged();
        Log.d(TAG, "notifyDataSetChanged called from setTestList. Current display list size: " + this.testList.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder called for viewType: " + viewType);
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_community_test, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (testList == null || position < 0 || position >= testList.size()) {
            Log.e(TAG, "onBindViewHolder: Invalid position or testList is null. Position: " + position + ", List size: " + (testList == null ? "null" : testList.size()));
            return;
        }
        CommunityTest test = testList.get(position);
        Log.d(TAG, "onBindViewHolder called for position: " + position + ", Game: " + test.getGameName());

        holder.tvGameName.setText(test.getGameName());
        holder.tvDescription.setText(test.getDescription());

        holder.youtubePlayerContainer.setOnClickListener(v -> {
            String youtubeUrl = test.getYoutubeUrl();
            Log.d(TAG, "Play icon clicked for URL: " + youtubeUrl);
            if (youtubeUrl != null && !youtubeUrl.isEmpty()) {
                try {
                    if (!youtubeUrl.startsWith("http://") && !youtubeUrl.startsWith("https://")) {
                        youtubeUrl = "https://" + youtubeUrl;
                    }
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl));
                    if (intent.resolveActivity(context.getPackageManager()) != null) {
                        context.startActivity(intent);
                    } else {
                        Log.w(TAG, "No app found to open YouTube link: " + youtubeUrl);
                        Toast.makeText(context, "No app found to open YouTube link", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Invalid YouTube URL: " + youtubeUrl, e);
                    Toast.makeText(context, "Invalid YouTube URL", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.w(TAG, "YouTube URL is missing for item: " + test.getGameName());
                Toast.makeText(context, "YouTube URL is missing", Toast.LENGTH_SHORT).show();
            }
        });

        // Ensure the play icon is visible, assuming ic_play is a valid drawable
        if (holder.ivPlayIcon != null) {
             holder.ivPlayIcon.setImageResource(R.drawable.ic_play);
        } else {
            Log.w(TAG, "ivPlayIcon is null in onBindViewHolder for position: " + position);
        }
    }

    @Override
    public int getItemCount() {
        int count = testList == null ? 0 : testList.size();
        Log.d(TAG, "getItemCount called, returning: " + count);
        return count;
    }

    @Override
    public Filter getFilter() {
        Log.d(TAG, "getFilter called");
        return testFilter;
    }

    private Filter testFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            Log.d(TAG, "performFiltering with constraint: " + constraint);
            List<CommunityTest> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(testListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (CommunityTest item : testListFull) {
                    if (item.getGameName().toLowerCase().contains(filterPattern) ||
                        item.getDescription().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            Log.d(TAG, "Filtering completed. Found " + filteredList.size() + " items.");
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            Log.d(TAG, "publishResults called with " + ((List<?>)results.values).size() + " items.");
            testList.clear();
            if (results.values != null) {
                testList.addAll((List<CommunityTest>) results.values);
            }
            notifyDataSetChanged();
            Log.d(TAG, "publishResults finished. Display list size: " + testList.size());
        }
    };

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        Log.d(TAG, "onViewRecycled called for position: " + holder.getAdapterPosition());
        // No specific resources to release here anymore for the YouTube player
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvGameName;
        TextView tvDescription;
        FrameLayout youtubePlayerContainer;
        ImageView ivPlayIcon;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGameName = itemView.findViewById(R.id.tv_test_game_name);
            tvDescription = itemView.findViewById(R.id.tv_test_description);
            youtubePlayerContainer = itemView.findViewById(R.id.youtube_player_container);
            ivPlayIcon = itemView.findViewById(R.id.iv_play_icon);
        }
    }
}
