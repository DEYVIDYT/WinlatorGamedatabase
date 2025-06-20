package com.winlator.Download.service;

import android.util.Log; // For logging in sanitize, if needed
import java.text.Normalizer; // For NFKD normalization
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleDriveUtils {

    private static final String TAG = "GoogleDriveUtils";

    // Patterns from the Python script to extract Google Drive File/Folder IDs
    // Order might matter if some patterns are more specific or general
    private static final Pattern[] ID_PATTERNS = {
        Pattern.compile("/file/d/([0-9A-Za-z_-]{10,})(?:/|$)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("/folders/([0-9A-Za-z_-]{10,})(?:/|$)", Pattern.CASE_INSENSITIVE),
        // Pattern for /uc?id= or /open?id=
        Pattern.compile("id=([0-9A-Za-z_-]{10,})(?:&|$)", Pattern.CASE_INSENSITIVE),
        // A more general pattern for IDs if they appear directly, less specific
        // This one should be used carefully, perhaps last, as it might match unintended strings.
        // The Python script has it, so including for completeness, but its usage context matters.
        // For typical shared URLs, the /file/d/ or id= patterns are more common.
        // For now, let's only include the more specific ones if this general one causes issues.
        // After review, the Python script seems to use this to find IDs in folder listings (<tr><td><a href="/open?id=...">)
        // For now, let's stick to the most common URL patterns for direct link ID extraction.
        // The Python script's fourth pattern "([0-9A-Za-z_-]{10,})" is very broad.
        // Let's limit to the first three which are common in share links.
        // If folder parsing is added later, the broader one might be useful in that specific context.
    };
    
    // More specific patterns for typical share links
     private static final Pattern[] SHARE_LINK_ID_PATTERNS = {
        Pattern.compile("/file/d/([0-9A-Za-z_-]{10,})(?:/|$)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("/folders/([0-9A-Za-z_-]{10,})(?:/|$)", Pattern.CASE_INSENSITIVE), // Though folders are future
        Pattern.compile("id=([0-9A-Za-z_-]{10,})(?:&|$)", Pattern.CASE_INSENSITIVE)
    };


    /**
     * Extracts the Google Drive file or folder ID from various URL formats.
     * @param url The Google Drive URL.
     * @return The extracted ID, or null if no ID is found.
     */
    public static String extractDriveId(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        for (Pattern pattern : SHARE_LINK_ID_PATTERNS) { // Using the more specific set for now
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        Log.w(TAG, "Could not extract Google Drive ID from URL: " + url);
        return null;
    }

    // Filename sanitization constants from Python script
    private static final Set<Character> FILENAME_BLACKLIST_CHARS = new HashSet<>(Arrays.asList(
        '\\', '/', ':', '*', '?', '"', '<', '>', '|', ' '
    ));
    private static final Set<String> FILENAME_RESERVED_NAMES = new HashSet<>(Arrays.asList(
        "CON", "PRN", "AUX", "NUL",
        "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    ));
    private static final int MAX_FILENAME_LENGTH = 255;


    /**
     * Sanitizes a filename to remove/replace illegal characters and ensure it's valid for most filesystems.
     * Based on the Python script's sanitize function.
     * @param filename The original filename.
     * @return A sanitized filename.
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "_"; // Default for empty filename
        }

        // HTML unescape - Android doesn't have a direct equivalent of Python's html.unescape easily available
        // For common cases like &amp; &lt; &gt; &quot; &apos;
        // A simple replacement can be done, or a more comprehensive library if needed.
        // For now, let's do common ones. TextUtils.htmlEncode / htmlDecode is more for display.
        // A full unescape might be complex. The Python script uses `html.unescape`.
        // We will skip full HTML unescaping for now as it's complex and focus on other rules.
        // If filenames truly contain HTML entities, this might need a library or more robust unescaping.

        // Normalize (NFKD form) - Java's Normalizer can do this
        String normalizedFilename = Normalizer.normalize(filename, Normalizer.Form.NFKD);

        StringBuilder sb = new StringBuilder();
        for (char c : normalizedFilename.toCharArray()) {
            // Remove blacklist characters and control characters (0-31)
            if (!FILENAME_BLACKLIST_CHARS.contains(c) && (int) c > 31) {
                sb.append(c);
            }
        }
        String currentFilename = sb.toString();

        // Strip trailing dots and spaces (Java's trim() handles leading/trailing spaces)
        currentFilename = currentFilename.trim();
        while (currentFilename.endsWith(".")) {
            currentFilename = currentFilename.substring(0, currentFilename.length() - 1);
        }
        currentFilename = currentFilename.trim(); // Trim again after removing dots

        // If all characters were dots (e.g. "...")
        if (currentFilename.replace(".", "").isEmpty() && !currentFilename.isEmpty()) {
             currentFilename = "_" + currentFilename;
        }
        
        // If filename is one of the reserved names (case-insensitive check for safety on Windows)
        if (FILENAME_RESERVED_NAMES.contains(currentFilename.toUpperCase())) {
            currentFilename = "_" + currentFilename;
        }

        if (currentFilename.isEmpty()) {
            currentFilename = "_"; // Default if everything was stripped
        }

        // Length check (very simplified for now, Python version is more complex with extension handling)
        if (currentFilename.length() > MAX_FILENAME_LENGTH) {
            // Basic truncation:
            // A more sophisticated approach would try to preserve the extension.
            String ext = "";
            int lastDot = currentFilename.lastIndexOf('.');
            if (lastDot > 0 && lastDot < currentFilename.length() - 1) { // Check if dot is not first or last
                ext = currentFilename.substring(lastDot);
                // Ensure extension itself is not overly long (Python script has more detailed logic here)
                if (ext.length() > MAX_FILENAME_LENGTH -1) { // e.g. ".verylongextension"
                    ext = ext.substring(0, Math.min(ext.length(), MAX_FILENAME_LENGTH -1 - 10)) + ".."; // Truncate ext too
                }
                 String baseName = currentFilename.substring(0, lastDot);
                 int maxBaseNameLength = MAX_FILENAME_LENGTH - ext.length();
                 if (baseName.length() > maxBaseNameLength) {
                     baseName = baseName.substring(0, maxBaseNameLength);
                 }
                 currentFilename = baseName + ext;

            } else { // No extension or dot is at the start
                 currentFilename = currentFilename.substring(0, MAX_FILENAME_LENGTH);
            }
            
            // Final strip of trailing dots/spaces after potential truncation
            currentFilename = currentFilename.trim();
            while (currentFilename.endsWith(".")) {
                currentFilename = currentFilename.substring(0, currentFilename.length() - 1);
            }
            currentFilename = currentFilename.trim();
             if (currentFilename.isEmpty()) { // If truncation resulted in empty
                currentFilename = "_";
            }
        }
        return currentFilename;
    }
}
