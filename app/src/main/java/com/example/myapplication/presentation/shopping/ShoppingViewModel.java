package com.example.myapplication.presentation.shopping;



import androidx.lifecycle.ViewModel;
import com.example.myapplication.domain.model.*;
import com.example.myapplication.domain.repository.PersonalDataRepository;
import com.example.myapplication.domain.catalog.KitchenUnitCatalog;
import com.example.myapplication.domain.catalog.ShoppingCategoryCatalog;
import com.example.myapplication.presentation.pantry.PantryViewModel;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Screen state and validation for module 7's current local shopping list. */
public final class ShoppingViewModel extends ViewModel {
    public static final String NEED_TO_BUY = "NEED_TO_BUY";
    public static final String AVAILABLE = "AVAILABLE";

    private final PersonalDataRepository repository;

    public ShoppingViewModel(PersonalDataRepository repository) {
        this.repository = repository;
    }

    public List<ShoppingListItem> getItems() { return repository.getShoppingItems(); }

    public String initialStatusForPantry(String ingredientId, String name, String amount, String unitId) {
        return initialStatusForPantry(ingredientId, name, amount, unitId,
                repository.getPantryItems(), System.currentTimeMillis());
    }

    public static String initialStatusForPantry(String ingredientId, String name, String amount, String unitId,
                                         List<PantryIngredient> pantryItems, long nowMillis) {
        if (name == null || amount == null || unitId == null || unitId.trim().isEmpty()) return NEED_TO_BUY;
        final BigDecimal requested;
        try {
            requested = new BigDecimal(amount.trim().replace(',', '.'));
            if (requested.signum() <= 0) return NEED_TO_BUY;
        } catch (NumberFormatException qualitativeAmount) {
            return NEED_TO_BUY;
        }

        String expectedName = normalizeIngredientName(name);
        String expectedUnit = KitchenUnitCatalog.canonicalId(unitId);
        if (expectedName.isEmpty() || expectedUnit.isEmpty()) return NEED_TO_BUY;
        BigDecimal available = BigDecimal.ZERO;
        for (PantryIngredient item : pantryItems) {
            if (item == null || item.amountDecimal == null || item.unitId == null) continue;
            if (ingredientId != null && !ingredientId.trim().isEmpty()) {
                if (!ingredientId.equals(item.ingredientId)) continue;
            } else if (!expectedName.equals(normalizeIngredientName(item.name))) {
                continue;
            }
            if (!expectedUnit.equals(KitchenUnitCatalog.canonicalId(item.unitId))) continue;
            if ("EXPIRED".equals(PantryViewModel.expiryStateForDate(item.expiryDate, nowMillis))) continue;
            try {
                BigDecimal pantryAmount = new BigDecimal(item.amountDecimal.trim());
                if (pantryAmount.signum() > 0) available = available.add(pantryAmount);
            } catch (NumberFormatException invalidStoredAmount) {
                // Malformed/qualitative pantry amounts cannot establish that enough is available.
            }
        }
        return available.compareTo(requested) >= 0 ? AVAILABLE : NEED_TO_BUY;
    }

    private static String normalizeIngredientName(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd');
        return normalized.replaceAll("\\s+", " ");
    }

    public ShoppingListItem findItem(String id) {
        if (id == null) return null;
        for (ShoppingListItem item : getItems()) {
            if (item.id.equals(id)) return item;
        }
        return null;
    }

    public boolean saveItem(String id, String name, String amount, String unit, String category, String status) {
        if (name == null || name.trim().isEmpty() || amount == null || amount.trim().isEmpty()
                || unit == null || unit.trim().isEmpty() || !ShoppingCategoryCatalog.contains(category)) return false;
        try {
            BigDecimal numericAmount = new BigDecimal(amount.trim().replace(',', '.'));
            if (numericAmount.signum() <= 0) return false;
            BigDecimal rounded = numericAmount.setScale(2, RoundingMode.HALF_UP);
            if (rounded.signum() <= 0) return false;
            amount = rounded.stripTrailingZeros().toPlainString();
        } catch (NumberFormatException qualitativeAmount) {
            // Text quantities such as “vừa đủ” stay qualitative and are never arithmetically combined.
        }
        if (!isValidStatus(status)) return false;
        repository.saveShoppingItem(id, name, amount, unit, category, status);
        return true;
    }

    public void setStatus(String id, String status) {
        if (id != null && isValidStatus(status)) repository.setShoppingItemStatus(id, status);
    }

    public void deleteItem(String id) {
        if (id != null) repository.deleteShoppingItem(id);
    }

    public void clearItems() { repository.clearShoppingItems(); }

    public PlanUpdate checkForPlanChanges() {
        List<PlannedRecipe> currentPlan = repository.getMealPlanRecipes();
        String currentSnapshot = serializePlan(currentPlan);
        String previousSnapshot = repository.getShoppingPlanSnapshot();
        if (previousSnapshot.isEmpty()) {
            repository.saveShoppingPlanSnapshot(currentSnapshot);
            return null;
        }
        if (previousSnapshot.equals(currentSnapshot)) return null;

        List<PlanUpdateRow> changes = comparePlans(previousSnapshot, currentPlan);
        if (changes.isEmpty()) {
            repository.saveShoppingPlanSnapshot(currentSnapshot);
            return null;
        }
        return new PlanUpdate(currentSnapshot, changes, buildPlanChangeMessage(changes));
    }

    public void acceptPlanUpdate(PlanUpdate update) {
        if (update != null) repository.saveShoppingPlanSnapshot(update.currentSnapshot);
    }

    private String serializePlan(List<PlannedRecipe> plan) {
        JSONArray json = new JSONArray();
        List<PlannedRecipe> stableOrder = new ArrayList<>(plan);
        stableOrder.sort((left, right) -> left.recipe.id.compareTo(right.recipe.id));
        for (PlannedRecipe item : stableOrder) {
            JSONObject row = new JSONObject();
            try {
                row.put("recipeId", item.recipe.id);
                row.put("title", item.recipe.title);
                row.put("servings", item.servings);
                json.put(row);
            } catch (JSONException ignored) {
                return "[]";
            }
        }
        return json.toString();
    }

    private List<PlanUpdateRow> comparePlans(String previousSnapshot,
                                              List<PlannedRecipe> currentPlan) {
        Map<String, PlanUpdateRow> previousById = new LinkedHashMap<>();
        try {
            JSONArray previous = new JSONArray(previousSnapshot);
            for (int i = 0; i < previous.length(); i++) {
                JSONObject row = previous.getJSONObject(i);
                String id = row.optString("recipeId", "");
                if (!id.isEmpty()) previousById.put(id,
                        new PlanUpdateRow(id, row.optString("title", "Món ăn"), row.optInt("servings"), 0));
            }
        } catch (JSONException invalidSnapshot) {
            previousById.clear();
        }

        Map<String, PlanUpdateRow> currentById = new LinkedHashMap<>();
        for (PlannedRecipe item : currentPlan) {
            currentById.put(item.recipe.id,
                    new PlanUpdateRow(item.recipe.id, item.recipe.title, item.servings, item.servings));
        }
        List<PlanUpdateRow> changes = new ArrayList<>();
        for (PlanUpdateRow old : previousById.values()) {
            PlanUpdateRow current = currentById.get(old.recipeId);
            if (current == null) {
                changes.add(new PlanUpdateRow(old.recipeId, old.title, old.previousServings, 0));
            } else if (current.currentServings != old.previousServings) {
                changes.add(new PlanUpdateRow(old.recipeId, current.title, old.previousServings,
                        current.currentServings));
            }
        }
        for (PlanUpdateRow current : currentById.values()) {
            if (!previousById.containsKey(current.recipeId)) {
                changes.add(new PlanUpdateRow(current.recipeId, current.title, 0, current.currentServings));
            }
        }
        return changes;
    }

    private String buildPlanChangeMessage(List<PlanUpdateRow> changes) {
        if (changes.size() == 1) {
            PlanUpdateRow change = changes.get(0);
            if (change.previousServings > 0 && change.currentServings > 0) {
                return "Bạn đã đổi khẩu phần " + change.title + " từ " + change.previousServings +
                        " lên " + change.currentServings + " người.";
            }
            if (change.previousServings == 0) return "Bạn đã thêm " + change.title + " vào kế hoạch.";
            return "Bạn đã bỏ " + change.title + " khỏi kế hoạch.";
        }
        return "Kế hoạch Nấu hôm nay đã thay đổi " + changes.size() + " món.";
    }

    public static final class PlanUpdate {
        public final String currentSnapshot;
        public final List<PlanUpdateRow> changes;
        public final String message;
        public PlanUpdate(String currentSnapshot, List<PlanUpdateRow> changes, String message) {
            this.currentSnapshot = currentSnapshot;
            this.changes = changes;
            this.message = message;
        }
    }

    public static final class PlanUpdateRow {
        public final String recipeId;
        public final String title;
        public final int previousServings;
        public final int currentServings;
        public PlanUpdateRow(String recipeId, String title, int previousServings, int currentServings) {
            this.recipeId = recipeId;
            this.title = title;
            this.previousServings = previousServings;
            this.currentServings = currentServings;
        }
        public String servingChangeLabel() {
            if (previousServings == 0) return "Thêm · " + currentServings + " người";
            if (currentServings == 0) return "Đã bỏ · " + previousServings + " người";
            return previousServings + " người → " + currentServings + " người";
        }
    }

    private boolean isValidStatus(String status) {
        return NEED_TO_BUY.equals(status) || AVAILABLE.equals(status);
    }
}
