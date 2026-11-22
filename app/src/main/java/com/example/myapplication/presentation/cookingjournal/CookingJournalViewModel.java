package com.example.myapplication.presentation.cookingjournal;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.example.myapplication.domain.model.CookingLogEntry;
import com.example.myapplication.domain.model.PhotoSaveResult;
import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.domain.repository.PersonalDataRepository;

import java.io.File;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ConcurrentHashMap;

/** Local cooking-session state and actions. All database/file operations use the shared I/O executor. */
public final class CookingJournalViewModel extends ViewModel {
    private final PersonalDataRepository repository;
    private final Executor ioExecutor;
    private final MutableLiveData<JournalState> journalState = new MutableLiveData<>(
            new JournalState(Collections.emptyList(), true, null));
    private final MutableLiveData<MutationResult> mutationResult = new MutableLiveData<>();
    private volatile List<CookingLogEntry> logs = new ArrayList<>();
    private volatile Map<String, Bitmap> photoBitmaps = new ConcurrentHashMap<>();

    public CookingJournalViewModel(PersonalDataRepository repository, Executor ioExecutor) {
        this.repository = repository;
        this.ioExecutor = ioExecutor;
    }

    public LiveData<JournalState> journalState() { return journalState; }
    public LiveData<MutationResult> mutationResult() { return mutationResult; }

    public void refresh() {
        JournalState current = journalState.getValue();
        journalState.setValue(new JournalState(logs, true, null));
        ioExecutor.execute(() -> loadLogs(null));
    }

    private void loadLogs(MutationResult result) {
        try {
            List<CookingLogEntry> loadedLogs = repository.getAllCookingLogs();
            Map<String, Bitmap> loadedPhotos = new HashMap<>();
            for (CookingLogEntry log : loadedLogs) {
                if (log.photoLocalPath == null || log.photoLocalPath.isEmpty()) continue;
                File photo = repository.getCookingLogPhoto(log.photoLocalPath);
                if (photo != null && photo.isFile()) {
                    Bitmap bitmap = decodeThumbnail(photo);
                    if (bitmap != null) loadedPhotos.put(log.photoLocalPath, bitmap);
                }
            }
            logs = new ArrayList<>(loadedLogs);
            photoBitmaps = new ConcurrentHashMap<>(loadedPhotos);
            journalState.postValue(new JournalState(logs, false, null));
            if (result != null) mutationResult.postValue(result);
        } catch (RuntimeException error) {
            journalState.postValue(new JournalState(logs, false, errorMessage(error)));
            if (result != null) mutationResult.postValue(new MutationResult(result.operation, false,
                    errorMessage(error), null));
        }
    }

    public List<CookingLogEntry> getLogs(String recipeId) {
        List<CookingLogEntry> filtered = new ArrayList<>();
        for (CookingLogEntry log : logs) if (recipeId != null && recipeId.equals(log.recipeId)) filtered.add(log);
        return filtered;
    }

    public List<CookingLogEntry> getAllLogs() { return new ArrayList<>(logs); }

    public List<CookingLogEntry> filterLogs(List<CookingLogEntry> source, String query,
                                             long fromInclusive, long toExclusive, int selectedRating) {
        String normalizedQuery = normalize(query);
        List<CookingLogEntry> matches = new ArrayList<>();
        for (CookingLogEntry log : source) {
            if (fromInclusive > 0 && log.cookedAt < fromInclusive) continue;
            if (toExclusive > 0 && log.cookedAt >= toExclusive) continue;
            if (selectedRating > 0 && (log.rating == null || log.rating != selectedRating)) continue;
            if (!normalizedQuery.isEmpty()
                    && !normalize(log.title + " " + log.adjustmentNote).contains(normalizedQuery)) continue;
            matches.add(log);
        }
        return matches;
    }

    public JournalStats getStats() {
        if (logs.isEmpty()) return new JournalStats("—", 0, null, null);
        LinkedHashMap<String, Integer> recipeCounts = new LinkedHashMap<>();
        String mostCookedRecipe = "—";
        int mostCookedCount = 0;
        Integer latestRating = null;
        for (CookingLogEntry log : logs) {
            int count = recipeCounts.containsKey(log.recipeId) ? recipeCounts.get(log.recipeId) + 1 : 1;
            recipeCounts.put(log.recipeId, count);
            if (count > mostCookedCount) {
                mostCookedCount = count;
                mostCookedRecipe = log.title;
            }
            if (latestRating == null && log.rating != null) latestRating = log.rating;
        }
        return new JournalStats(mostCookedRecipe, mostCookedCount, logs.get(0).cookedAt, latestRating);
    }

    private String normalize(String value) {
        String decomposed = Normalizer.normalize(value == null ? "" : value.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replace('đ', 'd');
        return decomposed.replaceAll("\\s+", " ").trim();
    }

    public static final class JournalStats {
        public final String mostCookedRecipe;
        public final int mostCookedCount;
        public final Long lastCookedAt;
        public final Integer latestRating;
        public JournalStats(String recipe, int count, Long lastCookedAt, Integer latestRating) {
            this.mostCookedRecipe = recipe;
            this.mostCookedCount = count;
            this.lastCookedAt = lastCookedAt;
            this.latestRating = latestRating;
        }
    }

    public CookingLogEntry getLog(String id) {
        if (id == null) return null;
        for (CookingLogEntry log : logs) if (id.equals(log.id)) return log;
        return null;
    }

    public Bitmap getPhotoBitmap(String filename) { return filename == null ? null : photoBitmaps.get(filename); }

    public boolean save(String id, Recipe recipe, long cookedAt, int servings, Integer duration,
                        Integer rating, String adjustment, String photoPath) {
        if (recipe == null || cookedAt <= 0 || servings < 1 || servings > 999
                || (duration != null && duration < 1) || (rating != null && (rating < 1 || rating > 5))) return false;
        journalState.setValue(new JournalState(logs, true, null));
        ioExecutor.execute(() -> {
            try {
                boolean saved = repository.saveCookingLog(id, recipe, cookedAt, servings,
                        duration, rating, adjustment, photoPath);
                loadLogs(new MutationResult(MutationResult.SAVE, saved,
                        saved ? null : "Không thể lưu lần nấu. Vui lòng kiểm tra thông tin.", null));
            } catch (RuntimeException error) {
                mutationResult.postValue(new MutationResult(MutationResult.SAVE, false, errorMessage(error), null));
            }
        });
        return true;
    }

    public void delete(String id) {
        if (id == null) return;
        List<CookingLogEntry> previous = new ArrayList<>(logs);
        List<CookingLogEntry> updated = new ArrayList<>(previous);
        updated.removeIf(log -> id.equals(log.id));
        logs = updated;
        journalState.setValue(new JournalState(logs, false, null));
        ioExecutor.execute(() -> {
            try {
                boolean deleted = repository.deleteCookingLog(id);
                if (!deleted) logs = previous;
                loadLogs(new MutationResult(MutationResult.DELETE, deleted,
                        deleted ? null : "Không tìm thấy lần nấu để xóa.", null));
            } catch (RuntimeException error) {
                logs = previous;
                journalState.postValue(new JournalState(logs, false, errorMessage(error)));
                mutationResult.postValue(new MutationResult(MutationResult.DELETE, false, errorMessage(error), null));
            }
        });
    }

    public void savePhotoAsync(String uriString) {
        ioExecutor.execute(() -> {
            try {
                PhotoSaveResult result = repository.saveCookingLogPhoto(uriString);
                if (result.filename != null) {
                    File photo = repository.getCookingLogPhoto(result.filename);
                    Bitmap bitmap = photo == null ? null : decodeThumbnail(photo);
                    if (bitmap != null) photoBitmaps.put(result.filename, bitmap);
                }
                mutationResult.postValue(new MutationResult(MutationResult.PHOTO, result.filename != null,
                        result.filename == null ? (result.tooLarge ? "Ảnh cần nhỏ hơn 10 MB." : "Không thể lưu ảnh đã chọn.") : null,
                        result.filename));
            } catch (RuntimeException error) {
                mutationResult.postValue(new MutationResult(MutationResult.PHOTO, false, errorMessage(error), null));
            }
        });
    }

    public void deletePhoto(String filename) {
        if (filename == null || filename.isEmpty()) return;
        ioExecutor.execute(() -> {
            try {
                repository.deleteCookingLogPhoto(filename);
                photoBitmaps.remove(filename);
            } catch (RuntimeException error) {
                mutationResult.postValue(new MutationResult(MutationResult.PHOTO, false, errorMessage(error), null));
            }
        });
    }

    public void clearMutationResult() { mutationResult.setValue(null); }

    private static String errorMessage(RuntimeException error) {
        return error.getMessage() == null ? "Không thể cập nhật nhật ký nấu." : error.getMessage();
    }

    private static Bitmap decodeThumbnail(File file) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        int sampleSize = 1;
        int longestSide = Math.max(bounds.outWidth, bounds.outHeight);
        while (longestSide / sampleSize > 512) sampleSize *= 2;
        options.inSampleSize = sampleSize;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }

    public static final class JournalState {
        public final List<CookingLogEntry> logs;
        public final boolean loading;
        public final String error;
        JournalState(List<CookingLogEntry> logs, boolean loading, String error) {
            this.logs = Collections.unmodifiableList(new ArrayList<>(logs));
            this.loading = loading;
            this.error = error;
        }
    }

    public static final class MutationResult {
        public static final String SAVE = "SAVE", DELETE = "DELETE", PHOTO = "PHOTO";
        public final String operation;
        public final boolean success;
        public final String message;
        public final String filename;
        MutationResult(String operation, boolean success, String message, String filename) {
            this.operation = operation;
            this.success = success;
            this.message = message;
            this.filename = filename;
        }
    }
}
