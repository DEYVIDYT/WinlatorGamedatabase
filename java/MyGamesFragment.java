package com.winlator.Download;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.content.Intent;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.winlator.Download.adapter.MyGamesAdapter;
import com.winlator.Download.db.GamesDatabaseHelper;
import com.winlator.Download.model.GameEntry;
import java.util.ArrayList;
import java.util.List;

public class MyGamesFragment extends Fragment {

    private RecyclerView recyclerView;
    private MyGamesAdapter adapter;
    private List<GameEntry> gameList = new ArrayList<>();
    private FloatingActionButton fabAddGame;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_games, container, false);

        recyclerView = view.findViewById(R.id.rv_my_games);
        fabAddGame = view.findViewById(R.id.fab_add_game);

        setupRecyclerView();
        // loadDummyGames(); // Removed dummy data loading

        fabAddGame.setOnClickListener(v -> {
            // Toast.makeText(getContext(), "Add Game clicked", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), AddGameActivity.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadGamesFromDb();
    }

    private void setupRecyclerView() {
        // Initialize gameList if it's null, though it's initialized at declaration
        if (gameList == null) {
            gameList = new ArrayList<>();
        }
        adapter = new MyGamesAdapter(getContext(), gameList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadGamesFromDb() {
        GamesDatabaseHelper dbHelper = new GamesDatabaseHelper(getContext());
        List<GameEntry> games = dbHelper.getAllGames();
        if (games != null) {
            // It's better to create a new list for the adapter if the adapter
            // is sensitive to the list instance, or update its internal list carefully.
            // For this adapter, setGames clears and adds all, so it's fine.
            adapter.setGames(games);
        } else {
            adapter.setGames(new ArrayList<>()); // Pass an empty list if null
        }
    }

    // private void loadDummyGames() {
    //     // Create some dummy game entries
    //     // In a real app, this data would come from a database or user input
    //     gameList.add(new GameEntry(1L, "Cyberpunk 2077", "/path/to/cyberpunk.desktop", "path/or/url/to/banner1.jpg"));
    //     gameList.add(new GameEntry(2L, "The Witcher 3", "/path/to/witcher3.desktop", "path/or/url/to/banner2.jpg"));
    //     gameList.add(new GameEntry(3L, "Stardew Valley", "/path/to/stardew.desktop", "path/or/url/to/banner3.jpg"));
    //
    //     adapter.setGames(gameList);
    // }
}
