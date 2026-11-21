package com.example.myapplication;

import android.app.Application;
import android.net.Uri;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.util.List;

/** Local cooking-session history linked to the original recipe. */
public final class CookingJournalViewModel extends AndroidViewModel {
    private final PersonalDataRepository repository;

    public CookingJournalViewModel(@NonNull Application application) {
        super(application);
        repository = PersonalDataRepository.getInstance(application);
    }

    List<PersonalDataDbHelper.CookingLog> getLogs(String recipeId) { return repository.getCookingLogs(recipeId); }
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
