package com.winlator.Download.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class FilePathUtil {
    private static final String TAG = "FilePathUtil";

    public static String getPathFromUri(final Context context, final Uri uri) {
        if (uri == null) {
            Log.w(TAG, "Input URI is null");
            return null;
        }

        final ContentResolver resolver = context.getContentResolver();
        if (resolver == null) {
            Log.e(TAG, "ContentResolver is null");
            return null;
        }

        // For file:// Uris
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            return uri.getPath();
        }

        // For content:// Uris
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            Cursor cursor = null;
            try {
                // Try to get path directly from MediaStore for common media types
                // This might not work for all URIs, especially those from DocumentProviders
                String[] projection = {MediaStore.Files.FileColumns.DATA};
                cursor = resolver.query(uri, projection, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
                    // Check if the column exists. Some providers might not have it.
                    if (columnIndex != -1) {
                        String path = cursor.getString(columnIndex);
                        if (!TextUtils.isEmpty(path)) {
                            Log.d(TAG, "Path obtained from MediaStore: " + path);
                            return path;
                        }
                    } else {
                        Log.w(TAG, "MediaStore.Files.FileColumns.DATA column not found for URI: " + uri);
                    }
                }
            } catch (Exception e) {
                // This can happen if the URI is not from MediaStore or column doesn't exist
                Log.w(TAG, "Could not get path from MediaStore for URI: " + uri + " - " + e.getMessage());
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            // Fallback: Try to copy the file to app's cache directory and return path to cached file
            String fileName = getFileName(context, uri);
            if (fileName != null) {
                File cacheDir = context.getCacheDir();
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs();
                }
                File file = new File(cacheDir, fileName);
                try (InputStream inputStream = resolver.openInputStream(uri);
                     OutputStream outputStream = new FileOutputStream(file)) {
                    if (inputStream == null) {
                        Log.e(TAG, "InputStream is null for URI: " + uri);
                        return null;
                    }
                    byte[] buffer = new byte[4 * 1024]; // 4K buffer
                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, read);
                    }
                    outputStream.flush();
                    Log.d(TAG, "File copied to cache: " + file.getAbsolutePath());
                    return file.getAbsolutePath();
                } catch (Exception e) {
                    Log.e(TAG, "Error copying file to cache for URI: " + uri + " - " + e.getMessage());
                }
            } else {
                Log.e(TAG, "Could not get file name for URI: " + uri);
            }
        }
        Log.w(TAG, "Could not determine path for URI: " + uri);
        return null; // Could not determine path
    }

    private static String getFileName(Context context, Uri uri) {
        String result = null;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                       result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
               Log.w(TAG, "Error getting file name from ContentResolver: " + e.getMessage());
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        // Sanitize filename to prevent path traversal or invalid characters
        if (result != null) {
           result = result.replaceAll("[^a-zA-Z0-9._-]", "_");
           // Ensure it's not excessively long
           if (result.length() > 255) {
               result = result.substring(result.length() - 255);
           }
        }
        return result;
    }
}
