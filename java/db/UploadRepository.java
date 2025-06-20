package com.winlator.Download.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.winlator.Download.model.UploadStatus;

import java.util.ArrayList;
import java.util.List;

public class UploadRepository {
    private SQLiteHelper dbHelper;

    public UploadRepository(Context context) {
        dbHelper = new SQLiteHelper(context);
    }

    public long insertUpload(UploadStatus uploadStatus) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UploadContract.UploadEntry.COLUMN_NAME_GAME_NAME, uploadStatus.getGameName());
        values.put(UploadContract.UploadEntry.COLUMN_NAME_FILE_NAME, uploadStatus.getFileName());
        values.put(UploadContract.UploadEntry.COLUMN_NAME_FILE_SIZE, uploadStatus.getFileSize());
        values.put(UploadContract.UploadEntry.COLUMN_NAME_STATUS, uploadStatus.getStatus().name());
        values.put(UploadContract.UploadEntry.COLUMN_NAME_PROGRESS, uploadStatus.getProgress());
        values.put(UploadContract.UploadEntry.COLUMN_NAME_ERROR_MESSAGE, uploadStatus.getErrorMessage());
        values.put(UploadContract.UploadEntry.COLUMN_NAME_START_TIME, uploadStatus.getStartTime());
        values.put(UploadContract.UploadEntry.COLUMN_NAME_UPLOADED_BYTES, uploadStatus.getUploadedBytes());
        values.put(UploadContract.UploadEntry.COLUMN_NAME_FILE_URI, uploadStatus.getFileUri());
        values.put(UploadContract.UploadEntry.COLUMN_NAME_GAME_LINK, uploadStatus.getGameLink());

        long newRowId = db.insert(UploadContract.UploadEntry.TABLE_NAME, null, values);
        db.close();
        return newRowId;
    }

    public int updateUpload(UploadStatus uploadStatus) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UploadContract.UploadEntry.COLUMN_NAME_GAME_NAME, uploadStatus.getGameName());
        values.put(UploadContract.UploadEntry.COLUMN_NAME_FILE_NAME, uploadStatus.getFileName());
        values.put(UploadContract.UploadEntry.COLUMN_NAME_FILE_SIZE, uploadStatus.getFileSize());
        values.put(UploadContract.UploadEntry.COLUMN_NAME_STATUS, uploadStatus.getStatus().name());
        values.put(UploadContract.UploadEntry.COLUMN_NAME_PROGRESS, uploadStatus.getProgress());
        values.put(UploadContract.UploadEntry.COLUMN_NAME_ERROR_MESSAGE, uploadStatus.getErrorMessage());
        values.put(UploadContract.UploadEntry.COLUMN_NAME_START_TIME, uploadStatus.getStartTime());
        values.put(UploadContract.UploadEntry.COLUMN_NAME_UPLOADED_BYTES, uploadStatus.getUploadedBytes());
        values.put(UploadContract.UploadEntry.COLUMN_NAME_FILE_URI, uploadStatus.getFileUri());
        values.put(UploadContract.UploadEntry.COLUMN_NAME_GAME_LINK, uploadStatus.getGameLink());

        String selection = UploadContract.UploadEntry._ID + " = ?";
        String[] selectionArgs = { String.valueOf(uploadStatus.getId()) };

        int count = db.update(
                UploadContract.UploadEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);
        db.close();
        return count;
    }

    public List<UploadStatus> getAllUploads() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<UploadStatus> uploads = new ArrayList<>();

        String[] projection = {
                UploadContract.UploadEntry._ID,
                UploadContract.UploadEntry.COLUMN_NAME_GAME_NAME,
                UploadContract.UploadEntry.COLUMN_NAME_FILE_NAME,
                UploadContract.UploadEntry.COLUMN_NAME_FILE_SIZE,
                UploadContract.UploadEntry.COLUMN_NAME_STATUS,
                UploadContract.UploadEntry.COLUMN_NAME_PROGRESS,
                UploadContract.UploadEntry.COLUMN_NAME_ERROR_MESSAGE,
                UploadContract.UploadEntry.COLUMN_NAME_START_TIME,
                UploadContract.UploadEntry.COLUMN_NAME_UPLOADED_BYTES,
                UploadContract.UploadEntry.COLUMN_NAME_FILE_URI,
                UploadContract.UploadEntry.COLUMN_NAME_GAME_LINK
        };

        Cursor cursor = db.query(
                UploadContract.UploadEntry.TABLE_NAME,
                projection,
                null, // selection
                null, // selectionArgs
                null, // groupBy
                null, // having
                UploadContract.UploadEntry.COLUMN_NAME_START_TIME + " DESC"  // orderBy
        );

        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(UploadContract.UploadEntry._ID));
            String gameName = cursor.getString(cursor.getColumnIndexOrThrow(UploadContract.UploadEntry.COLUMN_NAME_GAME_NAME));
            String fileName = cursor.getString(cursor.getColumnIndexOrThrow(UploadContract.UploadEntry.COLUMN_NAME_FILE_NAME));
            long fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(UploadContract.UploadEntry.COLUMN_NAME_FILE_SIZE));
            UploadStatus.Status status = UploadStatus.Status.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(UploadContract.UploadEntry.COLUMN_NAME_STATUS)));
            int progress = cursor.getInt(cursor.getColumnIndexOrThrow(UploadContract.UploadEntry.COLUMN_NAME_PROGRESS));
            String errorMessage = cursor.getString(cursor.getColumnIndexOrThrow(UploadContract.UploadEntry.COLUMN_NAME_ERROR_MESSAGE));
            long startTime = cursor.getLong(cursor.getColumnIndexOrThrow(UploadContract.UploadEntry.COLUMN_NAME_START_TIME));
            long uploadedBytes = cursor.getLong(cursor.getColumnIndexOrThrow(UploadContract.UploadEntry.COLUMN_NAME_UPLOADED_BYTES));
            String fileUri = cursor.getString(cursor.getColumnIndexOrThrow(UploadContract.UploadEntry.COLUMN_NAME_FILE_URI));
            String gameLink = cursor.getString(cursor.getColumnIndexOrThrow(UploadContract.UploadEntry.COLUMN_NAME_GAME_LINK));

            uploads.add(new UploadStatus(id, gameName, fileName, fileSize, status, progress, errorMessage, startTime, uploadedBytes, fileUri, gameLink));
        }
        cursor.close();
        db.close();
        return uploads;
    }

    public void deleteUpload(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = UploadContract.UploadEntry._ID + " = ?";
        String[] selectionArgs = { String.valueOf(id) };
        db.delete(UploadContract.UploadEntry.TABLE_NAME, selection, selectionArgs);
        db.close();
    }

    public UploadStatus getUploadById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        UploadStatus uploadStatus = null;

        String[] projection = {
                UploadContract.UploadEntry._ID,
                UploadContract.UploadEntry.COLUMN_NAME_GAME_NAME,
                UploadContract.UploadEntry.COLUMN_NAME_FILE_NAME,
                UploadContract.UploadEntry.COLUMN_NAME_FILE_SIZE,
                UploadContract.UploadEntry.COLUMN_NAME_STATUS,
                UploadContract.UploadEntry.COLUMN_NAME_PROGRESS,
                UploadContract.UploadEntry.COLUMN_NAME_ERROR_MESSAGE,
                UploadContract.UploadEntry.COLUMN_NAME_START_TIME,
                UploadContract.UploadEntry.COLUMN_NAME_UPLOADED_BYTES,
                UploadContract.UploadEntry.COLUMN_NAME_FILE_URI,
                UploadContract.UploadEntry.COLUMN_NAME_GAME_LINK
        };

        String selection = UploadContract.UploadEntry._ID + " = ?";
        String[] selectionArgs = { String.valueOf(id) };

        Cursor cursor = db.query(
                UploadContract.UploadEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        if (cursor.moveToFirst()) {
            String gameName = cursor.getString(cursor.getColumnIndexOrThrow(UploadContract.UploadEntry.COLUMN_NAME_GAME_NAME));
            String fileName = cursor.getString(cursor.getColumnIndexOrThrow(UploadContract.UploadEntry.COLUMN_NAME_FILE_NAME));
            long fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(UploadContract.UploadEntry.COLUMN_NAME_FILE_SIZE));
            UploadStatus.Status status = UploadStatus.Status.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(UploadContract.UploadEntry.COLUMN_NAME_STATUS)));
            int progress = cursor.getInt(cursor.getColumnIndexOrThrow(UploadContract.UploadEntry.COLUMN_NAME_PROGRESS));
            String errorMessage = cursor.getString(cursor.getColumnIndexOrThrow(UploadContract.UploadEntry.COLUMN_NAME_ERROR_MESSAGE));
            long startTime = cursor.getLong(cursor.getColumnIndexOrThrow(UploadContract.UploadEntry.COLUMN_NAME_START_TIME));
            long uploadedBytes = cursor.getLong(cursor.getColumnIndexOrThrow(UploadContract.UploadEntry.COLUMN_NAME_UPLOADED_BYTES));
            String fileUri = cursor.getString(cursor.getColumnIndexOrThrow(UploadContract.UploadEntry.COLUMN_NAME_FILE_URI));
            String gameLink = cursor.getString(cursor.getColumnIndexOrThrow(UploadContract.UploadEntry.COLUMN_NAME_GAME_LINK));

            uploadStatus = new UploadStatus(id, gameName, fileName, fileSize, status, progress, errorMessage, startTime, uploadedBytes, fileUri, gameLink);
        }
        cursor.close();
        db.close();
        return uploadStatus;
    }
}

