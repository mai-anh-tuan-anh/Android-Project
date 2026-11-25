package com.example.myapplication.data.local.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.myapplication.data.mapper.CatalogSnapshotMapper;
import com.example.myapplication.data.mapper.CatalogSnapshotMapper.Snapshot;
import com.example.myapplication.domain.model.CookingStep;
import com.example.myapplication.domain.model.Ingredient;
import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.domain.model.RecipeIngredient;
import com.example.myapplication.domain.repository.RecipeCatalogRepository.Catalog;
import com.example.myapplication.domain.repository.RecipeCatalogRepository.Lookup;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Stores only the replaceable recipe catalog, separate from personal data. */
public final class RecipeCatalogDbHelper extends SQLiteOpenHelper {
    public enum ApplyResult { APPLIED, UNCHANGED, STALE }

    private static final String DB_NAME = "bep_nha_catalog.db";
    private static final int DB_VERSION = 1;
    private static final String RECIPE_COLUMNS = "recipe_id,title,description,image_path,base_servings,"
            + "prep_minutes,cook_minutes,duration_minutes,difficulty,publication_status,ingredient_data_status,"
            + "published_at,updated_at,tools_json,tip";

    public RecipeCatalogDbHelper(Context context) {
        this(context, DB_NAME);
    }

    public RecipeCatalogDbHelper(Context context, String databaseName) {
        super(context, databaseName, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE CatalogMeta (id INTEGER PRIMARY KEY CHECK(id=1), schema_version INTEGER NOT NULL, "
                + "revision INTEGER NOT NULL, fingerprint TEXT NOT NULL)");
        db.execSQL("CREATE TABLE Category (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL)");
        db.execSQL("CREATE TABLE Diet (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL)");
        db.execSQL("CREATE TABLE Ingredient (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, group_name TEXT, "
                + "status TEXT NOT NULL, aliases_json TEXT NOT NULL)");
        db.execSQL("CREATE TABLE Recipe (recipe_id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, description TEXT, "
                + "image_path TEXT, base_servings INTEGER NOT NULL CHECK(base_servings BETWEEN 1 AND 999), "
                + "prep_minutes INTEGER, cook_minutes INTEGER, duration_minutes INTEGER, difficulty TEXT, "
                + "publication_status TEXT NOT NULL, ingredient_data_status TEXT NOT NULL, published_at INTEGER, "
                + "updated_at INTEGER, tools_json TEXT NOT NULL, tip TEXT)");
        db.execSQL("CREATE TABLE RecipeCategory (recipe_id TEXT NOT NULL REFERENCES Recipe(recipe_id) ON DELETE CASCADE, "
                + "category_id TEXT NOT NULL REFERENCES Category(id), PRIMARY KEY(recipe_id,category_id))");
        db.execSQL("CREATE TABLE RecipeDiet (recipe_id TEXT NOT NULL REFERENCES Recipe(recipe_id) ON DELETE CASCADE, "
                + "diet_id TEXT NOT NULL REFERENCES Diet(id), PRIMARY KEY(recipe_id,diet_id))");
        db.execSQL("CREATE TABLE RecipeIngredient (recipe_id TEXT NOT NULL REFERENCES Recipe(recipe_id) ON DELETE CASCADE, "
                + "line_id TEXT NOT NULL, ingredient_id TEXT NOT NULL REFERENCES Ingredient(id), amount_decimal TEXT, "
                + "unit_id TEXT, qualitative_note TEXT, sort_order INTEGER NOT NULL CHECK(sort_order>=0), "
                + "PRIMARY KEY(recipe_id,line_id), UNIQUE(recipe_id,sort_order))");
        db.execSQL("CREATE TABLE CookingStep (recipe_id TEXT NOT NULL REFERENCES Recipe(recipe_id) ON DELETE CASCADE, "
                + "step_id TEXT NOT NULL, title TEXT NOT NULL, instruction TEXT NOT NULL, "
                + "sort_order INTEGER NOT NULL CHECK(sort_order>=0), duration_seconds INTEGER, image_path TEXT, "
                + "PRIMARY KEY(recipe_id,step_id), UNIQUE(recipe_id,sort_order))");
        // Incomplete ingredient lists can retain unresolved references from the publisher.
        db.execSQL("CREATE TABLE StepIngredient (recipe_id TEXT NOT NULL, step_id TEXT NOT NULL, line_id TEXT NOT NULL, "
                + "PRIMARY KEY(recipe_id,step_id,line_id), "
                + "FOREIGN KEY(recipe_id,step_id) REFERENCES CookingStep(recipe_id,step_id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE RemovedRecipe (recipe_id TEXT NOT NULL PRIMARY KEY, removed_at INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("No catalog migration from " + oldVersion + " to " + newVersion);
    }

    public ApplyResult replaceSnapshot(Snapshot snapshot) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransactionNonExclusive();
        try (Cursor meta = db.rawQuery("SELECT revision,fingerprint FROM CatalogMeta WHERE id=1", null)) {
            if (meta.moveToFirst()) {
                long currentRevision = meta.getLong(0);
                if (snapshot.revision < currentRevision) return ApplyResult.STALE;
                if (snapshot.revision == currentRevision) {
                    if (!snapshot.fingerprint.equals(meta.getString(1))) {
                        throw new IllegalArgumentException("different catalog content at revision " + currentRevision);
                    }
                    return ApplyResult.UNCHANGED;
                }
            }

            // A later snapshot may omit an old tombstone. Preserve known removals until republished.
            db.execSQL("INSERT OR IGNORE INTO RemovedRecipe(recipe_id,removed_at) "
                    + "SELECT recipe_id,COALESCE(updated_at,0) FROM Recipe WHERE publication_status='ARCHIVED'");
            db.delete("Recipe", null, null);
            db.delete("Category", null, null);
            db.delete("Diet", null, null);
            db.delete("Ingredient", null, null);
            for (Map.Entry<String, String> category : snapshot.categories.entrySet()) {
                db.execSQL("INSERT INTO Category(id,name) VALUES(?,?)", new Object[]{category.getKey(), category.getValue()});
            }
            for (Map.Entry<String, String> diet : snapshot.diets.entrySet()) {
                db.execSQL("INSERT INTO Diet(id,name) VALUES(?,?)", new Object[]{diet.getKey(), diet.getValue()});
            }
            for (Ingredient ingredient : snapshot.ingredients.values()) {
                db.execSQL("INSERT INTO Ingredient(id,name,group_name,status,aliases_json) VALUES(?,?,?,?,?)",
                        new Object[]{ingredient.ingredientId, ingredient.canonicalName, ingredient.groupName,
                                ingredient.status.name(), new JSONArray(ingredient.aliases).toString()});
            }
            for (Recipe recipe : snapshot.recipes.values()) insertRecipe(db, recipe);
            db.execSQL("DELETE FROM RemovedRecipe WHERE recipe_id IN "
                    + "(SELECT recipe_id FROM Recipe WHERE publication_status='PUBLISHED')");
            for (Map.Entry<String, Long> removal : snapshot.removed.entrySet()) {
                db.execSQL("INSERT OR REPLACE INTO RemovedRecipe(recipe_id,removed_at) VALUES(?,?)",
                        new Object[]{removal.getKey(), removal.getValue()});
            }
            db.execSQL("INSERT OR REPLACE INTO CatalogMeta(id,schema_version,revision,fingerprint) VALUES(1,?,?,?)",
                    new Object[]{snapshot.schemaVersion, snapshot.revision, snapshot.fingerprint});
            db.setTransactionSuccessful();
            return ApplyResult.APPLIED;
        } finally {
            db.endTransaction();
        }
    }

    private static void insertRecipe(SQLiteDatabase db, Recipe recipe) {
        Recipe.Details details = recipe.details;
        db.execSQL("INSERT INTO Recipe(" + RECIPE_COLUMNS + ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                new Object[]{recipe.id, recipe.title, details.description, details.imagePath, details.baseServings,
                        details.prepMinutes, details.cookMinutes, details.durationMinutes, recipe.difficulty,
                        details.publicationStatus.name(), details.ingredientDataStatus.name(), details.publishedAtMillis,
                        details.updatedAtMillis, new JSONArray(details.tools).toString(), details.tip});
        for (String categoryId : details.categoryIds) {
            db.execSQL("INSERT INTO RecipeCategory(recipe_id,category_id) VALUES(?,?)",
                    new Object[]{recipe.id, categoryId});
        }
        for (String dietId : details.dietIds) {
            db.execSQL("INSERT INTO RecipeDiet(recipe_id,diet_id) VALUES(?,?)", new Object[]{recipe.id, dietId});
        }
        for (RecipeIngredient line : details.ingredientLines) {
            db.execSQL("INSERT INTO RecipeIngredient(recipe_id,line_id,ingredient_id,amount_decimal,unit_id,"
                            + "qualitative_note,sort_order) VALUES(?,?,?,?,?,?,?)",
                    new Object[]{recipe.id, line.recipeIngredientId, line.ingredientId, line.amountDecimal,
                            line.unitId, line.qualitativeNote, line.sortOrder});
        }
        for (CookingStep step : details.steps) {
            db.execSQL("INSERT INTO CookingStep(recipe_id,step_id,title,instruction,sort_order,duration_seconds,"
                            + "image_path) VALUES(?,?,?,?,?,?,?)",
                    new Object[]{recipe.id, step.stepId, step.title, step.instruction, step.sortOrder,
                            step.durationSeconds, step.imagePath});
            for (String lineId : step.recipeIngredientIds) {
                db.execSQL("INSERT INTO StepIngredient(recipe_id,step_id,line_id) VALUES(?,?,?)",
                        new Object[]{recipe.id, step.stepId, lineId});
            }
        }
    }

    public Long readRevision() { return readRevision(getReadableDatabase()); }

    private static Long readRevision(SQLiteDatabase db) {
        try (Cursor meta = db.rawQuery("SELECT revision FROM CatalogMeta WHERE id=1", null)) {
            return meta.moveToFirst() ? meta.getLong(0) : null;
        }
    }

    public Catalog readCatalog() {
        SQLiteDatabase db = getReadableDatabase();
        // The revision, dictionaries and recipe rows must come from the same SQLite snapshot.
        db.beginTransactionNonExclusive();
        try {
            Long revision = readRevision(db);
            Map<String, String> categories = readNames(db, "Category");
            Map<String, String> diets = readNames(db, "Diet");
            Map<String, Ingredient> ingredients = readIngredients(db);
            List<Recipe> recipes = new ArrayList<>();
            try (Cursor rows = db.rawQuery("SELECT " + RECIPE_COLUMNS + " FROM Recipe "
                    + "WHERE publication_status='PUBLISHED' ORDER BY recipe_id", null)) {
                while (rows.moveToNext()) recipes.add(readRecipe(db, rows, categories, diets, ingredients));
            }
            Set<String> removed = new LinkedHashSet<>();
            try (Cursor rows = db.rawQuery("SELECT recipe_id FROM RemovedRecipe UNION "
                    + "SELECT recipe_id FROM Recipe WHERE publication_status='ARCHIVED'", null)) {
                while (rows.moveToNext()) removed.add(rows.getString(0));
            }
            db.setTransactionSuccessful();
            return new Catalog(revision, recipes, categories, diets, ingredients, removed);
        } finally {
            db.endTransaction();
        }
    }

    public Lookup findById(String recipeId) {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransactionNonExclusive();
        try {
            Lookup result = Lookup.missing();
            try (Cursor row = db.rawQuery("SELECT " + RECIPE_COLUMNS + " FROM Recipe WHERE recipe_id=?",
                    new String[]{recipeId})) {
                if (row.moveToFirst()) {
                    Recipe.PublicationStatus status = Recipe.PublicationStatus.valueOf(row.getString(9));
                    if (status == Recipe.PublicationStatus.PUBLISHED) {
                        result = Lookup.available(readRecipe(db, row, readNames(db, "Category"),
                                readNames(db, "Diet"), readIngredients(db)));
                    } else {
                        result = status == Recipe.PublicationStatus.ARCHIVED ? Lookup.removed() : Lookup.missing();
                    }
                }
            }
            if (result.status == Lookup.Status.MISSING) {
                try (Cursor removed = db.rawQuery("SELECT 1 FROM RemovedRecipe WHERE recipe_id=?",
                        new String[]{recipeId})) {
                    if (removed.moveToFirst()) result = Lookup.removed();
                }
            }
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    private static Recipe readRecipe(SQLiteDatabase db, Cursor row, Map<String, String> categories,
                                     Map<String, String> diets, Map<String, Ingredient> ingredients) {
        String id = row.getString(0);
        String[] key = {id};
        List<RecipeIngredient> lines = new ArrayList<>();
        try (Cursor values = db.rawQuery("SELECT line_id,ingredient_id,amount_decimal,unit_id,qualitative_note,sort_order "
                + "FROM RecipeIngredient WHERE recipe_id=? ORDER BY sort_order", key)) {
            while (values.moveToNext()) {
                lines.add(new RecipeIngredient(values.getString(0), id, values.getString(1), values.getString(2),
                        values.getString(3), values.getString(4), values.getInt(5)));
            }
        }
        List<CookingStep> steps = new ArrayList<>();
        try (Cursor values = db.rawQuery("SELECT step_id,title,instruction,sort_order,duration_seconds,image_path "
                + "FROM CookingStep WHERE recipe_id=? ORDER BY sort_order", key)) {
            while (values.moveToNext()) {
                String stepId = values.getString(0);
                List<String> refs = readColumn(db, "SELECT line_id FROM StepIngredient WHERE recipe_id=? AND step_id=? "
                        + "ORDER BY line_id", id, stepId);
                steps.add(new CookingStep(stepId, id, values.getString(1), values.getString(2), values.getInt(3),
                        nullableInt(values, 4), values.getString(5), refs));
            }
        }
        Recipe.Details details = new Recipe.Details(row.getString(2), row.getString(3), row.getInt(4),
                nullableInt(row, 5), nullableInt(row, 6), nullableInt(row, 7),
                Recipe.PublicationStatus.valueOf(row.getString(9)), nullableLong(row, 11), nullableLong(row, 12),
                Recipe.IngredientDataStatus.valueOf(row.getString(10)),
                readColumn(db, "SELECT category_id FROM RecipeCategory WHERE recipe_id=? ORDER BY category_id", key),
                readColumn(db, "SELECT diet_id FROM RecipeDiet WHERE recipe_id=? ORDER BY diet_id", key),
                decodeStrings(row.getString(13)), row.getString(14), lines, steps);
        return CatalogSnapshotMapper.projectRecipe(id, row.getString(1), row.getString(8), details,
                categories, diets, ingredients);
    }

    private static Map<String, String> readNames(SQLiteDatabase db, String table) {
        Map<String, String> result = new LinkedHashMap<>();
        try (Cursor rows = db.rawQuery("SELECT id,name FROM " + table + " ORDER BY id", null)) {
            while (rows.moveToNext()) result.put(rows.getString(0), rows.getString(1));
        }
        return result;
    }

    private static Map<String, Ingredient> readIngredients(SQLiteDatabase db) {
        Map<String, Ingredient> result = new LinkedHashMap<>();
        try (Cursor rows = db.rawQuery("SELECT id,name,group_name,status,aliases_json FROM Ingredient ORDER BY id", null)) {
            while (rows.moveToNext()) {
                String id = rows.getString(0);
                result.put(id, new Ingredient(id, rows.getString(1), rows.getString(2),
                        Ingredient.Status.valueOf(rows.getString(3)), decodeStrings(rows.getString(4))));
            }
        }
        return result;
    }

    private static List<String> readColumn(SQLiteDatabase db, String sql, String... args) {
        List<String> result = new ArrayList<>();
        try (Cursor rows = db.rawQuery(sql, args)) {
            while (rows.moveToNext()) result.add(rows.getString(0));
        }
        return result;
    }

    private static List<String> decodeStrings(String json) {
        try {
            JSONArray array = new JSONArray(json);
            List<String> result = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++) result.add(array.getString(i));
            return result;
        } catch (JSONException error) {
            throw new IllegalStateException("invalid string list in local catalog", error);
        }
    }

    private static Integer nullableInt(Cursor cursor, int column) {
        return cursor.isNull(column) ? null : cursor.getInt(column);
    }

    private static Long nullableLong(Cursor cursor, int column) {
        return cursor.isNull(column) ? null : cursor.getLong(column);
    }
}
