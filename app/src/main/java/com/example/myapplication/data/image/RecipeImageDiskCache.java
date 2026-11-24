package com.example.myapplication.data.image;

import android.system.ErrnoException;
import android.system.Os;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

/** Stores only validated encoded recipe images. Called by the image worker. */
final class RecipeImageDiskCache {
    private final File directory;
    private final long limit;
    private boolean prepared;

    RecipeImageDiskCache(File directory, long limit) {
        this.directory = directory;
        this.limit = limit;
    }

    File entry(String key) {
        if (!prepared) prepare();
        return new File(directory, key + ".image");
    }

    private void prepare() {
        File[] partials = directory.listFiles((dir, name) -> name.endsWith(".partial"));
        if (partials != null) for (File partial : partials) partial.delete();
        evict(null);
        prepared = true;
    }

    void store(File target, byte[] bytes) throws IOException {
        if (bytes.length > limit) return;
        if (!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory()) {
            throw new IOException("recipe image cache is unavailable");
        }
        File partial = File.createTempFile("recipe-", ".partial", directory);
        try {
            try (FileOutputStream output = new FileOutputStream(partial)) {
                output.write(bytes);
                output.getFD().sync();
            }
            try {
                Os.rename(partial.getAbsolutePath(), target.getAbsolutePath());
            } catch (ErrnoException error) {
                throw new IOException("cannot publish recipe image cache entry", error);
            }
            evict(target);
        } finally {
            partial.delete();
        }
    }

    private void evict(File newest) {
        File[] entries = directory.listFiles((dir, name) -> name.endsWith(".image"));
        if (entries == null) return;
        long total = 0;
        for (File entry : entries) total += entry.length();
        if (total <= limit) return;
        Arrays.sort(entries, Comparator.comparing((File entry) -> entry.equals(newest))
                .thenComparingLong(File::lastModified).thenComparing(File::getName));
        for (File entry : entries) {
            if (total <= limit) break;
            long bytes = entry.length();
            if (entry.delete()) total -= bytes;
        }
    }
}
