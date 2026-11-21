package com.example.myapplication;

import android.app.Application;
import android.net.Uri;

import java.io.File;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

/** Local cooking-session history linked to the original recipe. */
public final class CookingJournalViewModel extends AndroidViewModel {
    private final PersonalDataRepository repository;

    public CookingJournalViewModel(@NonNull Application application) {
        super(application);
        repository = PersonalDataRepository.getInstance(application);
    }

    List<PersonalDataDbHelper.CookingLog> getLogs(String recipeId) { return repository.getCookingLogs(recipeId); }
    List<PersonalDataDbHelper.CookingLog> getAllLogs() { return repository.getAllCookingLogs(); }

    List<PersonalDataDbHelper.CookingLog> filterLogs(List<PersonalDataDbHelper.CookingLog> source,
                                                     String query, long fromInclusive, long toExclusive,
                                                     int selectedRating) {
        String normalizedQuery = normalize(query);
        List<PersonalDataDbHelper.CookingLog> matches = new ArrayList<>();
        for (PersonalDataDbHelper.CookingLog log : source) {
            if (fromInclusive > 0 && log.cookedAt < fromInclusive) continue;
            if (toExclusive > 0 && log.cookedAt >= toExclusive) continue;
            if (selectedRating > 0 && (log.rating == null || log.rating != selectedRating)) continue;
            if (!normalizedQuery.isEmpty()
                    && !normalize(log.title + " " + log.adjustmentNote).contains(normalizedQuery)) continue;
            matches.add(log);
        }
        return matches;
    }

    JournalStats getStats() {
        List<PersonalDataDbHelper.CookingLog> logs = getAllLogs();
        if (logs.isEmpty()) return new JournalStats("—", 0, null, null);
        LinkedHashMap<String, Integer> recipeCounts = new LinkedHashMap<>();
        String mostCookedRecipe = "—";
        int mostCookedCount = 0;
        Integer latestRating = null;
        for (PersonalDataDbHelper.CookingLog log : logs) {
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

    static final class JournalStats {
        final String mostCookedRecipe;
        final int mostCookedCount;
        final Long lastCookedAt;
        final Integer latestRating;
        JournalStats(String recipe, int count, Long lastCookedAt, Integer latestRating) {
            this.mostCookedRecipe = recipe;
            this.mostCookedCount = count;
            this.lastCookedAt = lastCookedAt;
            this.latestRating = latestRating;
        }
    }
    PersonalDataDbHelper.CookingLog getLog(String id) { return repository.getCookingLog(id); }
    boolean save(String id, MainActivity.RecipePreview recipe, long cookedAt, int servings,
                 Integer duration, Integer rating, String adjustment, String photoPath) {
        return repository.saveCookingLog(id, recipe, cookedAt, servings, duration, rating, adjustment, photoPath);
    }
    boolean delete(String id) { return repository.deleteCookingLog(id); }
    CookingJournalPhotoStore.CopyResult savePhoto(Uri uri) { return repository.saveCookingLogPhoto(uri); }
    File getPhoto(String filename) { return repository.getCookingLogPhoto(filename); }
    void deletePhoto(String filename) { repository.deleteCookingLogPhoto(filename); }
}
