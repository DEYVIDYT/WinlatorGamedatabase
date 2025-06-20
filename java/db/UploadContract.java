package com.winlator.Download.db;

import android.provider.BaseColumns;

public final class UploadContract {
    private UploadContract() {}

    public static class UploadEntry implements BaseColumns {
        public static final String TABLE_NAME = "uploads";
        public static final String COLUMN_NAME_GAME_NAME = "game_name";
        public static final String COLUMN_NAME_FILE_NAME = "file_name";
        public static final String COLUMN_NAME_FILE_SIZE = "file_size";
        public static final String COLUMN_NAME_STATUS = "status";
        public static final String COLUMN_NAME_PROGRESS = "progress";
        public static final String COLUMN_NAME_ERROR_MESSAGE = "error_message";
        public static final String COLUMN_NAME_START_TIME = "start_time";
        public static final String COLUMN_NAME_UPLOADED_BYTES = "uploaded_bytes";
        public static final String COLUMN_NAME_FILE_URI = "file_uri";
        public static final String COLUMN_NAME_GAME_LINK = "game_link";
    }
}

