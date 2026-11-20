package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Local storage for personal recipes and the current meal plan (modules 5 and 6). */
final class PersonalDataDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "bep_nha_personal.db";
    private static final int DB_VERSION = 3;
    private final boolean debugBuild;

    PersonalDataDbHelper(Context context) {
        this(context, DB_NAME);
    }

    PersonalDataDbHelper(Context context, String databaseName) {
        super(context, databaseName, null, DB_VERSION);
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
        createMealPlanTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) createMealPlanTables(db);
        if (oldVersion < 3) migrateMealPlanServingRange(db);
    }

    private static void createMealPlanTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE MealPlan (planId INTEGER PRIMARY KEY CHECK(planId=1), title TEXT NOT NULL)");
        db.execSQL("INSERT OR IGNORE INTO MealPlan(planId,title) VALUES(1,'Bữa cơm nhà')");
        createMealPlanRecipeTable(db);
    }

    private static void createMealPlanRecipeTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE MealPlanRecipe (planId INTEGER NOT NULL DEFAULT 1, recipeId TEXT NOT NULL, " +
                "title TEXT NOT NULL, imageRes TEXT NOT NULL, duration TEXT NOT NULL, difficulty TEXT NOT NULL, " +
                "servings INTEGER NOT NULL CHECK(servings BETWEEN 1 AND 999), position INTEGER NOT NULL, " +
                "PRIMARY KEY(planId,recipeId), FOREIGN KEY(planId) REFERENCES MealPlan(planId) ON DELETE CASCADE)");
    }

    private static void migrateMealPlanServingRange(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE MealPlanRecipe RENAME TO MealPlanRecipe_v2");
        createMealPlanRecipeTable(db);
        db.execSQL("INSERT INTO MealPlanRecipe(planId,recipeId,title,imageRes,duration,difficulty,servings,position) " +
                "SELECT planId,recipeId,title,imageRes,duration,difficulty,servings,position FROM MealPlanRecipe_v2");
        db.execSQL("DROP TABLE MealPlanRecipe_v2");
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
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
            addHistory(db, recipes.get(3));
            addHistory(db, recipes.get(2));
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
        return readRecipes("SELECT recipeId,title,imageRes,duration,difficulty FROM RecipeViewHistory " +
                "ORDER BY viewedAt DESC, rowid DESC LIMIT 10");
    }

    /** Upserts a recipe visit and retains only the 10 most recent IDs. */
    void recordViewedRecipe(MainActivity.RecipePreview recipe) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues value = recipeValues(recipe);
            value.put("viewedAt", System.currentTimeMillis());
            db.insertWithOnConflict("RecipeViewHistory", null, value, SQLiteDatabase.CONFLICT_REPLACE);
            db.delete("RecipeViewHistory", "recipeId NOT IN (" +
                    "SELECT recipeId FROM RecipeViewHistory ORDER BY viewedAt DESC, rowid DESC LIMIT 10)", null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
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
        String clean = normalizeCollectionDisplayName(name);
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
        String clean = normalizeCollectionDisplayName(name);
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

    List<PlannedRecipe> getMealPlanRecipes() {
        List<PlannedRecipe> rows = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT recipeId,title,imageRes,duration,difficulty,servings FROM MealPlanRecipe " +
                        "WHERE planId=1 ORDER BY position, rowid", null)) {
            while (cursor.moveToNext()) {
                MainActivity.RecipePreview recipe = new MainActivity.RecipePreview(
                        cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4));
                rows.add(new PlannedRecipe(recipe, cursor.getInt(5)));
            }
        }
        return rows;
    }

    void addMealPlanRecipe(MainActivity.RecipePreview recipe, int servings) {
        validateServings(servings);
        SQLiteDatabase db = getWritableDatabase();
        ContentValues value = recipeValues(recipe);
        value.put("planId", 1);
        value.put("servings", servings);
        value.put("position", getMealPlanRecipes().size());
        db.insertWithOnConflict("MealPlanRecipe", null, value, SQLiteDatabase.CONFLICT_IGNORE);
    }

    void setMealPlanRecipeServings(String recipeId, int servings) {
        validateServings(servings);
        ContentValues value = new ContentValues();
        value.put("servings", servings);
        getWritableDatabase().update("MealPlanRecipe", value, "planId=1 AND recipeId=?", new String[]{recipeId});
    }

    void removeMealPlanRecipe(String recipeId) {
        getWritableDatabase().delete("MealPlanRecipe", "planId=1 AND recipeId=?", new String[]{recipeId});
    }

    /** Replaces the current plan atomically so a failed save cannot leave a partial selection. */
    void replaceMealPlanRecipes(List<PlannedRecipe> recipes) {
        if (recipes == null) throw new IllegalArgumentException("recipes cannot be null");
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("MealPlanRecipe", "planId=1", null);
            Set<String> recipeIds = new HashSet<>();
            int position = 0;
            for (PlannedRecipe planned : recipes) {
                if (planned == null || planned.recipe == null || planned.recipe.id == null
                        || planned.recipe.id.trim().isEmpty()) {
                    throw new IllegalArgumentException("recipe must have a stable ID");
                }
                validateServings(planned.servings);
                if (!recipeIds.add(planned.recipe.id)) {
                    throw new IllegalArgumentException("recipe IDs must be unique in a meal plan");
                }
                ContentValues value = recipeValues(planned.recipe);
                value.put("planId", 1);
                value.put("servings", planned.servings);
                value.put("position", position++);
                db.insertOrThrow("MealPlanRecipe", null, value);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static void validateServings(int servings) {
        if (servings < 1 || servings > 999) {
            throw new IllegalArgumentException("servings must be between 1 and 999");
        }
    }

    static final class PlannedRecipe {
        final MainActivity.RecipePreview recipe;
        final int servings;
        PlannedRecipe(MainActivity.RecipePreview recipe, int servings) {
            this.recipe = recipe;
            this.servings = servings;
        }
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

    private static String normalizeCollectionDisplayName(String value) {
        if (value == null || value.isEmpty()) return "";
        StringBuilder normalized = new StringBuilder();
        boolean pendingSpace = false;
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)) {
                pendingSpace = normalized.length() > 0;
            } else {
                if (pendingSpace) normalized.append(' ');
                normalized.appendCodePoint(codePoint);
                pendingSpace = false;
            }
        }
        return normalized.toString();
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
