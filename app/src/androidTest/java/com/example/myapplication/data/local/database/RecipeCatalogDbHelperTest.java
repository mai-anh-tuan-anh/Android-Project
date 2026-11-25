package com.example.myapplication.data.local.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.myapplication.data.local.database.RecipeCatalogDbHelper.ApplyResult;
import com.example.myapplication.data.mapper.CatalogSnapshotMapper;
import com.example.myapplication.data.mapper.CatalogSnapshotMapper.Snapshot;
import com.example.myapplication.data.repository.CatalogRefreshService;
import com.example.myapplication.data.repository.LocalRecipeCatalogRepository;
import com.example.myapplication.domain.model.CookingStep;
import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.domain.repository.RecipeCatalogRepository.Catalog;
import com.example.myapplication.domain.repository.RecipeCatalogRepository.Lookup;
import com.example.myapplication.domain.usecase.ServingCalculator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class RecipeCatalogDbHelperTest {
    private static final String CATALOG_DB = "n2_07_catalog_test.db";
    private static final String PERSONAL_DB = "n2_07_personal_test.db";
    private static final String EMPTY = "{\"meta\":{\"schemaVersion\":1,\"revision\":0}}";

    private Context context;
    private RecipeCatalogDbHelper database;
    private LocalRecipeCatalogRepository repository;
    private String fixture;

    @Before
    public void setUp() throws IOException {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(CATALOG_DB);
        context.deleteDatabase(PERSONAL_DB);
        openCatalog();
        try (InputStream stream = InstrumentationRegistry.getInstrumentation().getContext().getAssets()
                .open("catalog/snapshot-v1.json")) {
            fixture = new String(readBytes(stream), StandardCharsets.UTF_8);
        }
    }

    @After
    public void tearDown() {
        database.close();
        context.deleteDatabase(CATALOG_DB);
        context.deleteDatabase(PERSONAL_DB);
    }

    @Test
    public void parsesFixtureAndReadsFullRecipeAfterReopeningDatabase() {
        Snapshot parsed = CatalogSnapshotMapper.parse(fixture);
        assertEquals(1, parsed.schemaVersion);
        assertEquals(12L, parsed.revision);
        assertEquals(Recipe.PublicationStatus.DRAFT, parsed.recipes.get("recipe-draft").details.publicationStatus);
        assertEquals("100.2500", parsed.recipes.get("recipe-soup").details.ingredientLines.get(0).amountDecimal);

        assertEquals(ApplyResult.APPLIED, repository.applySnapshot(fixture));
        database.close();
        openCatalog();

        Catalog catalog = repository.readCatalog();
        assertEquals(Long.valueOf(12), catalog.revision);
        assertEquals(2, catalog.recipes.size());
        assertEquals("Món canh", catalog.categories.get("cat-soup"));
        assertEquals("Món chay", catalog.diets.get("diet-plant"));
        assertEquals(Collections.singletonList("Cà chua đỏ"), catalog.ingredients.get("ing-tomato").aliases);
        Recipe soup = recipe("recipe-soup");
        Recipe.Details details = soup.details;
        assertEquals(3, details.baseServings);
        assertEquals("Dữ liệu kỹ thuật cho kiểm thử catalog.", details.description);
        assertEquals("recipes/recipe-soup/cover-v2.webp", details.imagePath);
        assertEquals(Recipe.PublicationStatus.PUBLISHED, details.publicationStatus);
        assertEquals(Recipe.IngredientDataStatus.COMPLETE, details.ingredientDataStatus);
        assertEquals(Integer.valueOf(10), details.prepMinutes);
        assertEquals(Integer.valueOf(20), details.cookMinutes);
        assertEquals(Integer.valueOf(30), details.durationMinutes);
        assertEquals(Long.valueOf(1780000000000L), details.publishedAtMillis);
        assertEquals(Long.valueOf(1780100000000L), details.updatedAtMillis);
        assertEquals(Arrays.asList("cat-soup", "cat-viet"), details.categoryIds);
        assertEquals(Arrays.asList("diet-gluten-free", "diet-plant"), details.dietIds);
        assertEquals(Arrays.asList("Nồi", "Dao"), details.tools);
        assertEquals("Thêm rau thơm trước khi dùng.", details.tip);
        assertEquals("Dễ", soup.difficulty);
        assertEquals("100.2500", details.ingredientLines.get(0).amountDecimal);
        assertEquals(1, details.ingredientLines.get(0).sortOrder);
        assertEquals("Vừa đủ", details.ingredientLines.get(1).qualitativeNote);
        assertEquals(4, details.ingredientLines.get(1).sortOrder);
        CookingStep prep = details.steps.get(0);
        assertEquals("step-b-prep", prep.stepId);
        assertEquals(2, prep.sortOrder);
        assertEquals("recipes/recipe-soup/prep-v3.webp", prep.imagePath);
        assertEquals(Collections.singletonList("line-b-tomato"), prep.recipeIngredientIds);
        assertEquals(7, details.steps.get(1).sortOrder);
        assertEquals(Integer.valueOf(300), details.steps.get(1).durationSeconds);
        ServingCalculator.Result scaled = ServingCalculator.calculate(soup, 6);
        assertEquals(0, scaled.ingredients.get(0).amount.compareQuantity(
                ServingCalculator.ExactAmount.of("ing-tomato", "G", new BigDecimal("200.5000"))));

        Recipe partial = recipe("recipe-partial");
        assertEquals(Recipe.IngredientDataStatus.INCOMPLETE, partial.details.ingredientDataStatus);
        assertNull(partial.details.durationMinutes);
        assertNull(partial.details.publishedAtMillis);
        assertNull(partial.details.ingredientLines.get(0).amountDecimal);
        assertNull(partial.details.ingredientLines.get(0).unitId);
        assertEquals(Collections.singletonList("line-not-yet-supplied"), partial.details.steps.get(0).recipeIngredientIds);
        assertFalse(partial.details.hasIngredientsForServingAdjustment());
    }

    @Test
    public void distinguishesMissingRemovedAndEmptyCatalogFromNeverLoaded() {
        assertNull(repository.readCatalog().revision);
        assertTrue(repository.readCatalog().recipes.isEmpty());
        assertEquals(Lookup.Status.MISSING, repository.findById("recipe-soup").status);

        assertEquals(ApplyResult.APPLIED, repository.applySnapshot(EMPTY));
        assertEquals(Long.valueOf(0), repository.readCatalog().revision);
        assertTrue(repository.readCatalog().recipes.isEmpty());

        repository.applySnapshot(fixture);
        assertEquals(Lookup.Status.AVAILABLE, repository.findById("recipe-soup").status);
        assertEquals(Lookup.Status.REMOVED, repository.findById("recipe-removed").status);
        assertEquals(Lookup.Status.REMOVED, repository.findById("recipe-archived").status);
        assertEquals(Lookup.Status.MISSING, repository.findById("recipe-draft").status);
        assertEquals(Lookup.Status.MISSING, repository.findById("unknown").status);
        assertNull(repository.findById("recipe-removed").recipe);
    }

    @Test
    public void newerRevisionReplacesAndOlderOrIdenticalRevisionDoesNotWrite() {
        repository.applySnapshot(fixture);
        String reordered = fixture.replace("\"schemaVersion\": 1, \"revision\": 12", "\"revision\":12,\"schemaVersion\":1");
        assertEquals(ApplyResult.UNCHANGED, repository.applySnapshot(reordered));
        assertEquals(2, repository.readCatalog().recipes.size());

        String conflicting = fixture.replace("100.2500", "100.250");
        assertThrows(IllegalArgumentException.class, () -> repository.applySnapshot(conflicting));
        assertEquals("100.2500", recipe("recipe-soup").details.ingredientLines.get(0).amountDecimal);

        String newer = "{\"meta\":{\"schemaVersion\":1,\"revision\":13},\"removed\":{"
                + "\"recipe-soup\":{\"removedAtMillis\":1780200000000}}}";
        assertEquals(ApplyResult.APPLIED, repository.applySnapshot(newer));
        assertEquals(Long.valueOf(13), repository.readCatalog().revision);
        assertTrue(repository.readCatalog().recipes.isEmpty());
        assertTrue(repository.readCatalog().ingredients.isEmpty());
        assertEquals(Lookup.Status.REMOVED, repository.findById("recipe-soup").status);
        assertEquals(ApplyResult.STALE, repository.applySnapshot(fixture));
        assertEquals(Long.valueOf(13), repository.readCatalog().revision);
        assertEquals(Lookup.Status.REMOVED, repository.findById("recipe-soup").status);
    }

    @Test
    public void malformedInvalidOrDuplicatePayloadKeepsTheLastSnapshot() {
        repository.applySnapshot(fixture);
        for (String bad : Arrays.asList("{broken", fixture + "{}",
                fixture.replace("\"revision\": 12", "\"revision\":13,\"revision\":14"),
                fixture.replace("\"recipe-partial\":", "\"recipe-soup\":"),
                fixture.replace("\"schemaVersion\": 1", "\"schemaVersion\": 2"),
                fixture.replace("\"line-a-herb\": true, \"line-b-tomato\": true", "\"unknown-line\":true"),
                fixture.replace("\"sortOrder\": 4", "\"sortOrder\": 1"),
                fixture.replace("\"unitId\": \"G\"", "\"unitId\": \"gram\""))) {
            assertThrows(IllegalArgumentException.class, () -> repository.applySnapshot(bad));
        }
        assertEquals(Long.valueOf(12), repository.readCatalog().revision);
        assertEquals("100.2500", recipe("recipe-soup").details.ingredientLines.get(0).amountDecimal);
        assertEquals(Lookup.Status.REMOVED, repository.findById("recipe-removed").status);
    }

    @Test
    public void rollsBackRecipeRowsDictionariesAndRevisionOnWriteFailure() {
        repository.applySnapshot(fixture);
        database.getWritableDatabase().execSQL("CREATE TRIGGER fail_step BEFORE INSERT ON CookingStep "
                + "WHEN NEW.step_id='step-a-cook' BEGIN SELECT RAISE(ABORT, 'test step write failure'); END");
        String newer = fixture.replace("\"revision\": 12", "\"revision\": 13")
                .replace("Canh mẫu kiểm thử", "Tên sau cập nhật").replace("Món canh", "Danh mục mới");

        assertThrows(SQLiteException.class, () -> repository.applySnapshot(newer));
        assertEquals(Long.valueOf(12), repository.readCatalog().revision);
        assertEquals("Món canh", repository.readCatalog().categories.get("cat-soup"));
        assertEquals("Canh mẫu kiểm thử", recipe("recipe-soup").title);
        assertEquals(2, recipe("recipe-soup").details.steps.size());
        assertEquals(Lookup.Status.REMOVED, repository.findById("recipe-removed").status);
        assertEquals(ApplyResult.UNCHANGED, repository.applySnapshot(fixture));
    }

    @Test
    public void ioFailureIsNotReportedAsMissing() {
        repository.applySnapshot(fixture);
        database.getWritableDatabase().execSQL("DROP TABLE RemovedRecipe");
        assertThrows(SQLiteException.class, () -> repository.findById("unknown"));
    }

    @Test
    public void replacementLeavesThePersonalDatabaseIntact() throws IOException {
        try (PersonalDataDbHelper personal = new PersonalDataDbHelper(context, PERSONAL_DB)) {
            Recipe saved = new Recipe("recipe-soup", "Món đã lưu", "", "30 phút", "Dễ");
            personal.addFavorite(saved);
            personal.saveNote(saved, "Ghi chú của tôi");
            personal.addMealPlanRecipe(saved, 4);
            assertTrue(personal.saveCookingLog(null, saved, 1780100000000L, 4, 28, 5, "Nêm nhạt", null));
        }
        byte[] before = databaseBytes(PERSONAL_DB);

        repository.applySnapshot(fixture);
        repository.applySnapshot("{\"meta\":{\"schemaVersion\":1,\"revision\":13},\"removed\":{"
                + "\"recipe-soup\":{\"removedAtMillis\":1780200000000}}}");

        assertArrayEquals(before, databaseBytes(PERSONAL_DB));
        try (PersonalDataDbHelper personal = new PersonalDataDbHelper(context, PERSONAL_DB)) {
            assertTrue(personal.isFavorite("recipe-soup"));
            assertEquals("Ghi chú của tôi", personal.getNote("recipe-soup"));
            assertEquals(4, personal.getMealPlanRecipes().get(0).servings);
            assertEquals("Nêm nhạt", personal.getCookingLogs("recipe-soup").get(0).adjustmentNote);
            try (Cursor version = personal.getReadableDatabase().rawQuery("PRAGMA user_version", null)) {
                assertTrue(version.moveToFirst());
                assertEquals(10, version.getInt(0));
            }
        }
    }

    @Test
    public void knownTombstonesSurviveLaterSnapshotsUntilRepublished() {
        repository.applySnapshot(fixture);
        repository.applySnapshot("{\"meta\":{\"schemaVersion\":1,\"revision\":13},\"removed\":{"
                + "\"recipe-soup\":{\"removedAtMillis\":1780200000000}}}");
        repository.applySnapshot("{\"meta\":{\"schemaVersion\":1,\"revision\":14}}");

        assertEquals(Lookup.Status.REMOVED, repository.findById("recipe-soup").status);
        assertEquals(Lookup.Status.REMOVED, repository.findById("recipe-archived").status);
        assertEquals(Lookup.Status.REMOVED, repository.findById("recipe-removed").status);
        assertEquals(Lookup.Status.MISSING, repository.findById("unknown").status);
        assertTrue(repository.readCatalog().removedRecipeIds.contains("recipe-soup"));

        assertEquals(ApplyResult.APPLIED, repository.applySnapshot(
                fixture.replace("\"revision\": 12", "\"revision\": 15")));
        assertEquals(Lookup.Status.AVAILABLE, repository.findById("recipe-soup").status);
        assertFalse(repository.readCatalog().removedRecipeIds.contains("recipe-soup"));
    }

    @Test
    public void refreshUsesLocalRevisionRulesForRtdbSnapshots() throws Exception {
        // RTDB omits optional empty nodes. A catalog with metadata alone is valid.
        assertEquals(ApplyResult.APPLIED, refresh(EMPTY));
        assertEquals(Long.valueOf(0), repository.readCatalog().revision);
        assertTrue(repository.readCatalog().recipes.isEmpty());

        assertEquals(ApplyResult.APPLIED, refresh(fixture));
        assertEquals(ApplyResult.STALE, refresh(EMPTY));
        String reordered = fixture.replace("\"schemaVersion\": 1, \"revision\": 12",
                "\"revision\":12,\"schemaVersion\":1");
        assertEquals(ApplyResult.UNCHANGED, refresh(reordered));
        assertEquals(Long.valueOf(12), repository.readCatalog().revision);
        assertEquals("100.2500", recipe("recipe-soup").details.ingredientLines.get(0).amountDecimal);
    }

    @Test
    public void failedRefreshKeepsCatalogAndPersonalData() throws Exception {
        try (PersonalDataDbHelper personal = new PersonalDataDbHelper(context, PERSONAL_DB)) {
            Recipe saved = new Recipe("recipe-soup", "Món đã lưu", "", "30 phút", "Dễ");
            personal.saveNote(saved, "Ghi chú của tôi");
        }
        byte[] before = databaseBytes(PERSONAL_DB);
        assertEquals(ApplyResult.APPLIED, refresh(fixture));

        CatalogRefreshService offline = new CatalogRefreshService(() -> {
            throw new IOException("offline");
        }, repository::applySnapshot, Runnable::run);
        assertTrue(assertThrows(ExecutionException.class, () -> offline.refresh().get())
                .getCause() instanceof IOException);
        for (String rejected : Arrays.asList("null", "{broken", fixture.replace("100.2500", "200.2500"))) {
            assertTrue(assertThrows(ExecutionException.class, () -> refresh(rejected))
                    .getCause() instanceof IllegalArgumentException);
        }

        assertEquals(Long.valueOf(12), repository.readCatalog().revision);
        assertEquals("100.2500", recipe("recipe-soup").details.ingredientLines.get(0).amountDecimal);
        assertArrayEquals(before, databaseBytes(PERSONAL_DB));
    }

    private ApplyResult refresh(String snapshot) throws Exception {
        return new CatalogRefreshService(() -> snapshot, repository::applySnapshot, Runnable::run).refresh().get();
    }

    private void openCatalog() {
        database = new RecipeCatalogDbHelper(context, CATALOG_DB);
        repository = new LocalRecipeCatalogRepository(database);
    }

    private Recipe recipe(String id) {
        Lookup result = repository.findById(id);
        assertEquals(Lookup.Status.AVAILABLE, result.status);
        return result.recipe;
    }

    private byte[] databaseBytes(String name) throws IOException {
        try (InputStream stream = new FileInputStream(context.getDatabasePath(name))) {
            return readBytes(stream);
        }
    }

    private static byte[] readBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = stream.read(buffer)) != -1) bytes.write(buffer, 0, count);
        return bytes.toByteArray();
    }
}
