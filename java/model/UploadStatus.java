package com.winlator.Download.model;

import java.util.Locale;

public class UploadStatus {
    
    public enum Status {
        UPLOADING,
        COMPLETED,
        ERROR,
        PAUSED
    }
    
    private int id;
    private String gameName;
    private String fileName;
    private long fileSize;
    private Status status;
    private int progress;
    private String errorMessage;
    private long startTime;
    private long uploadedBytes;
    private String fileUri;
    private String gameLink; // New field

    // Constructor for new uploads, fileUri and gameLink can be null
    public UploadStatus(String gameName, String fileName, long fileSize, String fileUri, String gameLink) {
        this.gameName = gameName;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.status = Status.UPLOADING;
        this.progress = 0;
        this.startTime = System.currentTimeMillis();
        this.uploadedBytes = 0;
        this.fileUri = fileUri;
        this.gameLink = gameLink;
    }

    // Constructor for restoring from database or full state
    public UploadStatus(int id, String gameName, String fileName, long fileSize, Status status, int progress, String errorMessage, long startTime, long uploadedBytes, String fileUri, String gameLink) {
        this.id = id;
        this.gameName = gameName;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.status = status;
        this.progress = progress;
        this.errorMessage = errorMessage;
        this.startTime = startTime;
        this.uploadedBytes = uploadedBytes;
        this.fileUri = fileUri;
        this.gameLink = gameLink;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFormattedFileSize() {
        return formatBytes(fileSize);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getUploadedBytes() {
        return uploadedBytes;
    }

    public void setUploadedBytes(long uploadedBytes) {
        this.uploadedBytes = uploadedBytes;
    }

    public String getFormattedUploadedBytes() {
        return formatBytes(uploadedBytes);
    }

    public String getFileUri() {
        return fileUri;
    }

    public void setFileUri(String fileUri) {
        this.fileUri = fileUri;
    }

    public String getGameLink() {
        return gameLink;
    }

    public void setGameLink(String gameLink) {
        this.gameLink = gameLink;
    }

    public String getStatusText() {
        switch (status) {
            case UPLOADING:
                return String.format(Locale.getDefault(), "Enviando: %d%%", progress);
            case COMPLETED:
                return "Concluído";
            case ERROR:
                return "Erro: " + (errorMessage != null ? errorMessage : "Desconhecido");
            case PAUSED:
                return "Pausado";
            default:
                return "Desconhecido";
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = ("KMGTPE").charAt(exp-1) + "";
        return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

}

