package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Local storage for the user's personal recipe data (module 5). */
final class PersonalDataDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "bep_nha_personal.db";
    private static final int DB_VERSION = 1;
    private final boolean debugBuild;

    PersonalDataDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        debugBuild = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Favorite (recipeId TEXT PRIMARY KEY, title TEXT NOT NULL, " +
                "imageRes TEXT NOT NULL, duration TEXT NOT NULL, difficulty TEXT NOT NULL, savedAt INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE Collection (collectionId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, normalizedName TEXT NOT NULL UNIQUE)");
        db.execSQL("CREATE TABLE CollectionRecipe (collectionId INTEGER NOT NULL, recipeId TEXT NOT NULL, " +
                "title TEXT NOT NULL, imageRes TEXT NOT NULL, duration TEXT NOT NULL, difficulty TEXT NOT NULL, " +
                "PRIMARY KEY(collectionId, recipeId), FOREIGN KEY(collectionId) REFERENCES Collection(collectionId) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE RecipeNote (recipeId TEXT PRIMARY KEY, title TEXT NOT NULL, " +
                "imageRes TEXT NOT NULL, note TEXT NOT NULL, updatedAt INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE RecipeViewHistory (recipeId TEXT PRIMARY KEY, title TEXT NOT NULL, " +
                "imageRes TEXT NOT NULL, duration TEXT NOT NULL, difficulty TEXT NOT NULL, viewedAt INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE LocalMeta (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Add forward-only migrations here; personal data must never be dropped on upgrade.
    }

    void seedDesignPreviewIfNeeded(List<MainActivity.RecipePreview> recipes) {
        if (!debugBuild) return;
        SQLiteDatabase db = getWritableDatabase();
        if (exists(db, "SELECT 1 FROM LocalMeta WHERE key='design_preview_seeded'")) return;
        db.beginTransaction();
        try {
            ContentValues value = new ContentValues();
            value.put("key", "design_preview_seeded");
            value.put("value", "1");
            db.insert("LocalMeta", null, value);
            // These local preview rows reproduce the illustrative content shown in screens 13–17.
            addFavorite(db, recipes.get(0));
            addFavorite(db, recipes.get(1));
            addFavorite(db, recipes.get(2));
            addFavorite(db, recipes.get(3));
            addHistory(db, recipes.get(2));
            addHistory(db, recipes.get(3));
            long family = addCollection(db, "Bữa cơm nhà");
            addCollectionRecipe(db, family, recipes.get(0));
            addCollectionRecipe(db, family, recipes.get(1));
            long quick = addCollection(db, "Món nhanh");
            addCollectionRecipe(db, quick, recipes.get(4));
            ContentValues note = noteValues(recipes.get(0));
            note.put("note", "Lần sau giảm một chút me và thêm rau thơm sau khi tắt bếp.");
            note.put("updatedAt", System.currentTimeMillis());
            db.insertWithOnConflict("RecipeNote", null, note, SQLiteDatabase.CONFLICT_REPLACE);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private boolean exists(SQLiteDatabase db, String sql) {
        try (Cursor cursor = db.rawQuery(sql, null)) { return cursor.moveToFirst(); }
    }

    private ContentValues recipeValues(MainActivity.RecipePreview recipe) {
        ContentValues value = new ContentValues();
        value.put("recipeId", recipe.id);
        value.put("title", recipe.title);
        value.put("imageRes", recipe.imageName);
        value.put("duration", recipe.duration);
        value.put("difficulty", recipe.difficulty);
        return value;
    }

    private void addFavorite(SQLiteDatabase db, MainActivity.RecipePreview recipe) {
        ContentValues value = recipeValues(recipe);
        value.put("savedAt", System.currentTimeMillis());
        db.insertWithOnConflict("Favorite", null, value, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private void addHistory(SQLiteDatabase db, MainActivity.RecipePreview recipe) {
        ContentValues value = recipeValues(recipe);
        value.put("viewedAt", System.currentTimeMillis());
        db.insertWithOnConflict("RecipeViewHistory", null, value, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private long addCollection(SQLiteDatabase db, String name) {
        ContentValues value = new ContentValues();
        value.put("name", name);
        value.put("normalizedName", normalizeName(name));
        return db.insertWithOnConflict("Collection", null, value, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private void addCollectionRecipe(SQLiteDatabase db, long collectionId, MainActivity.RecipePreview recipe) {
        ContentValues value = recipeValues(recipe);
        value.put("collectionId", collectionId);
        db.insertWithOnConflict("CollectionRecipe", null, value, SQLiteDatabase.CONFLICT_IGNORE);
    }

    void addFavorite(MainActivity.RecipePreview recipe) {
        SQLiteDatabase db = getWritableDatabase();
        addFavorite(db, recipe);
    }

    void removeFavorite(String recipeId) {
        getWritableDatabase().delete("Favorite", "recipeId=?", new String[]{recipeId});
    }

    boolean isFavorite(String recipeId) {
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT 1 FROM Favorite WHERE recipeId=?", new String[]{recipeId})) {
            return cursor.moveToFirst();
        }
    }

    List<MainActivity.RecipePreview> getFavorites() {
        return readRecipes("SELECT recipeId,title,imageRes,duration,difficulty FROM Favorite ORDER BY savedAt ASC, rowid ASC");
    }

    List<MainActivity.RecipePreview> getHistory() {
        return readRecipes("SELECT recipeId,title,imageRes,duration,difficulty FROM RecipeViewHistory ORDER BY viewedAt DESC LIMIT 10");
    }

    List<CollectionRow> getCollections() {
        List<CollectionRow> rows = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT c.collectionId,c.name,COUNT(r.recipeId) FROM Collection c " +
                        "LEFT JOIN CollectionRecipe r ON r.collectionId=c.collectionId " +
                        "GROUP BY c.collectionId ORDER BY c.collectionId", null)) {
            while (cursor.moveToNext()) rows.add(new CollectionRow(cursor.getLong(0), cursor.getString(1), cursor.getInt(2)));
        }
        return rows;
    }

    List<MainActivity.RecipePreview> getCollectionRecipes(long collectionId) {
        return readRecipes("SELECT recipeId,title,imageRes,duration,difficulty FROM CollectionRecipe WHERE collectionId=" + collectionId + " ORDER BY rowid");
    }

    boolean isCollectionNameAvailable(String name, Long collectionId) {
        String clean = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        if (clean.isEmpty()) return false;
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT collectionId FROM Collection WHERE normalizedName=?",
                new String[]{normalizeName(clean)})) {
            if (!cursor.moveToFirst()) return true;
            return collectionId != null && cursor.getLong(0) == collectionId;
        }
    }

    private List<MainActivity.RecipePreview> readRecipes(String sql) {
        List<MainActivity.RecipePreview> rows = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, null)) {
            while (cursor.moveToNext()) rows.add(new MainActivity.RecipePreview(
                    cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4)));
        }
        return rows;
    }

    boolean saveCollection(String name, List<MainActivity.RecipePreview> selected, Long collectionId) {
        String clean = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        if (clean.isEmpty()) return false;
        SQLiteDatabase db = getWritableDatabase();
        String normalized = normalizeName(clean);
        db.beginTransaction();
        try {
            Long duplicateId = null;
            try (Cursor cursor = db.rawQuery("SELECT collectionId FROM Collection WHERE normalizedName=?", new String[]{normalized})) {
                if (cursor.moveToFirst()) duplicateId = cursor.getLong(0);
            }
            if (duplicateId != null && (collectionId == null || duplicateId.longValue() != collectionId.longValue())) return false;

            if (collectionId == null) {
                ContentValues value = new ContentValues();
                value.put("name", clean);
                value.put("normalizedName", normalized);
                collectionId = db.insertOrThrow("Collection", null, value);
            } else {
                ContentValues value = new ContentValues();
                value.put("name", clean);
                value.put("normalizedName", normalized);
                db.update("Collection", value, "collectionId=?", new String[]{String.valueOf(collectionId)});
                db.delete("CollectionRecipe", "collectionId=?", new String[]{String.valueOf(collectionId)});
            }
            for (MainActivity.RecipePreview recipe : selected) addCollectionRecipe(db, collectionId, recipe);
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    String getNote(String recipeId) {
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT note FROM RecipeNote WHERE recipeId=?", new String[]{recipeId})) {
            return cursor.moveToFirst() ? cursor.getString(0) : "";
        }
    }

    void saveNote(MainActivity.RecipePreview recipe, String note) {
        ContentValues value = noteValues(recipe);
        value.put("note", note);
        value.put("updatedAt", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("RecipeNote", null, value, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private ContentValues noteValues(MainActivity.RecipePreview recipe) {
        ContentValues value = new ContentValues();
        value.put("recipeId", recipe.id);
        value.put("title", recipe.title);
        value.put("imageRes", recipe.imageName);
        return value;
    }

    private static String normalizeName(String value) {
        String clean = value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        String decomposed = Normalizer.normalize(clean, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "");
    }

    static final class CollectionRow {
        final long id;
        final String name;
        final int recipeCount;
        CollectionRow(long id, String name, int recipeCount) {
            this.id = id;
            this.name = name;
            this.recipeCount = recipeCount;
        }
    }
}
