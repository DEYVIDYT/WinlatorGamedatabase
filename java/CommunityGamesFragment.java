package com.winlator.Download;

// import android.app.AlertDialog; // To be removed
import androidx.appcompat.app.AlertDialog; // Added for explicit typing
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.Intent;
// import android.content.SharedPreferences; // Removed
// import android.database.Cursor; // Removed for file selection
// import android.net.Uri; // Removed for file selection
import android.os.Bundle;
// import android.provider.OpenableColumns; // Removed for file selection
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText; // Keep for game name
import android.widget.SeekBar;
import android.widget.Spinner; // Added for Spinner
import android.widget.AdapterView; // Added for Spinner listener
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView; // Added

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.winlator.Download.adapter.CommunityGamesAdapter;
import com.winlator.Download.db.UploadRepository;
import com.winlator.Download.model.CommunityGame;
// import com.winlator.Download.model.UploadStatus; // Not directly used for creation here anymore
import com.winlator.Download.service.UploadService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommunityGamesFragment extends Fragment {

    // private static final int PICK_FILE_REQUEST = 1001; // Removed
    
    private RecyclerView recyclerView;
    private CommunityGamesAdapter adapter;
    private List<CommunityGame> gamesList;
    private ExecutorService executor;
    private FloatingActionButton fabUpload;
    private UploadRepository uploadRepository;
    
    // Dialog variables - updated
    private EditText etDialogGameName;
    private TextInputEditText etDialogGameLink;
    private SeekBar sbDialogGameSize;
    private TextView tvDialogSelectedSize;
    private TextInputEditText etDialogManualGameSize;
    private Spinner spinnerDialogSizeUnit;
    private Button btnDialogUpload;

    // Flags to prevent listener loops
    private boolean isUpdatingFromSeekBar = false;
    private boolean isUpdatingFromEditText = false;
    private boolean isUpdatingFromSpinner = false;

    // Constants for size units and SeekBar limits
    private static final int MAX_MB_SEEKBAR = 1024; // e.g. up to 1GB in MB mode for finer control
    private static final int MAX_GB_SEEKBAR = 100;  // e.g. up to 100GB in GB mode
    private static final int GB_SPINNER_POSITION = 1;
    private static final int MB_SPINNER_POSITION = 0;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community_games, container, false);
        
        // RecyclerView and FAB are initialized here
        recyclerView = view.findViewById(R.id.recycler_view_community_games);
        fabUpload = view.findViewById(R.id.fab_upload);
        
        // Adapter and LayoutManager setup
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        gamesList = new ArrayList<>();
        adapter = new CommunityGamesAdapter(gamesList, getContext()); // gamesList is initialized before this
        recyclerView.setAdapter(adapter);
        
        // Executor and Repository initialization
        executor = Executors.newSingleThreadExecutor();
        uploadRepository = new UploadRepository(getContext()); // Assuming this is still needed or used elsewhere
        
        // Setup for FAB and initial data load
        setupFabClickListener();
        loadCommunityGames(); // This will populate gamesList and communityGamesListFull in adapter via setGamesList
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SearchView searchView = view.findViewById(R.id.search_view_community_games);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Optional: Handle search query submission.
                // Usually, filtering is live with onQueryTextChange.
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) {
                    adapter.getFilter().filter(newText);
                }
                return false;
            }
        });
    }

    private void setupFabClickListener() {
        fabUpload.setOnClickListener(v -> showUploadDialog());
    }

    private void showUploadDialog() {
        // SharedPreferences prefs = requireContext().getSharedPreferences("community_games", requireContext().MODE_PRIVATE); // Removed IA creds
        // String accessKey = prefs.getString("access_key", ""); // Removed
        // String secretKey = prefs.getString("secret_key", ""); // Removed
        // String itemIdentifier = prefs.getString("item_identifier", ""); // Removed

        // if (accessKey.isEmpty() || secretKey.isEmpty() || itemIdentifier.isEmpty()) { // Removed IA check
        //     Toast.makeText(getContext(), "Configure primeiro as credenciais do Internet Archive nas configurações", Toast.LENGTH_LONG).show();
        //     Intent intent = new Intent(getContext(), SettingsActivity.class);
        //     startActivity(intent);
        //     return;
        // }

        // AlertDialog.Builder builder = new AlertDialog.Builder(getContext()); // Old
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext()); // New
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_upload_game, null);
        builder.setView(dialogView);

        androidx.appcompat.app.AlertDialog dialog = builder.create(); // Changed variable type

        etDialogGameName = dialogView.findViewById(R.id.et_dialog_game_name);
        etDialogGameLink = dialogView.findViewById(R.id.et_dialog_game_link);
        sbDialogGameSize = dialogView.findViewById(R.id.sb_dialog_game_size);
        tvDialogSelectedSize = dialogView.findViewById(R.id.tv_dialog_selected_size);
        etDialogManualGameSize = dialogView.findViewById(R.id.et_dialog_game_size);
        spinnerDialogSizeUnit = dialogView.findViewById(R.id.spinner_dialog_size_unit); // Added
        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        btnDialogUpload = dialogView.findViewById(R.id.btn_dialog_upload);

        // Initialize UI states
        isUpdatingFromSeekBar = false; // Reset flags
        isUpdatingFromEditText = false;
        isUpdatingFromSpinner = false;

        spinnerDialogSizeUnit.setSelection(GB_SPINNER_POSITION); // Default to GB
        sbDialogGameSize.setMax(MAX_GB_SEEKBAR);
        etDialogManualGameSize.setText("0");
        sbDialogGameSize.setProgress(0);
        updateSelectedSizeTextView(0, "GB"); // Initial display
        updateUploadButtonState();


        TextWatcher formValidationWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { updateUploadButtonState(); }
        };

        etDialogGameName.addTextChangedListener(formValidationWatcher);
        etDialogGameLink.addTextChangedListener(formValidationWatcher);

        sbDialogGameSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || isUpdatingFromEditText || isUpdatingFromSpinner) return;

                isUpdatingFromSeekBar = true;
                String currentUnit = spinnerDialogSizeUnit.getSelectedItem().toString();
                etDialogManualGameSize.setText(String.valueOf(progress));
                // No need to change spinner selection here, SeekBar always reflects the selected unit's scale
                updateSelectedSizeTextView(progress, currentUnit);
                updateUploadButtonState();
                isUpdatingFromSeekBar = false;
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        etDialogManualGameSize.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isUpdatingFromSeekBar || isUpdatingFromSpinner) return;

                isUpdatingFromEditText = true;
                String valueStr = s.toString().trim();
                double numericValue = 0;
                if (isValidNumber(valueStr)) {
                    numericValue = Double.parseDouble(valueStr);
                } else if (!valueStr.isEmpty()){ // if not empty and not valid, show error indication
                     etDialogManualGameSize.setError("Número inválido"); // Basic error
                }


                String currentUnit = spinnerDialogSizeUnit.getSelectedItem().toString();
                int maxSeekBar = "GB".equals(currentUnit) ? MAX_GB_SEEKBAR : MAX_MB_SEEKBAR;
                int progress = (int) Math.round(numericValue);

                sbDialogGameSize.setProgress(Math.min(maxSeekBar, Math.max(0,progress)));
                updateSelectedSizeTextView(numericValue, currentUnit); // Display what user typed
                updateUploadButtonState();
                isUpdatingFromEditText = false;
            }
        });

        spinnerDialogSizeUnit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isUpdatingFromSeekBar || isUpdatingFromEditText) return;

                isUpdatingFromSpinner = true;
                String selectedUnit = parent.getItemAtPosition(position).toString();
                double currentValue = 0;
                String currentTextValue = etDialogManualGameSize.getText().toString().trim();
                if (isValidNumber(currentTextValue)) {
                    currentValue = Double.parseDouble(currentTextValue);
                }

                if ("MB".equals(selectedUnit)) {
                    sbDialogGameSize.setMax(MAX_MB_SEEKBAR);
                    // Value in EditText now refers to MB. If it was GB, it should be converted.
                    // For simplicity, assume user changes unit for current number.
                    int progressMB = Math.min(MAX_MB_SEEKBAR, (int) Math.round(currentValue));
                    sbDialogGameSize.setProgress(progressMB);
                    updateSelectedSizeTextView(currentValue, "MB");
                } else { // GB
                    sbDialogGameSize.setMax(MAX_GB_SEEKBAR);
                    // Value in EditText now refers to GB.
                    int progressGB = Math.min(MAX_GB_SEEKBAR, (int) Math.round(currentValue));
                    sbDialogGameSize.setProgress(progressGB);
                    updateSelectedSizeTextView(currentValue, "GB");
                }
                updateUploadButtonState();
                isUpdatingFromSpinner = false;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            // cleanupDialogViewReferences(); // Handled by onDismiss
        });

        btnDialogUpload.setOnClickListener(v -> {
            String gameName = etDialogGameName.getText().toString().trim();
            String gameLink = etDialogGameLink.getText().toString().trim();

            String sizeValueStr = etDialogManualGameSize.getText().toString().trim();
            if (TextUtils.isEmpty(sizeValueStr) || !isValidNumber(sizeValueStr)) {
                 etDialogManualGameSize.setError("Valor numérico inválido");
                 Toast.makeText(getContext(), "Valor de tamanho inválido", Toast.LENGTH_SHORT).show();
                return;
            }
            double sizeValue = Double.parseDouble(sizeValueStr);
            String selectedUnit = spinnerDialogSizeUnit.getSelectedItem().toString();
            long gameSizeBytes = 0;

            if ("MB".equalsIgnoreCase(selectedUnit)) {
                gameSizeBytes = (long) (sizeValue * 1024 * 1024);
            } else if ("GB".equalsIgnoreCase(selectedUnit)) {
                gameSizeBytes = (long) (sizeValue * 1024 * 1024 * 1024);
            }

            if (TextUtils.isEmpty(gameName)) {
                etDialogGameName.setError("Nome do jogo é obrigatório");
                Toast.makeText(getContext(), "Nome do jogo é obrigatório", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(gameLink)) {
                 etDialogGameLink.setError("Link do jogo é obrigatório");
                Toast.makeText(getContext(), "Link do jogo é obrigatório", Toast.LENGTH_SHORT).show();
                return;
            }
            // Basic URL validation (more robust validation server-side)
            try {
                new URL(gameLink);
            } catch (Exception e) {
                etDialogGameLink.setError("Link do jogo inválido");
                Toast.makeText(getContext(), "Link do jogo inválido", Toast.LENGTH_SHORT).show();
                return;
            }
            if (gameSizeBytes <= 0) {
                Toast.makeText(getContext(), "Tamanho do jogo deve ser maior que zero", Toast.LENGTH_SHORT).show();
                 etDialogManualGameSize.setError("Tamanho inválido");
                return;
            }

            // UploadService Intent
            Intent uploadIntent = new Intent(getContext(), UploadService.class);
            // uploadIntent.putExtra("upload_id", newUpload.getId()); // ID will be generated by service/repo
            uploadIntent.putExtra("game_name", gameName);
            uploadIntent.putExtra("game_link", gameLink);
            uploadIntent.putExtra("game_size_bytes", gameSizeBytes);
            uploadIntent.putExtra("file_name", gameName); // Using gameName as fileName

            ContextCompat.startForegroundService(requireContext(), uploadIntent);
            
            Toast.makeText(getContext(), "Registro de jogo iniciado", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            // cleanupDialogViewReferences(); // Handled by onDismiss
        });

        dialog.setOnDismissListener(dialogInterface -> cleanupDialogViewReferences());
        dialog.show();
    }

    // onActivityResult and getFileInfo removed as file selection is gone.

    // Method to update the selected size TextView (tvDialogSelectedSize)
    private void updateSelectedSizeTextView(double value, String unit) {
        if (tvDialogSelectedSize == null) return;
        if ("MB".equalsIgnoreCase(unit)) {
            // For MB, typically whole numbers are expected by users in this context
            tvDialogSelectedSize.setText(String.format(java.util.Locale.getDefault(), "%.0f MB", value));
        } else { // GB
            // For GB, allow one decimal place
            tvDialogSelectedSize.setText(String.format(java.util.Locale.getDefault(), "%.1f GB", value));
        }
    }

    private void updateUploadButtonState() {
        if (btnDialogUpload == null || etDialogGameName == null || etDialogGameLink == null || etDialogManualGameSize == null || sbDialogGameSize == null) {
            return; // Dialog not fully initialized or already dismissed
        }
        String gameName = etDialogGameName.getText().toString().trim();
        String gameLink = etDialogGameLink.getText().toString().trim();

        boolean isSizeValid = false;
        String manualSizeStr = etDialogManualGameSize.getText().toString().trim();
        boolean isSizeValueValid = false;
        if (isValidNumber(manualSizeStr)) {
            double value = Double.parseDouble(manualSizeStr);
            if (value > 0) {
                isSizeValueValid = true;
            }
        }
        // If manual input is empty or invalid, consider if SeekBar value should enable.
        // But since listeners sync them, etDialogManualGameSize should reflect SeekBar.
        // So, valid numeric text > 0 in etDialogManualGameSize is the primary check.

        btnDialogUpload.setEnabled(!TextUtils.isEmpty(gameName) && !TextUtils.isEmpty(gameLink) && isSizeValueValid);
    }

    // updateSeekBarFromManualInput was removed, logic integrated into listeners.
    // Re-adding a refined version or ensuring listeners cover all sync.
    // The TextWatcher for etDialogManualGameSize already updates the SeekBar.
    // The Spinner's onItemSelected listener also updates the SeekBar based on current EditText value and new unit.

    private boolean isValidNumber(String str) {
        if (str == null || str.trim().isEmpty()) return false;
        try {
            Double.parseDouble(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void cleanupDialogViewReferences() {
        etDialogGameName = null;
        etDialogGameLink = null;
        sbDialogGameSize = null;
        tvDialogSelectedSize = null;
        etDialogManualGameSize = null;
        spinnerDialogSizeUnit = null; // Added
        btnDialogUpload = null;
    }

    // formatFileSize can be kept if used elsewhere, or removed if only for old dialog
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B"; // Ensure this utility is still needed. UploadService has one.
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024.0));
        return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }

    private void loadCommunityGames() {
        executor.execute(() -> {
            try {
                URL url = new URL("https://ldgames.x10.mx/list_games.php");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    parseGamesJson(response.toString());
                } else {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> 
                            Toast.makeText(getContext(), "Erro ao carregar jogos da comunidade", Toast.LENGTH_SHORT).show()
                        );
                    }
                }
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> 
                        Toast.makeText(getContext(), "Erro de conexão", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void parseGamesJson(String jsonString) {
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            List<CommunityGame> newGamesList = new ArrayList<>();
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject gameObject = jsonArray.getJSONObject(i);
                String name = gameObject.getString("name");
                String size = gameObject.getString("size");
                String url = gameObject.getString("url");
                
                CommunityGame game = new CommunityGame(name, size, url);
                newGamesList.add(game);
            }
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (newGamesList != null) {
                        java.util.Collections.reverse(newGamesList); // Reverse the list here
                    }
                    if (adapter != null) {
                         adapter.setGamesList(newGamesList);
                    }
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> 
                    Toast.makeText(getContext(), "Erro ao processar dados", Toast.LENGTH_SHORT).show()
                );
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cleanupDialogViewReferences();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}


