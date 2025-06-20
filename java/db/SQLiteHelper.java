package com.winlator.Download.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

// import com.winlator.Download.db.UploadContract; // Removed

public class SQLiteHelper extends SQLiteOpenHelper {
    private static final String TAG = "SQLiteHelper";

    public static final int DATABASE_VERSION = 4; // Incremented database version
    // Consider if DATABASE_VERSION needs to change if schema changes.
    // If only removing a table, existing users might not need an "upgrade" that drops other tables.
    // However, the current onUpgrade drops all tables and recreates.
    // For simplicity of this step, we'll keep version 4. If issues arise, version management might need review.
    public static final String DATABASE_NAME = "WinlatorDownloads.db";

    // SQL_CREATE_UPLOADS_ENTRIES removed
    // SQL_DELETE_UPLOADS_ENTRIES removed

    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating database table: " + DownloadContract.SQL_CREATE_ENTRIES);
        db.execSQL(DownloadContract.SQL_CREATE_ENTRIES);
        // Log.d(TAG, "Creating database table: " + SQL_CREATE_UPLOADS_ENTRIES); // Removed
        // db.execSQL(SQL_CREATE_UPLOADS_ENTRIES); // Removed
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
        db.execSQL(DownloadContract.SQL_DELETE_ENTRIES);
        // db.execSQL(SQL_DELETE_UPLOADS_ENTRIES); // Removed
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}

