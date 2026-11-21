package com.example.myapplication;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/** Private, device-local image storage for cooking journal entries. */
final class CookingJournalPhotoStore {
    private static final long MAX_PHOTO_BYTES = 10L * 1024 * 1024;
    private final Context context;

    CookingJournalPhotoStore(Context context) {
        this.context = context.getApplicationContext();
    }

    CopyResult copy(Uri source) {
        File directory = new File(context.getFilesDir(), "cooking-log-images");
        if (!directory.exists() && !directory.mkdirs()) return new CopyResult(null, false);
        String filename = UUID.randomUUID().toString() + ".img";
        File target = new File(directory, filename);
        long copied = 0;
        boolean tooLarge = false;
        try (InputStream input = context.getContentResolver().openInputStream(source);
             FileOutputStream output = new FileOutputStream(target)) {
            if (input == null) {
                target.delete();
                return new CopyResult(null, false);
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                copied += read;
                if (copied > MAX_PHOTO_BYTES) {
                    tooLarge = true;
                    break;
                }
                output.write(buffer, 0, read);
            }
            output.flush();
        } catch (IOException error) {
            target.delete();
            return new CopyResult(null, false);
        }
        if (tooLarge || copied == 0) {
            target.delete();
            return new CopyResult(null, tooLarge);
        }
        return new CopyResult(filename, false);
    }

    File resolve(String filename) {
        if (filename == null || filename.isEmpty() || filename.contains("/") || filename.contains("\\")) return null;
        File directory = new File(context.getFilesDir(), "cooking-log-images");
        File file = new File(directory, filename);
        try {
            return file.getCanonicalPath().startsWith(directory.getCanonicalPath() + File.separator) ? file : null;
        } catch (IOException error) {
            return null;
        }
    }

    void delete(String filename) {
        File file = resolve(filename);
        if (file != null && file.exists()) file.delete();
    }

    static final class CopyResult {
        final String filename;
        final boolean tooLarge;
        CopyResult(String filename, boolean tooLarge) {
            this.filename = filename;
            this.tooLarge = tooLarge;
        }
    }
}
