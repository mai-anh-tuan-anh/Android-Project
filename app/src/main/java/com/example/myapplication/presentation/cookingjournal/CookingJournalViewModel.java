package com.example.myapplication.presentation.cookingjournal;



import androidx.lifecycle.ViewModel;
import com.example.myapplication.domain.model.*;
import com.example.myapplication.domain.repository.PersonalDataRepository;
import java.io.File;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/** Local cooking-session history linked to the original recipe. */
public final class CookingJournalViewModel extends ViewModel {
    private final PersonalDataRepository repository;

    public CookingJournalViewModel(PersonalDataRepository repository) {
        this.repository = repository;
    }

    public List<CookingLogEntry> getLogs(String recipeId) { return repository.getCookingLogs(recipeId); }
    public List<CookingLogEntry> getAllLogs() { return repository.getAllCookingLogs(); }

    public List<CookingLogEntry> filterLogs(List<CookingLogEntry> source,
                                                     String query, long fromInclusive, long toExclusive,
                                                     int selectedRating) {
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
        List<CookingLogEntry> logs = getAllLogs();
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
    public CookingLogEntry getLog(String id) { return repository.getCookingLog(id); }
    public boolean save(String id, Recipe recipe, long cookedAt, int servings,
                 Integer duration, Integer rating, String adjustment, String photoPath) {
        return repository.saveCookingLog(id, recipe, cookedAt, servings, duration, rating, adjustment, photoPath);
    }
    public boolean delete(String id) { return repository.deleteCookingLog(id); }
    public PhotoSaveResult savePhoto(String uriString) { return repository.saveCookingLogPhoto(uriString); }
    public File getPhoto(String filename) { return repository.getCookingLogPhoto(filename); }
    public void deletePhoto(String filename) { repository.deleteCookingLogPhoto(filename); }
}
