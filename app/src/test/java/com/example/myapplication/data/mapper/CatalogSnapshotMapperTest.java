package com.example.myapplication.data.mapper;

import com.example.myapplication.domain.model.Recipe;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class CatalogSnapshotMapperTest {
    @Test
    public void mapsNullableFieldsAndExplicitStatusWithoutInventingDuration() {
        Recipe recipe = CatalogSnapshotMapper.map(catalog(recipeFields("INCOMPLETE"))).recipes.get("recipe-1");

        assertEquals(Recipe.IngredientDataStatus.INCOMPLETE, recipe.details.ingredientDataStatus);
        assertEquals(Recipe.PublicationStatus.PUBLISHED, recipe.details.publicationStatus);
        assertEquals(Recipe.UNKNOWN_DURATION_MINUTES, recipe.durationMinutes);
        assertNull(recipe.details.durationMinutes);
        assertNull(recipe.details.publishedAtMillis);
        assertFalse(recipe.details.hasIngredientsForServingAdjustment());
    }

    @Test
    public void rejectsUnsupportedSchemaAndNonIntegerRevision() {
        Map<String, Object> root = catalog(recipeFields("UNKNOWN"));
        root.put("meta", Map.of("schemaVersion", BigDecimal.valueOf(2), "revision", BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> CatalogSnapshotMapper.map(root));

        root.put("meta", Map.of("schemaVersion", BigDecimal.ONE, "revision", new BigDecimal("1.5")));
        assertThrows(IllegalArgumentException.class, () -> CatalogSnapshotMapper.map(root));
    }

    @Test
    public void fingerprintIgnoresObjectOrderButPreservesDecimalStringsAndArrayOrder() {
        Map<String, Object> fields = recipeFields("COMPLETE");
        fields.put("tools", Arrays.asList("Nồi", "Dao"));
        fields.put("ingredientLines", Map.of("line", Map.of("ingredientId", "tomato",
                "amountDecimal", "1.2500", "unitId", "G", "sortOrder", BigDecimal.ZERO)));
        Map<String, Object> root = catalog(fields);
        String original = CatalogSnapshotMapper.map(root).fingerprint;

        Map<String, Object> reordered = new LinkedHashMap<>();
        reordered.put("recipes", root.get("recipes"));
        reordered.put("ingredients", root.get("ingredients"));
        reordered.put("meta", Map.of("revision", new BigDecimal("1.0"), "schemaVersion", BigDecimal.ONE));
        reordered.put("removed", null);
        assertEquals(original, CatalogSnapshotMapper.map(reordered).fingerprint);

        fields.put("tools", Arrays.asList("Dao", "Nồi"));
        assertNotEquals(original, CatalogSnapshotMapper.map(root).fingerprint);
        fields.put("tools", Arrays.asList("Nồi", "Dao"));
        fields.put("ingredientLines", Map.of("line", Map.of("ingredientId", "tomato",
                "amountDecimal", "1.25", "unitId", "G", "sortOrder", BigDecimal.ZERO)));
        assertNotEquals(original, CatalogSnapshotMapper.map(root).fingerprint);
    }

    @Test
    public void rejectsInvalidIdsAndUnknownDictionaryReferences() {
        Map<String, Object> fields = recipeFields("UNKNOWN");
        fields.put("categoryIds", Map.of("unknown", true));
        assertThrows(IllegalArgumentException.class, () -> CatalogSnapshotMapper.map(catalog(fields)));
        fields.remove("categoryIds");

        fields.put("dietIds", Map.of("unknown", true));
        assertThrows(IllegalArgumentException.class, () -> CatalogSnapshotMapper.map(catalog(fields)));
        fields.remove("dietIds");

        fields.put("ingredientLines", Map.of("line", Map.of("ingredientId", "unknown", "sortOrder", BigDecimal.ZERO)));
        assertThrows(IllegalArgumentException.class, () -> CatalogSnapshotMapper.map(catalog(fields)));
        fields.remove("ingredientLines");

        Map<String, Object> root = catalog(fields);
        root.put("recipes", Map.of("bad/id", fields));
        assertThrows(IllegalArgumentException.class, () -> CatalogSnapshotMapper.map(root));
    }

    @Test
    public void requiresRelativeImagePathsAndExactDecimalQuantityStrings() {
        Map<String, Object> fields = recipeFields("COMPLETE");
        for (String image : Arrays.asList("https://example.test/photo.webp", "/photo.webp", "recipes/%2e%2e/photo.webp")) {
            fields.put("imagePath", image);
            assertThrows(IllegalArgumentException.class, () -> CatalogSnapshotMapper.map(catalog(fields)));
        }
        fields.put("imagePath", "recipes/recipe-1/cover-v1.webp");
        fields.put("ingredientLines", Map.of("line", Map.of("ingredientId", "tomato", "unitId", "G",
                "amountDecimal", new BigDecimal("1.25"), "sortOrder", BigDecimal.ZERO)));
        assertThrows(IllegalArgumentException.class, () -> CatalogSnapshotMapper.map(catalog(fields)));

        fields.put("ingredientLines", Map.of("line", Map.of("ingredientId", "tomato", "unitId", "gram",
                "amountDecimal", "1.25", "sortOrder", BigDecimal.ZERO)));
        assertThrows(IllegalArgumentException.class, () -> CatalogSnapshotMapper.map(catalog(fields)));
    }

    private static Map<String, Object> catalog(Map<String, Object> recipe) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("meta", Map.of("schemaVersion", BigDecimal.ONE, "revision", BigDecimal.ONE));
        fields.put("ingredients", Map.of("tomato", Map.of("name", "Cà chua", "status", "ACTIVE")));
        fields.put("recipes", Map.of("recipe-1", recipe));
        return fields;
    }

    private static Map<String, Object> recipeFields(String ingredientDataStatus) {
        return new LinkedHashMap<>(Map.of("title", "Món mẫu", "baseServings", BigDecimal.valueOf(2),
                "publicationStatus", "PUBLISHED", "ingredientDataStatus", ingredientDataStatus));
    }
}
