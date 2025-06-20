package com.winlator.Download.model;

public class Download {
    private long id;
    private String url;
    private String fileName;
    private String localPath;
    private long totalBytes;
    private long downloadedBytes;
    private int status;
    private long timestamp;
    private double speed; // Velocidade em bytes por segundo (calculada, não armazenada no banco)
    private boolean isSelected; // Novo campo para controlar seleção

    // Status constants (duplicados do DownloadContract para facilitar o acesso)
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_DOWNLOADING = 1;
    public static final int STATUS_COMPLETED = 2;
    public static final int STATUS_FAILED = 3;
    public static final int STATUS_PAUSED = 4;

    public Download(long id, String url, String fileName, String localPath, long totalBytes, long downloadedBytes, int status, long timestamp) {
        this.id = id;
        this.url = url;
        this.fileName = fileName;
        this.localPath = localPath;
        this.totalBytes = totalBytes;
        this.downloadedBytes = downloadedBytes;
        this.status = status;
        this.timestamp = timestamp;
        this.speed = 0;
        this.isSelected = false;
    }

    public long getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getFileName() {
        return fileName;
    }

    public String getLocalPath() {
        return localPath;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public long getDownloadedBytes() {
        return downloadedBytes;
    }

    public int getStatus() {
        return status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public int getProgress() {
        if (totalBytes <= 0) {
            return 0;
        }
        return (int) ((downloadedBytes * 100) / totalBytes);
    }

    // Método para formatar o tamanho do arquivo em KB, MB, etc.
    public String getFormattedTotalSize() {
        return formatSize(totalBytes);
    }

    public String getFormattedDownloadedSize() {
        return formatSize(downloadedBytes);
    }

    public String getFormattedSpeed() {
        return formatSize((long) speed) + "/s";
    }

    private String formatSize(long bytes) {
        if (bytes < 0) {
            return "Desconhecido";
        } else if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}

