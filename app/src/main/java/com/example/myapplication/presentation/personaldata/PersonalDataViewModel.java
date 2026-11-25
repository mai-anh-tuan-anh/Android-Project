package com.example.myapplication.presentation.personaldata;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.domain.model.RecipeCollection;
import com.example.myapplication.domain.repository.PersonalDataRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.text.Normalizer;
import java.util.concurrent.Executor;

/** Asynchronous local state and actions for module 5. */
public final class PersonalDataViewModel extends ViewModel {
    private final PersonalDataRepository repository;
    private final Executor ioExecutor;
    private final MutableLiveData<PersonalDataState> personalDataState = new MutableLiveData<>(
            new PersonalDataState(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyMap(), Collections.emptyMap(), false, null));
    private final MutableLiveData<CollectionSaveResult> collectionSaveResult = new MutableLiveData<>();
    private final MutableLiveData<NoteSaveResult> noteSaveResult = new MutableLiveData<>();
    private final List<String> selectedCollectionRecipeIds = new ArrayList<>();
    private final Set<String> favoriteIds = new HashSet<>();
    private List<Recipe> favorites = new ArrayList<>();
    private List<Recipe> history = new ArrayList<>();
    private List<RecipeCollection> collections = new ArrayList<>();
    private Map<Long, List<Recipe>> collectionRecipes = new HashMap<>();
    private Map<String, String> notes = new HashMap<>();
    private String collectionNameDraft = "";
    private boolean collectionSubmitAttempted;
    private boolean loaded;

    public PersonalDataViewModel(PersonalDataRepository repository, Executor ioExecutor) {
        this.repository = repository;
        this.ioExecutor = ioExecutor;
    }

    public LiveData<PersonalDataState> personalDataState() { return personalDataState; }
    public LiveData<CollectionSaveResult> collectionSaveResult() { return collectionSaveResult; }
    public LiveData<NoteSaveResult> noteSaveResult() { return noteSaveResult; }
    public boolean isLoaded() { return loaded; }

    public void seedDesignPreviewIfNeeded(List<Recipe> recipes) {
        List<Recipe> recipeSnapshot = recipes == null ? Collections.emptyList() : new ArrayList<>(recipes);
        ioExecutor.execute(() -> {
            try {
                repository.seedDesignPreviewIfNeeded(recipeSnapshot);
                loadPersonalData(recipeSnapshot);
            } catch (RuntimeException error) {
                postError(error);
            }
        });
    }

    public void refresh(List<Recipe> knownRecipes) {
        List<Recipe> recipeSnapshot = knownRecipes == null ? Collections.emptyList() : new ArrayList<>(knownRecipes);
        personalDataState.setValue(new PersonalDataState(favorites, history, collections, collectionRecipes, notes,
                true, null));
        ioExecutor.execute(() -> loadPersonalData(recipeSnapshot));
    }

    private void loadPersonalData(List<Recipe> knownRecipes) {
        try {
            List<Recipe> loadedFavorites = repository.getFavorites();
            List<Recipe> loadedHistory = repository.getHistory();
            List<RecipeCollection> loadedCollections = repository.getCollections();
            Map<Long, List<Recipe>> loadedCollectionRecipes = new HashMap<>();
            for (RecipeCollection collection : loadedCollections) {
                loadedCollectionRecipes.put(collection.id, repository.getCollectionRecipes(collection.id));
            }
            Map<String, String> loadedNotes = new HashMap<>(notes);
            for (Recipe recipe : knownRecipes) loadedNotes.put(recipe.id, repository.getNote(recipe.id));
            favorites = new ArrayList<>(loadedFavorites);
            history = new ArrayList<>(loadedHistory);
            collections = new ArrayList<>(loadedCollections);
            collectionRecipes = loadedCollectionRecipes;
            notes = loadedNotes;
            favoriteIds.clear();
            for (Recipe recipe : favorites) favoriteIds.add(recipe.id);
            loaded = true;
            personalDataState.postValue(snapshot(false, null));
        } catch (RuntimeException error) {
            postError(error);
        }
    }

    private void postError(RuntimeException error) {
        personalDataState.postValue(new PersonalDataState(favorites, history, collections,
                collectionRecipes, notes, false,
                error.getMessage() == null ? "Không thể tải dữ liệu cá nhân." : error.getMessage()));
    }

    private PersonalDataState snapshot(boolean loading, String error) {
        return new PersonalDataState(favorites, history, collections, collectionRecipes, notes, loading, error);
    }

    public void addFavorite(Recipe recipe) {
        if (recipe == null || favoriteIds.contains(recipe.id)) return;
        favoriteIds.add(recipe.id);
        favorites.add(0, recipe);
        publishLocalState();
        ioExecutor.execute(() -> {
            try { repository.addFavorite(recipe); loadPersonalData(Collections.emptyList()); }
            catch (RuntimeException error) { postError(error); }
        });
    }

    public void removeFavorite(String recipeId) {
        if (recipeId == null || !favoriteIds.remove(recipeId)) return;
        favorites.removeIf(recipe -> recipeId.equals(recipe.id));
        publishLocalState();
        ioExecutor.execute(() -> {
            try { repository.removeFavorite(recipeId); loadPersonalData(Collections.emptyList()); }
            catch (RuntimeException error) { postError(error); }
        });
    }

    public boolean isFavorite(String recipeId) { return recipeId != null && favoriteIds.contains(recipeId); }
    public List<Recipe> getFavorites() { return new ArrayList<>(favorites); }
    public List<Recipe> getHistory() { return new ArrayList<>(history); }

    public Recipe findSavedRecipe(String id) {
        for (Recipe recipe : favorites) if (recipe.id.equals(id)) return recipe;
        for (Recipe recipe : history) if (recipe.id.equals(id)) return recipe;
        for (List<Recipe> recipes : collectionRecipes.values()) {
            for (Recipe recipe : recipes) if (recipe.id.equals(id)) return recipe;
        }
        return null;
    }

    public void recordViewedRecipe(Recipe recipe) {
        if (recipe == null) return;
        history.removeIf(item -> recipe.id.equals(item.id));
        history.add(0, recipe);
        if (history.size() > 10) history = new ArrayList<>(history.subList(0, 10));
        publishLocalState();
        ioExecutor.execute(() -> {
            try { repository.recordViewedRecipe(recipe); loadPersonalData(Collections.emptyList()); }
            catch (RuntimeException error) { postError(error); }
        });
    }

    public List<RecipeCollection> getCollections() { return new ArrayList<>(collections); }

    public void deleteCollection(long collectionId) {
        collections.removeIf(collection -> collection.id == collectionId);
        collectionRecipes.remove(collectionId);
        publishLocalState();
        ioExecutor.execute(() -> {
            try { repository.deleteCollection(collectionId); loadPersonalData(Collections.emptyList()); }
            catch (RuntimeException error) { postError(error); }
        });
    }

    public List<Recipe> getCollectionRecipes(long id) {
        List<Recipe> recipes = collectionRecipes.get(id);
        return recipes == null ? new ArrayList<>() : new ArrayList<>(recipes);
    }

    public boolean saveCollection(String name, List<Recipe> recipes, Long id) {
        List<Recipe> snapshot = recipes == null ? Collections.emptyList() : new ArrayList<>(recipes);
        if (!isCollectionNameAvailable(name, id)) return false;
        ioExecutor.execute(() -> {
            try { repository.saveCollection(name, snapshot, id); loadPersonalData(Collections.emptyList()); }
            catch (RuntimeException error) { postError(error); }
        });
        return true;
    }

    public void beginCollectionForm(String currentName, List<Recipe> currentRecipes) {
        collectionNameDraft = currentName == null ? "" : currentName;
        collectionSubmitAttempted = false;
        selectedCollectionRecipeIds.clear();
        if (currentRecipes != null) {
            for (Recipe recipe : currentRecipes) selectedCollectionRecipeIds.add(recipe.id);
        }
    }

    public String getCollectionNameDraft() { return collectionNameDraft; }
    public void setCollectionNameDraft(String name) { collectionNameDraft = name == null ? "" : name; }
    public boolean wasCollectionSubmitAttempted() { return collectionSubmitAttempted; }
    public void markCollectionSubmitAttempted() { collectionSubmitAttempted = true; }
    public boolean isCollectionRecipeSelected(String recipeId) { return selectedCollectionRecipeIds.contains(recipeId); }
    public void toggleCollectionRecipe(String recipeId) {
        if (selectedCollectionRecipeIds.contains(recipeId)) selectedCollectionRecipeIds.remove(recipeId);
        else selectedCollectionRecipeIds.add(recipeId);
    }

    public boolean isCollectionNameAvailable(String name, Long collectionId) {
        if (name == null || name.trim().isEmpty()) return false;
        String normalized = normalizeCollectionName(name);
        for (RecipeCollection collection : collections) {
            if (collectionId != null && collection.id == collectionId) continue;
            if (normalized.equals(normalizeCollectionName(collection.name))) return false;
        }
        return true;
    }

    public boolean saveCollectionForm(Long collectionId, List<Recipe> availableRecipes) {
        if (!loaded || collectionNameDraft.trim().isEmpty()
                || !isCollectionNameAvailable(collectionNameDraft, collectionId)) return false;
        Map<String, Recipe> recipesById = new LinkedHashMap<>();
        for (Recipe recipe : availableRecipes) recipesById.put(recipe.id, recipe);
        List<Recipe> selected = new ArrayList<>();
        for (String id : selectedCollectionRecipeIds) {
            Recipe recipe = recipesById.get(id);
            if (recipe != null) selected.add(recipe);
        }
        String name = collectionNameDraft.trim();
        ioExecutor.execute(() -> {
            try {
                Long savedId = repository.saveCollectionAndGetId(name, selected, collectionId);
                loadPersonalData(Collections.emptyList());
                collectionSaveResult.postValue(new CollectionSaveResult(savedId,
                        savedId == null ? "Tên này đã được sử dụng." : null));
            } catch (RuntimeException error) {
                collectionSaveResult.postValue(new CollectionSaveResult(null,
                        error.getMessage() == null ? "Không thể lưu bộ sưu tập." : error.getMessage()));
            }
        });
        return true;
    }

    public String getNote(String recipeId) { return recipeId == null ? "" : notes.getOrDefault(recipeId, ""); }
    public boolean hasLoadedNote(String recipeId) { return notes.containsKey(recipeId); }

    public void loadNote(String recipeId) {
        if (recipeId == null || hasLoadedNote(recipeId)) return;
        ioExecutor.execute(() -> {
            try {
                String note = repository.getNote(recipeId);
                Map<String, String> updated = new HashMap<>(notes);
                updated.put(recipeId, note);
                notes = updated;
                personalDataState.postValue(snapshot(false, null));
            } catch (RuntimeException error) {
                postError(error);
            }
        });
    }

    public void saveNote(Recipe recipe, String note) {
        if (recipe == null) return;
        String content = note == null ? "" : note;
        notes.put(recipe.id, content);
        ioExecutor.execute(() -> {
            try {
                repository.saveNote(recipe, content);
                noteSaveResult.postValue(new NoteSaveResult(true, null));
            } catch (RuntimeException error) {
                noteSaveResult.postValue(new NoteSaveResult(false,
                        error.getMessage() == null ? "Không thể lưu ghi chú." : error.getMessage()));
            }
        });
    }

    public void clearCollectionSaveResult() { collectionSaveResult.setValue(null); }
    public void clearNoteSaveResult() { noteSaveResult.setValue(null); }

    private void publishLocalState() {
        personalDataState.setValue(snapshot(false, null));
    }

    private static String normalizeCollectionName(String value) {
        if (value == null) return "";
        String clean = value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        return Normalizer.normalize(clean, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    public static final class PersonalDataState {
        public final List<Recipe> favorites;
        public final List<Recipe> history;
        public final List<RecipeCollection> collections;
        public final Map<Long, List<Recipe>> collectionRecipes;
        public final Map<String, String> notes;
        public final boolean loading;
        public final String error;

        PersonalDataState(List<Recipe> favorites, List<Recipe> history, List<RecipeCollection> collections,
                          Map<Long, List<Recipe>> collectionRecipes, Map<String, String> notes,
                          boolean loading, String error) {
            this.favorites = Collections.unmodifiableList(new ArrayList<>(favorites));
            this.history = Collections.unmodifiableList(new ArrayList<>(history));
            this.collections = Collections.unmodifiableList(new ArrayList<>(collections));
            Map<Long, List<Recipe>> recipesCopy = new HashMap<>();
            for (Map.Entry<Long, List<Recipe>> entry : collectionRecipes.entrySet()) {
                recipesCopy.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
            }
            this.collectionRecipes = Collections.unmodifiableMap(recipesCopy);
            this.notes = Collections.unmodifiableMap(new HashMap<>(notes));
            this.loading = loading;
            this.error = error;
        }
    }

    public static final class CollectionSaveResult {
        public final Long collectionId;
        public final String error;
        CollectionSaveResult(Long collectionId, String error) {
            this.collectionId = collectionId;
            this.error = error;
        }
    }

    public static final class NoteSaveResult {
        public final boolean success;
        public final String error;
        NoteSaveResult(boolean success, String error) {
            this.success = success;
            this.error = error;
        }
    }
}
