package com.winlator.Download.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.winlator.Download.model.GameEntry;
import java.util.ArrayList;
import java.util.List;

public class GamesDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "games.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_GAMES = "games";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_DESKTOP_PATH = "desktop_path";
    public static final String COLUMN_BANNER_PATH = "banner_path";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_GAMES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TITLE + " TEXT NOT NULL, " +
                    COLUMN_DESKTOP_PATH + " TEXT NOT NULL, " +
                    COLUMN_BANNER_PATH + " TEXT);";

    public GamesDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GAMES);
        onCreate(db);
    }

    public long addGame(GameEntry game) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, game.getTitle());
        values.put(COLUMN_DESKTOP_PATH, game.getDesktopFilePath());
        values.put(COLUMN_BANNER_PATH, game.getBannerImagePath());

        long id = db.insert(TABLE_GAMES, null, values);
        db.close();
        return id;
    }

    public List<GameEntry> getAllGames() {
        List<GameEntry> games = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_GAMES, null, null, null, null, null, COLUMN_TITLE + " ASC");

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                String desktopPath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESKTOP_PATH));
                String bannerPath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BANNER_PATH));
                games.add(new GameEntry(id, title, desktopPath, bannerPath));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return games;
    }

    // Optional: updateGame method
    public int updateGame(GameEntry game) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, game.getTitle());
        values.put(COLUMN_DESKTOP_PATH, game.getDesktopFilePath());
        values.put(COLUMN_BANNER_PATH, game.getBannerImagePath());

        int rowsAffected = db.update(TABLE_GAMES, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(game.getId())});
        db.close();
        return rowsAffected;
    }

    // Optional: deleteGame method
    public void deleteGame(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_GAMES, COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
    }
}
