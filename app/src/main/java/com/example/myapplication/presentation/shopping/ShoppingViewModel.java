package com.example.myapplication.presentation.shopping;



import androidx.lifecycle.ViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
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
import java.util.Collections;
import java.util.concurrent.Executor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Screen state and validation for module 7's current local shopping list. */
public final class ShoppingViewModel extends ViewModel {
    public static final String NEED_TO_BUY = "NEED_TO_BUY";
    public static final String AVAILABLE = "AVAILABLE";

    private final PersonalDataRepository repository;
    private final Executor ioExecutor;
    private final MutableLiveData<ShoppingState> shoppingState = new MutableLiveData<>(
            new ShoppingState(Collections.emptyList(), true, null));
    private final MutableLiveData<SaveResult> saveResult = new MutableLiveData<>();
    private final MutableLiveData<PlanUpdate> planUpdate = new MutableLiveData<>();

    public ShoppingViewModel(PersonalDataRepository repository, Executor ioExecutor) {
        this.repository = repository;
        this.ioExecutor = ioExecutor;
    }

    public LiveData<ShoppingState> shoppingState() { return shoppingState; }
    public LiveData<SaveResult> saveResult() { return saveResult; }
    public LiveData<PlanUpdate> planUpdate() { return planUpdate; }

    public List<ShoppingListItem> getItems() {
        ShoppingState state = shoppingState.getValue();
        return state == null ? Collections.emptyList() : state.items;
    }

    public void refreshItems() {
        ShoppingState old = shoppingState.getValue();
        shoppingState.setValue(new ShoppingState(old == null ? Collections.emptyList() : old.items, true, null));
        ioExecutor.execute(() -> {
            try {
                shoppingState.postValue(new ShoppingState(repository.getShoppingItems(), false, null));
            } catch (RuntimeException error) {
                ShoppingState current = shoppingState.getValue();
                shoppingState.postValue(new ShoppingState(current == null ? Collections.emptyList() : current.items,
                        false, error.getMessage()));
            }
        });
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
                || unit == null || unit.trim().isEmpty() || !ShoppingCategoryCatalog.contains(category)) {
            saveResult.setValue(new SaveResult(false, null, "Vui lòng nhập tên, số lượng hợp lệ và đơn vị."));
            return false;
        }
        try {
            BigDecimal numericAmount = new BigDecimal(amount.trim().replace(',', '.'));
            if (numericAmount.signum() <= 0) {
                saveResult.setValue(new SaveResult(false, null, "Số lượng phải lớn hơn 0."));
                return false;
            }
            BigDecimal rounded = numericAmount.setScale(2, RoundingMode.HALF_UP);
            if (rounded.signum() <= 0) {
                saveResult.setValue(new SaveResult(false, null, "Số lượng phải lớn hơn 0."));
                return false;
            }
            amount = rounded.stripTrailingZeros().toPlainString();
        } catch (NumberFormatException qualitativeAmount) {
            // Text quantities such as “vừa đủ” stay qualitative and are never arithmetically combined.
        }
        ShoppingListItem current = id == null ? null : findItem(id);
        String resolvedStatus = status;
        if (current != null) resolvedStatus = current.status;
        if (resolvedStatus != null && !isValidStatus(resolvedStatus)) return false;
        saveResult.setValue(null);
        String finalAmount = amount;
        String finalStatus = resolvedStatus;
        ioExecutor.execute(() -> {
            try {
                String statusToSave = finalStatus;
                if (statusToSave == null) {
                    statusToSave = initialStatusForPantry(null, name, finalAmount, unit,
                            repository.getPantryItems(), System.currentTimeMillis());
                }
                repository.saveShoppingItem(id, name, finalAmount, unit, category, statusToSave);
                shoppingState.postValue(new ShoppingState(repository.getShoppingItems(), false, null));
                saveResult.postValue(new SaveResult(true, statusToSave, null));
            } catch (RuntimeException error) {
                saveResult.postValue(new SaveResult(false, null, error.getMessage()));
            }
        });
        return true;
    }

    public void setStatus(String id, String status) {
        if (id == null || !isValidStatus(status)) return;
        List<ShoppingListItem> previousItems = new ArrayList<>(getItems());
        updateItemsOptimistically(id, status, false);
        ioExecutor.execute(() -> {
            try {
                repository.setShoppingItemStatus(id, status);
                shoppingState.postValue(new ShoppingState(repository.getShoppingItems(), false, null));
            } catch (RuntimeException error) {
                shoppingState.postValue(new ShoppingState(previousItems, false, error.getMessage()));
            }
        });
    }

    public void deleteItem(String id) {
        if (id == null) return;
        List<ShoppingListItem> previousItems = new ArrayList<>(getItems());
        updateItemsOptimistically(id, null, true);
        ioExecutor.execute(() -> {
            try {
                repository.deleteShoppingItem(id);
                shoppingState.postValue(new ShoppingState(repository.getShoppingItems(), false, null));
            } catch (RuntimeException error) {
                shoppingState.postValue(new ShoppingState(previousItems, false, error.getMessage()));
            }
        });
    }

    public void clearItems() {
        List<ShoppingListItem> previousItems = new ArrayList<>(getItems());
        shoppingState.setValue(new ShoppingState(Collections.emptyList(), false, null));
        ioExecutor.execute(() -> {
            try {
                repository.clearShoppingItems();
                shoppingState.postValue(new ShoppingState(repository.getShoppingItems(), false, null));
            } catch (RuntimeException error) {
                shoppingState.postValue(new ShoppingState(previousItems, false, error.getMessage()));
            }
        });
    }

    private PlanUpdate checkForPlanChanges() {
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

    public void checkForPlanChangesAsync() {
        planUpdate.setValue(null);
        ioExecutor.execute(() -> {
            try {
                PlanUpdate update = checkForPlanChanges();
                planUpdate.postValue(update);
            } catch (RuntimeException error) {
                planUpdate.postValue(new PlanUpdate("", Collections.emptyList(), error.getMessage()));
            }
        });
    }

    public void acceptPlanUpdate(PlanUpdate update) {
        if (update != null) ioExecutor.execute(() -> repository.saveShoppingPlanSnapshot(update.currentSnapshot));
    }

    public void clearPlanUpdate() { planUpdate.setValue(null); }
    public void clearSaveResult() { saveResult.setValue(null); }

    private void updateItemsOptimistically(String id, String status, boolean remove) {
        ShoppingState current = shoppingState.getValue();
        if (current == null) return;
        List<ShoppingListItem> updated = new ArrayList<>();
        for (ShoppingListItem item : current.items) {
            if (item.id.equals(id)) {
                if (!remove) updated.add(new ShoppingListItem(item.id, item.name, item.amount, item.unit,
                        status, item.category));
            } else updated.add(item);
        }
        shoppingState.setValue(new ShoppingState(updated, false, null));
    }

    public static final class ShoppingState {
        public final List<ShoppingListItem> items;
        public final boolean loading;
        public final String error;

        ShoppingState(List<ShoppingListItem> items, boolean loading, String error) {
            this.items = Collections.unmodifiableList(new ArrayList<>(items));
            this.loading = loading;
            this.error = error;
        }
    }

    public static final class SaveResult {
        public final boolean success;
        public final String status;
        public final String error;

        SaveResult(boolean success, String status, String error) {
            this.success = success;
            this.status = status;
            this.error = error;
        }
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
