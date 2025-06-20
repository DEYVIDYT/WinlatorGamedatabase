package com.winlator.Download.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SQLiteHelper extends SQLiteOpenHelper {
    private static final String TAG = "SQLiteHelper";

    public static final int DATABASE_VERSION = 4; // Incremented database version
    public static final String DATABASE_NAME = "WinlatorDownloads.db";

    private static final String SQL_CREATE_UPLOADS_ENTRIES =
            "CREATE TABLE " + UploadContract.UploadEntry.TABLE_NAME + " (" +
                    UploadContract.UploadEntry._ID + " INTEGER PRIMARY KEY," +
                    UploadContract.UploadEntry.COLUMN_NAME_GAME_NAME + " TEXT," +
                    UploadContract.UploadEntry.COLUMN_NAME_FILE_NAME + " TEXT," +
                    UploadContract.UploadEntry.COLUMN_NAME_FILE_SIZE + " INTEGER," +
                    UploadContract.UploadEntry.COLUMN_NAME_STATUS + " TEXT," +
                    UploadContract.UploadEntry.COLUMN_NAME_PROGRESS + " INTEGER," +
                    UploadContract.UploadEntry.COLUMN_NAME_ERROR_MESSAGE + " TEXT," +
                    UploadContract.UploadEntry.COLUMN_NAME_START_TIME + " INTEGER," +
                    UploadContract.UploadEntry.COLUMN_NAME_UPLOADED_BYTES + " INTEGER," +
                    UploadContract.UploadEntry.COLUMN_NAME_FILE_URI + " TEXT," +
                    UploadContract.UploadEntry.COLUMN_NAME_GAME_LINK + " TEXT)";
                    // Removed UploadContract.UploadEntry.COLUMN_NAME_ACCESS_KEY,
                    // UploadContract.UploadEntry.COLUMN_NAME_SECRET_KEY,
                    // UploadContract.UploadEntry.COLUMN_NAME_ITEM_IDENTIFIER

    private static final String SQL_DELETE_UPLOADS_ENTRIES =
            "DROP TABLE IF EXISTS " + UploadContract.UploadEntry.TABLE_NAME;

    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating database table: " + DownloadContract.SQL_CREATE_ENTRIES);
        db.execSQL(DownloadContract.SQL_CREATE_ENTRIES);
        Log.d(TAG, "Creating database table: " + SQL_CREATE_UPLOADS_ENTRIES);
        db.execSQL(SQL_CREATE_UPLOADS_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
        db.execSQL(DownloadContract.SQL_DELETE_ENTRIES);
        db.execSQL(SQL_DELETE_UPLOADS_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}

