package com.example.myapplication.data.mapper;

import android.util.JsonReader;
import android.util.JsonToken;

import com.example.myapplication.data.image.HostingImageUrlResolver;

import com.example.myapplication.domain.model.CookingStep;
import com.example.myapplication.domain.model.Ingredient;
import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.domain.model.RecipeIngredient;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/** Validates the value of /catalog and resolves its IDs into domain models. */
public final class CatalogSnapshotMapper {
    private static final int SUPPORTED_SCHEMA = 1;
    private static final int MAX_JSON_DEPTH = 64;
    private static final Pattern INVALID_ID = Pattern.compile("[.#$\\[\\]/\\p{Cntrl}]");

    private CatalogSnapshotMapper() { }

    public static Snapshot parse(String json) {
        if (json == null) throw invalid("catalog JSON is missing");
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            Object root = readJson(reader, 0);
            if (reader.peek() != JsonToken.END_DOCUMENT) throw invalid("trailing content after catalog JSON");
            return map(asObject(root, "catalog"));
        } catch (IOException | IllegalStateException error) {
            throw new IllegalArgumentException("invalid catalog JSON", error);
        }
    }

    static Snapshot map(Map<String, Object> root) {
        Map<String, Object> meta = asObject(root.get("meta"), "meta");
        int schemaVersion = requiredInt(meta, "schemaVersion");
        if (schemaVersion != SUPPORTED_SCHEMA) throw invalid("unsupported catalog schemaVersion " + schemaVersion);
        long revision = requiredLong(meta, "revision");
        Map<String, String> categories = namedEntries(root, "categories");
        Map<String, String> diets = namedEntries(root, "diets");
        Map<String, Ingredient> ingredients = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : optionalObject(root, "ingredients").entrySet()) {
            String id = validateId(entry.getKey());
            Map<String, Object> value = asObject(entry.getValue(), "ingredient " + id);
            ingredients.put(id, new Ingredient(id, requiredText(value, "name"), optionalText(value, "groupName"),
                    enumValue(Ingredient.Status.class, value, "status"), stringList(value, "aliases")));
        }

        Map<String, Recipe> recipes = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : optionalObject(root, "recipes").entrySet()) {
            String id = validateId(entry.getKey());
            try {
                recipes.put(id, recipe(id, asObject(entry.getValue(), "recipe"), categories, diets, ingredients));
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException("recipe " + id + ": " + error.getMessage(), error);
            }
        }

        Map<String, Long> removed = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : optionalObject(root, "removed").entrySet()) {
            String id = validateId(entry.getKey());
            if (recipes.containsKey(id)) throw invalid("recipe is also listed as removed: " + id);
            removed.put(id, requiredLong(asObject(entry.getValue(), "removed recipe " + id), "removedAtMillis"));
        }
        return new Snapshot(schemaVersion, revision, fingerprint(root), categories, diets, ingredients, recipes, removed);
    }

    private static Recipe recipe(String id, Map<String, Object> value, Map<String, String> categories,
                                 Map<String, String> diets, Map<String, Ingredient> ingredients) {
        List<String> categoryIds = references(value, "categoryIds");
        List<String> dietIds = references(value, "dietIds");
        for (String categoryId : categoryIds) requireReference(categories, categoryId, "category");
        for (String dietId : dietIds) requireReference(diets, dietId, "diet");

        List<RecipeIngredient> lines = new ArrayList<>();
        for (Map.Entry<String, Object> entry : optionalObject(value, "ingredientLines").entrySet()) {
            Map<String, Object> line = asObject(entry.getValue(), "ingredient line");
            String ingredientId = requiredText(line, "ingredientId");
            requireReference(ingredients, ingredientId, "ingredient");
            lines.add(new RecipeIngredient(validateId(entry.getKey()), id, ingredientId,
                    optionalText(line, "amountDecimal"), optionalText(line, "unitId"),
                    optionalText(line, "qualitativeNote"), requiredInt(line, "sortOrder")));
        }

        List<CookingStep> steps = new ArrayList<>();
        for (Map.Entry<String, Object> entry : optionalObject(value, "steps").entrySet()) {
            Map<String, Object> step = asObject(entry.getValue(), "cooking step");
            steps.add(new CookingStep(validateId(entry.getKey()), id, requiredText(step, "title"),
                    requiredText(step, "instruction"), requiredInt(step, "sortOrder"),
                    optionalInt(step, "durationSeconds"), imagePath(step), references(step, "recipeIngredientIds")));
        }

        Recipe.Details details = new Recipe.Details(optionalText(value, "description"), imagePath(value),
                requiredInt(value, "baseServings"), optionalInt(value, "prepMinutes"),
                optionalInt(value, "cookMinutes"), optionalInt(value, "durationMinutes"),
                enumValue(Recipe.PublicationStatus.class, value, "publicationStatus"),
                optionalLong(value, "publishedAtMillis"), optionalLong(value, "updatedAtMillis"),
                enumValue(Recipe.IngredientDataStatus.class, value, "ingredientDataStatus"),
                categoryIds, dietIds, stringList(value, "tools"), optionalText(value, "tip"), lines, steps);
        return projectRecipe(id, requiredText(value, "title"), difficultyLabel(optionalText(value, "difficulty")),
                details, categories, diets, ingredients);
    }

    /** Projects catalog labels into existing Recipe fields without discarding the canonical IDs. */
    public static Recipe projectRecipe(String id, String title, String difficulty, Recipe.Details details,
                                       Map<String, String> categories, Map<String, String> diets,
                                       Map<String, Ingredient> ingredients) {
        List<String> categoryNames = new ArrayList<>();
        for (String categoryId : details.categoryIds) categoryNames.add(categories.get(categoryId));
        List<String> ingredientNames = new ArrayList<>();
        for (RecipeIngredient line : details.ingredientLines) {
            ingredientNames.add(ingredients.get(line.ingredientId).canonicalName);
        }
        // The legacy field can represent one diet. Multi-diet consumers use details.dietIds.
        String diet = details.dietIds.size() == 1 ? diets.get(details.dietIds.get(0)) : null;
        return Recipe.fromCatalog(id, title, "", difficulty, diet, categoryNames, ingredientNames, details);
    }

    private static String difficultyLabel(String code) {
        if (code == null) return null;
        switch (code) {
            case "EASY": return "Dễ";
            case "MEDIUM": return "Vừa";
            case "HARD": return "Khó";
            default: throw invalid("unknown difficulty " + code);
        }
    }

    private static String imagePath(Map<String, Object> fields) {
        String value = optionalText(fields, "imagePath");
        if (value != null) HostingImageUrlResolver.relativePath(value);
        return value;
    }

    private static Map<String, String> namedEntries(Map<String, Object> root, String key) {
        Map<String, String> names = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : optionalObject(root, key).entrySet()) {
            names.put(validateId(entry.getKey()), requiredText(asObject(entry.getValue(), key), "name"));
        }
        return names;
    }

    private static List<String> references(Map<String, Object> values, String key) {
        List<String> ids = new ArrayList<>();
        for (Map.Entry<String, Object> entry : optionalObject(values, key).entrySet()) {
            if (!Boolean.TRUE.equals(entry.getValue())) throw invalid(key + " references must have value true");
            ids.add(validateId(entry.getKey()));
        }
        Collections.sort(ids);
        return ids;
    }

    private static void requireReference(Map<String, ?> entries, String id, String type) {
        if (!entries.containsKey(id)) throw invalid("unknown " + type + " ID " + id);
    }

    private static String validateId(String id) {
        if (id.trim().isEmpty() || INVALID_ID.matcher(id).find()) throw invalid("invalid catalog ID " + id);
        return id;
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, Map<String, Object> values, String key) {
        String value = requiredText(values, key);
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException error) {
            throw invalid("unknown " + key + " " + value);
        }
    }

    private static List<String> stringList(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null) return Collections.emptyList();
        if (!(value instanceof List)) throw invalid(key + " must be an array");
        List<String> result = new ArrayList<>();
        for (Object element : (List<?>) value) {
            if (!(element instanceof String) || ((String) element).trim().isEmpty()) {
                throw invalid(key + " must contain non-blank strings");
            }
            result.add((String) element);
        }
        return result;
    }

    private static String requiredText(Map<String, Object> values, String key) {
        String value = optionalText(values, key);
        if (value == null || value.trim().isEmpty()) throw invalid(key + " must be a non-blank string");
        return value;
    }

    private static String optionalText(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null || value instanceof String) return (String) value;
        throw invalid(key + " must be a string");
    }

    private static int requiredInt(Map<String, Object> values, String key) {
        long value = requiredLong(values, key);
        if (value > Integer.MAX_VALUE) throw invalid(key + " exceeds integer range");
        return (int) value;
    }

    private static Integer optionalInt(Map<String, Object> values, String key) {
        return values.get(key) == null ? null : requiredInt(values, key);
    }

    private static Long optionalLong(Map<String, Object> values, String key) {
        return values.get(key) == null ? null : requiredLong(values, key);
    }

    private static long requiredLong(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value instanceof BigDecimal) {
            try {
                long number = ((BigDecimal) value).longValueExact();
                if (number >= 0) return number;
            } catch (ArithmeticException error) {
                throw invalid(key + " must be a non-negative integer in range");
            }
        }
        throw invalid(key + " must be a non-negative integer");
    }

    private static Map<String, Object> optionalObject(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? Collections.emptyMap() : asObject(value, key);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asObject(Object value, String key) {
        if (!(value instanceof Map)) throw invalid(key + " must be an object");
        return (Map<String, Object>) value;
    }

    private static Object readJson(JsonReader reader, int depth) throws IOException {
        if (depth > MAX_JSON_DEPTH) throw invalid("catalog JSON is nested too deeply");
        switch (reader.peek()) {
            case BEGIN_OBJECT:
                Map<String, Object> fields = new TreeMap<>();
                reader.beginObject();
                while (reader.hasNext()) {
                    String key = reader.nextName();
                    if (fields.containsKey(key)) throw invalid("duplicate JSON key " + key);
                    fields.put(key, readJson(reader, depth + 1));
                }
                reader.endObject();
                return fields;
            case BEGIN_ARRAY:
                List<Object> elements = new ArrayList<>();
                reader.beginArray();
                while (reader.hasNext()) elements.add(readJson(reader, depth + 1));
                reader.endArray();
                return elements;
            case STRING: return reader.nextString();
            case NUMBER: return new BigDecimal(reader.nextString());
            case BOOLEAN: return reader.nextBoolean();
            case NULL: reader.nextNull(); return null;
            default: throw invalid("unexpected token in catalog JSON");
        }
    }

    private static String fingerprint(Map<String, Object> root) {
        StringBuilder canonical = new StringBuilder();
        appendCanonical(canonical, root);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(Character.forDigit((value & 0xff) >> 4, 16));
                hex.append(Character.forDigit(value & 0xf, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new AssertionError(error);
        }
    }

    // Ignore object key ordering and explicit nulls, which RTDB omits. Keep array order and string text.
    private static void appendCanonical(StringBuilder output, Object value) {
        if (value instanceof Map) {
            output.append('{');
            for (Map.Entry<String, Object> entry : new TreeMap<>(asObject(value, "catalog object")).entrySet()) {
                Object member = entry.getValue();
                if (member == null || (member instanceof Map && ((Map<?, ?>) member).isEmpty())
                        || (member instanceof List && ((List<?>) member).isEmpty())) continue;
                appendCanonical(output, entry.getKey());
                appendCanonical(output, member);
            }
            output.append('}');
        } else if (value instanceof List) {
            output.append('[');
            for (Object element : (List<?>) value) appendCanonical(output, element);
            output.append(']');
        } else if (value instanceof String) {
            String text = (String) value;
            output.append('s').append(text.length()).append(':').append(text);
        } else if (value instanceof BigDecimal) {
            output.append('n').append(((BigDecimal) value).stripTrailingZeros()).append(';');
        } else {
            output.append(value == null ? 'x' : Boolean.TRUE.equals(value) ? 't' : 'f');
        }
    }

    private static IllegalArgumentException invalid(String message) { return new IllegalArgumentException(message); }

    public static final class Snapshot {
        public final int schemaVersion;
        public final long revision;
        public final String fingerprint;
        public final Map<String, String> categories;
        public final Map<String, String> diets;
        public final Map<String, Ingredient> ingredients;
        public final Map<String, Recipe> recipes;
        public final Map<String, Long> removed;

        private Snapshot(int schemaVersion, long revision, String fingerprint, Map<String, String> categories,
                         Map<String, String> diets, Map<String, Ingredient> ingredients,
                         Map<String, Recipe> recipes, Map<String, Long> removed) {
            this.schemaVersion = schemaVersion;
            this.revision = revision;
            this.fingerprint = fingerprint;
            this.categories = Collections.unmodifiableMap(categories);
            this.diets = Collections.unmodifiableMap(diets);
            this.ingredients = Collections.unmodifiableMap(ingredients);
            this.recipes = Collections.unmodifiableMap(recipes);
            this.removed = Collections.unmodifiableMap(removed);
        }
    }
}
