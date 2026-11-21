package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.Normalizer;
import java.math.BigDecimal;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Local storage for personal recipes, meal plans, and shopping lists (modules 5–7). */
final class PersonalDataDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "bep_nha_personal.db";
    private static final int DB_VERSION = 8;
    private static final long HISTORY_RETENTION_MS = 3L * 24 * 60 * 60 * 1000;
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
        createShoppingTables(db);
        createOfflinePersonalFeatureTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) createMealPlanTables(db);
        if (oldVersion < 3) migrateMealPlanServingRange(db);
        if (oldVersion < 4) createShoppingTables(db);
        if (oldVersion >= 4 && oldVersion < 5) {
            db.execSQL("ALTER TABLE ShoppingListItem ADD COLUMN category TEXT NOT NULL DEFAULT 'Khác'");
        }
        if (oldVersion >= 4 && oldVersion < 6) {
            db.execSQL("ALTER TABLE ShoppingList ADD COLUMN planSnapshot TEXT NOT NULL DEFAULT ''");
        }
        if (oldVersion >= 4 && oldVersion < 7) {
            migrateShoppingStatusesToTwoStates(db);
        }
        if (oldVersion < 8) createOfflinePersonalFeatureTables(db);
    }

    private static void createOfflinePersonalFeatureTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE PantryItem (itemId TEXT PRIMARY KEY, ingredientId TEXT, name TEXT NOT NULL, " +
                "amountDecimal TEXT, unitId TEXT, category TEXT NOT NULL DEFAULT 'Khác', expiryDate TEXT, " +
                "storageLocation TEXT NOT NULL DEFAULT '', updatedAt INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX PantryItem_expiry_idx ON PantryItem(expiryDate)");
        db.execSQL("CREATE TABLE CookingLog (logId TEXT PRIMARY KEY, recipeId TEXT NOT NULL, title TEXT NOT NULL, " +
                "imageRes TEXT NOT NULL, cookedAt INTEGER NOT NULL, servings INTEGER NOT NULL " +
                "CHECK(servings BETWEEN 1 AND 999), actualDurationMinutes INTEGER, rating INTEGER " +
                "CHECK(rating IS NULL OR rating BETWEEN 1 AND 5), adjustmentNote TEXT NOT NULL DEFAULT '', " +
                "photoLocalPath TEXT NOT NULL DEFAULT '', createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX CookingLog_recipe_date_idx ON CookingLog(recipeId,cookedAt DESC)");
    }

    private static void createShoppingTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE ShoppingList (listId INTEGER PRIMARY KEY CHECK(listId=1), " +
                "updatedAt INTEGER NOT NULL, planSnapshot TEXT NOT NULL DEFAULT '')");
        db.execSQL("INSERT OR IGNORE INTO ShoppingList(listId,updatedAt) VALUES(1,0)");
        db.execSQL("CREATE TABLE ShoppingListItem (itemId TEXT PRIMARY KEY, listId INTEGER NOT NULL DEFAULT 1, " +
                "ingredientId TEXT, name TEXT NOT NULL, amount TEXT NOT NULL, unit TEXT NOT NULL, " +
                "category TEXT NOT NULL DEFAULT 'Khác', " +
                "status TEXT NOT NULL CHECK(status IN ('NEED_TO_BUY','AVAILABLE')), " +
                "position INTEGER NOT NULL, updatedAt INTEGER NOT NULL, " +
                "FOREIGN KEY(listId) REFERENCES ShoppingList(listId) ON DELETE CASCADE)");
    }

    private static void migrateShoppingStatusesToTwoStates(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE ShoppingListItem_v7 (itemId TEXT PRIMARY KEY, listId INTEGER NOT NULL DEFAULT 1, " +
                "ingredientId TEXT, name TEXT NOT NULL, amount TEXT NOT NULL, unit TEXT NOT NULL, " +
                "category TEXT NOT NULL DEFAULT 'Khác', " +
                "status TEXT NOT NULL CHECK(status IN ('NEED_TO_BUY','AVAILABLE')), " +
                "position INTEGER NOT NULL, updatedAt INTEGER NOT NULL, " +
                "FOREIGN KEY(listId) REFERENCES ShoppingList(listId) ON DELETE CASCADE)");
        db.execSQL("INSERT INTO ShoppingListItem_v7(itemId,listId,ingredientId,name,amount,unit,category,status,position,updatedAt) " +
                "SELECT itemId,listId,ingredientId,name,amount,unit,category, " +
                "CASE WHEN status='NEED_TO_BUY' THEN 'NEED_TO_BUY' ELSE 'AVAILABLE' END,position,updatedAt " +
                "FROM ShoppingListItem");
        db.execSQL("DROP TABLE ShoppingListItem");
        db.execSQL("ALTER TABLE ShoppingListItem_v7 RENAME TO ShoppingListItem");
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
        deleteExpiredHistory(getWritableDatabase());
        return readRecipes("SELECT recipeId,title,imageRes,duration,difficulty FROM RecipeViewHistory " +
                "ORDER BY viewedAt DESC, rowid DESC LIMIT 10");
    }

    private static void deleteExpiredHistory(SQLiteDatabase db) {
        long cutoff = System.currentTimeMillis() - HISTORY_RETENTION_MS;
        db.delete("RecipeViewHistory", "viewedAt<=?", new String[]{String.valueOf(cutoff)});
    }

    /** Keeps client-local recipe history for at most three days and ten recent IDs. */
    void recordViewedRecipe(MainActivity.RecipePreview recipe) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            deleteExpiredHistory(db);
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

    boolean deleteCollection(long collectionId) {
        return getWritableDatabase().delete("Collection", "collectionId=?",
                new String[]{String.valueOf(collectionId)}) > 0;
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
        return saveCollectionAndGetId(name, selected, collectionId) != null;
    }

    Long saveCollectionAndGetId(String name, List<MainActivity.RecipePreview> selected, Long collectionId) {
        String clean = normalizeCollectionDisplayName(name);
        if (clean.isEmpty()) return null;
        SQLiteDatabase db = getWritableDatabase();
        String normalized = normalizeName(clean);
        db.beginTransaction();
        try {
            Long duplicateId = null;
            try (Cursor cursor = db.rawQuery("SELECT collectionId FROM Collection WHERE normalizedName=?", new String[]{normalized})) {
                if (cursor.moveToFirst()) duplicateId = cursor.getLong(0);
            }
            if (duplicateId != null && (collectionId == null || duplicateId.longValue() != collectionId.longValue())) return null;

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
            return collectionId;
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

    List<ShoppingItem> getShoppingItems() {
        List<ShoppingItem> items = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT itemId,name,amount,unit,status,category FROM ShoppingListItem WHERE listId=1 ORDER BY position,rowid", null)) {
            while (cursor.moveToNext()) {
                items.add(new ShoppingItem(cursor.getString(0), cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4), cursor.getString(5)));
            }
        }
        return items;
    }

    void saveShoppingItem(String itemId, String name, String amount, String unit, String category, String status) {
        if (name == null || name.trim().isEmpty() || amount == null || amount.trim().isEmpty()
                || unit == null || unit.trim().isEmpty() || category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("shopping item fields cannot be empty");
        }
        if (!"NEED_TO_BUY".equals(status) && !"AVAILABLE".equals(status)) {
            throw new IllegalArgumentException("invalid shopping item status");
        }
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("itemId", itemId == null ? java.util.UUID.randomUUID().toString() : itemId);
        values.put("listId", 1);
        values.putNull("ingredientId");
        values.put("name", name.trim().replaceAll("\\s+", " "));
        values.put("amount", amount.trim());
        values.put("unit", unit.trim().replaceAll("\\s+", " "));
        values.put("category", category.trim());
        values.put("status", status);
        values.put("updatedAt", System.currentTimeMillis());
        if (itemId == null) {
            try (Cursor cursor = db.rawQuery("SELECT COALESCE(MAX(position),-1)+1 FROM ShoppingListItem WHERE listId=1", null)) {
                values.put("position", cursor.moveToFirst() ? cursor.getInt(0) : 0);
            }
        }
        else {
            try (Cursor cursor = db.rawQuery("SELECT position FROM ShoppingListItem WHERE itemId=? AND listId=1",
                    new String[]{itemId})) {
                if (!cursor.moveToFirst()) return;
                values.put("position", cursor.getInt(0));
            }
        }
        db.insertWithOnConflict("ShoppingListItem", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        updateShoppingListTimestamp(db);
    }

    private static void updateShoppingListTimestamp(SQLiteDatabase db) {
        ContentValues list = new ContentValues();
        list.put("updatedAt", System.currentTimeMillis());
        db.update("ShoppingList", list, "listId=1", null);
    }

    void setShoppingItemStatus(String itemId, String status) {
        if (!"NEED_TO_BUY".equals(status) && !"AVAILABLE".equals(status)) {
            throw new IllegalArgumentException("invalid shopping item status");
        }
        ContentValues values = new ContentValues();
        values.put("status", status);
        values.put("updatedAt", System.currentTimeMillis());
        SQLiteDatabase db = getWritableDatabase();
        db.update("ShoppingListItem", values, "itemId=? AND listId=1", new String[]{itemId});
        updateShoppingListTimestamp(db);
    }

    void deleteShoppingItem(String itemId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("ShoppingListItem", "itemId=? AND listId=1", new String[]{itemId});
        updateShoppingListTimestamp(db);
    }

    void clearShoppingItems() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("ShoppingListItem", "listId=1", null);
        updateShoppingListTimestamp(db);
    }

    String getShoppingPlanSnapshot() {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT planSnapshot FROM ShoppingList WHERE listId=1", null)) {
            return cursor.moveToFirst() ? cursor.getString(0) : "";
        }
    }

    void saveShoppingPlanSnapshot(String snapshot) {
        ContentValues values = new ContentValues();
        values.put("planSnapshot", snapshot == null ? "" : snapshot);
        values.put("updatedAt", System.currentTimeMillis());
        getWritableDatabase().update("ShoppingList", values, "listId=1", null);
    }

    List<PantryItem> getPantryItems() {
        List<PantryItem> items = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT itemId,ingredientId,name,amountDecimal,unitId,category,expiryDate,storageLocation,updatedAt " +
                        "FROM PantryItem ORDER BY CASE WHEN expiryDate IS NULL THEN 1 ELSE 0 END, expiryDate, name COLLATE NOCASE, rowid", null)) {
            while (cursor.moveToNext()) {
                items.add(new PantryItem(cursor.getString(0), cursor.getString(1), cursor.getString(2),
                        cursor.isNull(3) ? null : cursor.getString(3), cursor.getString(4), cursor.getString(5),
                        cursor.getString(6), cursor.getString(7), cursor.getLong(8)));
            }
        }
        return items;
    }

    PantryItem getPantryItem(String itemId) {
        if (itemId == null) return null;
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT itemId,ingredientId,name,amountDecimal,unitId,category,expiryDate,storageLocation,updatedAt " +
                        "FROM PantryItem WHERE itemId=?", new String[]{itemId})) {
            if (!cursor.moveToFirst()) return null;
            return new PantryItem(cursor.getString(0), cursor.getString(1), cursor.getString(2),
                    cursor.isNull(3) ? null : cursor.getString(3), cursor.getString(4), cursor.getString(5),
                    cursor.getString(6), cursor.getString(7), cursor.getLong(8));
        }
    }

    boolean savePantryItem(String itemId, String name, String amount, String unitId,
                           String category, String expiryDate, String storageLocation) {
        String cleanName = cleanDisplayValue(name);
        if (cleanName.isEmpty() || category == null || category.trim().isEmpty()) return false;
        String cleanAmount = amount == null ? "" : amount.trim().replace(',', '.');
        String cleanUnit = unitId == null ? "" : unitId.trim();
        if (!cleanAmount.isEmpty()) {
            try {
                BigDecimal parsed = new BigDecimal(cleanAmount);
                if (parsed.signum() < 0 || cleanUnit.isEmpty()) return false;
                cleanAmount = parsed.stripTrailingZeros().toPlainString();
            } catch (NumberFormatException invalidAmount) {
                return false;
            }
        } else {
            cleanAmount = null;
            cleanUnit = "";
        }
        String cleanExpiry = expiryDate == null || expiryDate.trim().isEmpty() ? null : expiryDate.trim();
        if (cleanExpiry != null && !isValidIsoDate(cleanExpiry)) return false;

        ContentValues values = new ContentValues();
        values.put("itemId", itemId == null ? java.util.UUID.randomUUID().toString() : itemId);
        values.putNull("ingredientId");
        values.put("name", cleanName);
        if (cleanAmount == null) values.putNull("amountDecimal"); else values.put("amountDecimal", cleanAmount);
        values.put("unitId", cleanUnit);
        values.put("category", category.trim());
        if (cleanExpiry == null) values.putNull("expiryDate"); else values.put("expiryDate", cleanExpiry);
        values.put("storageLocation", cleanDisplayValue(storageLocation));
        values.put("updatedAt", System.currentTimeMillis());
        SQLiteDatabase db = getWritableDatabase();
        if (itemId != null && getPantryItem(itemId) == null) return false;
        db.insertWithOnConflict("PantryItem", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        return true;
    }

    boolean deletePantryItem(String itemId) {
        return getWritableDatabase().delete("PantryItem", "itemId=?", new String[]{itemId}) > 0;
    }

    List<CookingLog> getCookingLogs(String recipeId) {
        List<CookingLog> logs = new ArrayList<>();
        if (recipeId == null) return logs;
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT logId,recipeId,title,imageRes,cookedAt,servings,actualDurationMinutes,rating," +
                        "adjustmentNote,photoLocalPath,createdAt,updatedAt FROM CookingLog " +
                        "WHERE recipeId=? ORDER BY cookedAt DESC,createdAt DESC", new String[]{recipeId})) {
            while (cursor.moveToNext()) logs.add(readCookingLog(cursor));
        }
        return logs;
    }

    CookingLog getCookingLog(String logId) {
        if (logId == null) return null;
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT logId,recipeId,title,imageRes,cookedAt,servings,actualDurationMinutes,rating," +
                        "adjustmentNote,photoLocalPath,createdAt,updatedAt FROM CookingLog WHERE logId=?",
                new String[]{logId})) {
            return cursor.moveToFirst() ? readCookingLog(cursor) : null;
        }
    }

    private CookingLog readCookingLog(Cursor cursor) {
        return new CookingLog(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3),
                cursor.getLong(4), cursor.getInt(5), cursor.isNull(6) ? null : cursor.getInt(6),
                cursor.isNull(7) ? null : cursor.getInt(7), cursor.getString(8), cursor.getString(9),
                cursor.getLong(10), cursor.getLong(11));
    }

    boolean saveCookingLog(String logId, MainActivity.RecipePreview recipe, long cookedAt, int servings,
                           Integer actualDurationMinutes, Integer rating, String adjustmentNote, String photoLocalPath) {
        if (recipe == null || recipe.id == null || recipe.id.trim().isEmpty()) return false;
        if (servings < 1 || servings > 999 || (actualDurationMinutes != null && actualDurationMinutes < 1)
                || (rating != null && (rating < 1 || rating > 5))) return false;
        String id = logId == null ? java.util.UUID.randomUUID().toString() : logId;
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("logId", id);
        values.put("recipeId", recipe.id);
        values.put("title", recipe.title);
        values.put("imageRes", recipe.imageName);
        values.put("cookedAt", cookedAt);
        values.put("servings", servings);
        if (actualDurationMinutes == null) values.putNull("actualDurationMinutes");
        else values.put("actualDurationMinutes", actualDurationMinutes);
        if (rating == null) values.putNull("rating"); else values.put("rating", rating);
        values.put("adjustmentNote", cleanDisplayValue(adjustmentNote));
        values.put("photoLocalPath", photoLocalPath == null ? "" : photoLocalPath);
        if (logId == null) values.put("createdAt", now);
        else {
            CookingLog existing = getCookingLog(logId);
            if (existing == null) return false;
            values.put("createdAt", existing.createdAt);
        }
        values.put("updatedAt", now);
        getWritableDatabase().insertWithOnConflict("CookingLog", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        return true;
    }

    boolean deleteCookingLog(String logId) {
        return getWritableDatabase().delete("CookingLog", "logId=?", new String[]{logId}) > 0;
    }

    private static String cleanDisplayValue(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private static boolean isValidIsoDate(String value) {
        if (value == null || !value.matches("\\d{4}-\\d{2}-\\d{2}")) return false;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT);
        format.setLenient(false);
        ParsePosition position = new ParsePosition(0);
        return format.parse(value, position) != null && position.getIndex() == value.length();
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

    static final class ShoppingItem {
        final String id;
        final String name;
        final String amount;
        final String unit;
        final String status;
        final String category;
        ShoppingItem(String id, String name, String amount, String unit, String status, String category) {
            this.id = id;
            this.name = name;
            this.amount = amount;
            this.unit = unit;
            this.status = status;
            this.category = category;
        }
    }

    static final class PantryItem {
        final String id;
        final String ingredientId;
        final String name;
        final String amountDecimal;
        final String unitId;
        final String category;
        final String expiryDate;
        final String storageLocation;
        final long updatedAt;
        PantryItem(String id, String ingredientId, String name, String amountDecimal, String unitId,
                   String category, String expiryDate, String storageLocation, long updatedAt) {
            this.id = id;
            this.ingredientId = ingredientId;
            this.name = name;
            this.amountDecimal = amountDecimal;
            this.unitId = unitId;
            this.category = category;
            this.expiryDate = expiryDate;
            this.storageLocation = storageLocation;
            this.updatedAt = updatedAt;
        }
    }

    static final class CookingLog {
        final String id;
        final String recipeId;
        final String title;
        final String imageRes;
        final long cookedAt;
        final int servings;
        final Integer actualDurationMinutes;
        final Integer rating;
        final String adjustmentNote;
        final String photoLocalPath;
        final long createdAt;
        final long updatedAt;
        CookingLog(String id, String recipeId, String title, String imageRes, long cookedAt, int servings,
                   Integer actualDurationMinutes, Integer rating, String adjustmentNote, String photoLocalPath,
                   long createdAt, long updatedAt) {
            this.id = id;
            this.recipeId = recipeId;
            this.title = title;
            this.imageRes = imageRes;
            this.cookedAt = cookedAt;
            this.servings = servings;
            this.actualDurationMinutes = actualDurationMinutes;
            this.rating = rating;
            this.adjustmentNote = adjustmentNote;
            this.photoLocalPath = photoLocalPath;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
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
