package com.winlator.Download.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable; // Added
import androidx.core.app.NotificationCompat;

import com.winlator.Download.MainActivity;
import com.winlator.Download.R;
import com.winlator.Download.db.UploadRepository;
import com.winlator.Download.model.UploadStatus;
// import com.winlator.Download.utils.InternetArchiveUploader; // Removed

import org.json.JSONObject;

import java.io.BufferedReader;
// import java.io.File; // Removed
// import java.io.FileInputStream; // Removed
// import java.io.FileOutputStream; // Removed
import java.io.IOException;
// import java.io.InputStream; // Removed
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
// import java.security.MessageDigest; // Removed
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
// import android.util.Base64; // Removed for MD5
// import android.database.Cursor; // Removed
// import android.provider.MediaStore; // Removed

public class UploadService extends Service {

    private static final String CHANNEL_ID = "upload_channel";
    private static final int NOTIFICATION_ID = 1001;
    public static final String ACTION_UPLOAD_STARTED = "com.winlator.Download.UPLOAD_STARTED";
    public static final String ACTION_UPLOAD_PROGRESS = "com.winlator.Download.UPLOAD_PROGRESS";
    public static final String ACTION_UPLOAD_COMPLETED = "com.winlator.Download.UPLOAD_COMPLETED";
    public static final String ACTION_UPLOAD_ERROR = "com.winlator.Download.UPLOAD_ERROR";

    private NotificationManager notificationManager;
    private ExecutorService executor;
    private PowerManager.WakeLock wakeLock;
    private UploadRepository uploadRepository;

    private static final List<String> ALLOWED_DOMAINS = Arrays.asList(
             "www.mediafire.com",
            "buzzheavier.com",
            "gofile.io",
            "datanodes.to",
            "drive.google.com",
            "pixeldrain.com",
            "dn721601.ca.archive.org",
            "archive.org"
    );

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        executor = Executors.newSingleThreadExecutor();
        uploadRepository = new UploadRepository(this);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "UploadService::UploadWakelockTag");
            wakeLock.setReferenceCounted(false); // Hold wake lock for the service lifetime
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
                Log.d("UploadService", "Service WakeLock acquired.");
            }
        } else {
            Log.e("UploadService", "PowerManager not available, WakeLock not initialized.");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String gameName = intent.getStringExtra("game_name");
            String gameLink = intent.getStringExtra("game_link");
            long gameSizeBytes = intent.getLongExtra("game_size_bytes", 0);
            String fileUriString = null; // Assuming file URI is not passed for link-based uploads
            String fileName = gameName; // Using gameName as fileName for now
            int uploadId = intent.getIntExtra("upload_id", -1);

            String currentProcessGameName = gameName;
            if (currentProcessGameName == null || currentProcessGameName.isEmpty()) {
                currentProcessGameName = "Novo Jogo";
            }

            // Use a more generic "fileName" for UploadStatus if gameLink doesn't imply one.
            // For this iteration, we'll use gameName as a placeholder for fileName if a distinct one isn't derived.
            // The UploadStatus constructor will need to be adapted if its 'fileName' field is critical and different from gameName.
            // Assuming UploadStatus will be updated to take gameLink and gameSizeBytes
            // and that its internal 'fileName' might be derived or set to gameName.

            Notification preparingNotification = createNotification("Preparando " + currentProcessGameName + "...", 0);
            startForeground(NOTIFICATION_ID, preparingNotification);

            executor.execute(() -> {
                UploadStatus uploadStatus;
                if (uploadId != -1) {
                    uploadStatus = uploadRepository.getUploadById(uploadId);
                    if (uploadStatus == null) {
                        Log.e("UploadService", "UploadStatus com ID " + uploadId + " não encontrado. Iniciando novo upload.");
                        uploadStatus = new UploadStatus(gameName, fileName, gameSizeBytes, fileUriString, gameLink);
                        // Set id after insertion for new UploadStatus object
                        long newId = uploadRepository.insertUpload(uploadStatus);
                        uploadStatus.setId((int) newId);
                    } else {
                        if (uploadStatus.getStatus() == UploadStatus.Status.PAUSED || uploadStatus.getStatus() == UploadStatus.Status.ERROR) {
                            uploadStatus.setStatus(UploadStatus.Status.UPLOADING);
                            uploadStatus.setFileSize(gameSizeBytes); // User might have changed this for a retry
                            uploadStatus.setGameLink(gameLink);
                            uploadStatus.setFileUri(fileUriString);
                            // Reset progress for resume, or let UploadService manage it based on actual uploaded bytes if resumable links were a thing
                            uploadStatus.setProgress(0);
                            uploadStatus.setUploadedBytes(0);
                            uploadRepository.updateUpload(uploadStatus);
                        }
                        // If not paused or error, it might be an active upload the service is re-attaching to.
                        // Or if the user tries to "start" an already completed/processing one, this logic might need refinement.
                        // For now, we assume if it's an existing ID, we primarily care about PAUSED/ERROR states for restart.
                    }
                } else {
                    uploadStatus = new UploadStatus(gameName, fileName, gameSizeBytes, fileUriString, gameLink);
                    long newId = uploadRepository.insertUpload(uploadStatus);
                    uploadStatus.setId((int) newId);
                }

                sendUploadBroadcast(ACTION_UPLOAD_STARTED, uploadStatus, null);
                uploadGame(uploadStatus);
            });
        }
        return START_NOT_STICKY;
    }

    private boolean isValidGameLink(String gameLink) {
        if (gameLink == null || gameLink.trim().isEmpty()) {
            return false;
        }
        try {
            URL url = new URL(gameLink);
            String host = url.getHost();
            return ALLOWED_DOMAINS.contains(host);
        } catch (Exception e) {
            Log.e("UploadService", "Error validating URL: " + gameLink, e);
            return false;
        }
    }

    private void uploadGame(UploadStatus uploadStatus) {
        updateNotification("Validando link para " + uploadStatus.getGameName() + "...", 10);
        uploadStatus.setProgress(10); // Keep status object updated
        sendUploadBroadcast(ACTION_UPLOAD_PROGRESS, uploadStatus, null);

        if (!isValidGameLink(uploadStatus.getGameLink())) {
            String errorMsg = "Link do jogo inválido ou não permitido: " + uploadStatus.getGameLink();
            Log.e("UploadService", errorMsg);
            handleError(uploadStatus, errorMsg); // handleError will update status and call sendUploadBroadcast
            stopSelfIfNeeded();
            return;
        }

        updateNotification("Registrando " + uploadStatus.getGameName() + " com o servidor...", 50);
        uploadStatus.setProgress(50); // Keep status object updated
        sendUploadBroadcast(ACTION_UPLOAD_PROGRESS, uploadStatus, null);

        sendToPhpApi(uploadStatus);
    }

    // getFilePathFromDownloadUri method removed
    // copyUriToTempFileInternal method removed
    // calculateMd5FromFile method removed
    // calculateMd5FromUri method removed

    private void sendToPhpApi(UploadStatus uploadStatus) {
        try {
            updateNotification("Registrando " + uploadStatus.getGameName() + "...", 75);

            JSONObject jsonData = new JSONObject();
            jsonData.put("name", uploadStatus.getGameName());
            jsonData.put("size", formatFileSize(uploadStatus.getFileSize()));
            jsonData.put("url", uploadStatus.getGameLink()); // Get gameLink from UploadStatus

            URL url = new URL("https://ldgames.x10.mx/add_update_game.php");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(jsonData.toString().getBytes());
            outputStream.close();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject responseJson = new JSONObject(response.toString());
                if (responseJson.getBoolean("success")) {
                    String successMsg = "Registro de " + uploadStatus.getGameName() + " concluído com sucesso!";
                    showSuccessNotification(successMsg);
                    uploadStatus.setStatus(UploadStatus.Status.COMPLETED);
                    uploadStatus.setProgress(100);
                    uploadStatus.setUploadedBytes(uploadStatus.getFileSize()); // Mark as fully "uploaded"
                    uploadRepository.updateUpload(uploadStatus);
                    sendUploadBroadcast(ACTION_UPLOAD_COMPLETED, uploadStatus, null);
                } else {
                    String error = "Erro da API ao registrar " + uploadStatus.getGameName() + ": " + responseJson.getString("message");
                    handleError(uploadStatus, error);
                }
            } else {
                String error = "Erro de conexão com API ao registrar " + uploadStatus.getGameName() + ": Código " + responseCode;
                handleError(uploadStatus, error);
            }
            connection.disconnect();
        } catch (Exception e) {
            Log.e("UploadService", "Exceção ao enviar para API PHP (" + uploadStatus.getGameName() + "): " + e.getMessage(), e);
            String error = "Exceção na API para " + uploadStatus.getGameName() + ": " + e.getMessage();
            handleError(uploadStatus, error);
        }
        stopSelfIfNeeded();
    }

    private void handleError(UploadStatus uploadStatus, String errorMessage) {
        showErrorNotification(errorMessage);
        uploadStatus.setStatus(UploadStatus.Status.ERROR);
        uploadStatus.setErrorMessage(errorMessage);
        uploadRepository.updateUpload(uploadStatus);
        sendUploadBroadcast(ACTION_UPLOAD_ERROR, uploadStatus, errorMessage);
    }

    // Updated sendUploadBroadcast to take UploadStatus object
    private void sendUploadBroadcast(String action, UploadStatus status, @Nullable String errorMessage) {
        Intent broadcast = new Intent(action);
        broadcast.putExtra("upload_id", status.getId());
        broadcast.putExtra("game_name", status.getGameName());
        broadcast.putExtra("file_name", status.getFileName() != null ? status.getFileName() : status.getGameName());
        broadcast.putExtra("file_size", status.getFileSize());
        broadcast.putExtra("progress", status.getProgress());
        broadcast.putExtra("uploaded_bytes", status.getUploadedBytes());
        broadcast.putExtra("file_uri", status.getFileUri());
        broadcast.putExtra("game_link", status.getGameLink());
        broadcast.putExtra("start_time", status.getStartTime());
        // No longer broadcasting status string, receiver infers from action or uses persisted one.
        // For error, it's passed explicitly.

        if (errorMessage != null) {
            broadcast.putExtra("error", errorMessage);
        }

        Log.d("UploadService", "Sending broadcast: " + action + " for " + status.getGameName() + " (ID: " + status.getId() + ") Prog: " + status.getProgress());
        sendBroadcast(broadcast);
    }

    private String formatFileSize(long size) {
        // Keep this utility function as it's used for the API payload
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024.0));
        return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Upload de Jogos",
                NotificationManager.IMPORTANCE_LOW // Or IMPORTANCE_DEFAULT if errors are critical
            );
            channel.setDescription("Notificações de upload de jogos da comunidade");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String text, int progress) {
        Intent intent = new Intent(this, MainActivity.class); // Or specific upload monitor activity
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Registro de Jogo") // Changed title
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_upload) // Keep relevant icon
            .setContentIntent(pendingIntent)
            .setOngoing(true); // Service is foreground

        // Simplified progress: 0 for initial, 10 for validating, 50 for registering, 100 for done/error
        if (progress > 0 && progress < 100) {
            builder.setProgress(100, progress, false); // determinate for known steps
        } else if (progress == 0 && text.toLowerCase().contains("preparando")) {
             builder.setProgress(100, 0, true); // Indeterminate for initial "preparing"
        } else if (progress == 10 && text.toLowerCase().contains("validando")) {
             builder.setProgress(100, 10, true); // Indeterminate for "validating"
        } else if (progress == 50 && text.toLowerCase().contains("registrando")) {
             builder.setProgress(100, 50, true); // Indeterminate for "registering"
        }
        // For 100% (completion) or error, setOngoing(false) and AutoCancel(true) is handled by specific notification methods

        return builder.build();
    }

    private void updateNotification(String text, int progress) {
        // Ensure this is only called when the service is in foreground state
        if (notificationManager != null) {
            Notification notification = createNotification(text, progress);
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private void showSuccessNotification(String message) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Registro Concluído") // Changed title
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_download) // Or a success icon like ic_check
            .setContentIntent(pendingIntent)
            .setOngoing(false) // No longer ongoing
            .setAutoCancel(true)
            .build();
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notification); // Use same ID to replace progress
        }
    }

    private void showErrorNotification(String error) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Erro no Registro") // Changed title
            .setContentText(error)
            .setSmallIcon(R.drawable.ic_cancel) // Keep error icon
            .setContentIntent(pendingIntent)
            .setOngoing(false) // No longer ongoing
            .setAutoCancel(true)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(error)) // For longer error messages
            .build();
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notification); // Use same ID to replace progress
        }
    }

    private void stopSelfIfNeeded() {
        // Consider if the service should stop after one operation or if it's designed to handle multiple.
        // For now, assume it stops after each completed/failed operation.
        Log.d("UploadService", "Upload task finished. Stopping service.");
        stopForeground(true); // Remove notification
        stopSelf();
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d("UploadService", "Service WakeLock released.");
        }
        Log.d("UploadService", "Service destroyed.");
    }
}

