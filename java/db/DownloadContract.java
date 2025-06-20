
package com.winlator.Download.db;

import android.provider.BaseColumns;

public final class DownloadContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private DownloadContract() {}

    /* Inner class that defines the table contents */
    public static class DownloadEntry implements BaseColumns {
        public static final String TABLE_NAME = "downloads";
        public static final String COLUMN_NAME_URL = "url";
        public static final String COLUMN_NAME_FILE_NAME = "file_name";
        public static final String COLUMN_NAME_LOCAL_PATH = "local_path";
        public static final String COLUMN_NAME_TOTAL_BYTES = "total_bytes";
        public static final String COLUMN_NAME_DOWNLOADED_BYTES = "downloaded_bytes";
        public static final String COLUMN_NAME_STATUS = "status"; // e.g., PENDING, DOWNLOADING, COMPLETED, FAILED
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    }

    // Define status constants
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_DOWNLOADING = 1;
    public static final int STATUS_COMPLETED = 2;
    public static final int STATUS_FAILED = 3;
    public static final int STATUS_PAUSED = 4; // Optional for future resume functionality

    // SQL query to create the table
    public static final String SQL_CREATE_ENTRIES = 
        "CREATE TABLE " + DownloadEntry.TABLE_NAME + " (" +
        DownloadEntry._ID + " INTEGER PRIMARY KEY," +
        DownloadEntry.COLUMN_NAME_URL + " TEXT UNIQUE," + // URL should be unique
        DownloadEntry.COLUMN_NAME_FILE_NAME + " TEXT," +
        DownloadEntry.COLUMN_NAME_LOCAL_PATH + " TEXT," +
        DownloadEntry.COLUMN_NAME_TOTAL_BYTES + " INTEGER," +
        DownloadEntry.COLUMN_NAME_DOWNLOADED_BYTES + " INTEGER," +
        DownloadEntry.COLUMN_NAME_STATUS + " INTEGER," +
        DownloadEntry.COLUMN_NAME_TIMESTAMP + " INTEGER)";

    // SQL query to delete the table
    public static final String SQL_DELETE_ENTRIES = 
        "DROP TABLE IF EXISTS " + DownloadEntry.TABLE_NAME;
}

