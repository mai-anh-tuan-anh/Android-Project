package com.example.myapplication.presentation.pantry;



import androidx.lifecycle.ViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.myapplication.domain.model.PantryIngredient;
import com.example.myapplication.domain.repository.PersonalDataRepository;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executor;

/** Local pantry state and actions. */
public final class PantryViewModel extends ViewModel {
    private static final int EXPIRING_WITHIN_DAYS = 3;
    private final PersonalDataRepository repository;
    private final Executor ioExecutor;
    private final MutableLiveData<PantryState> pantryState = new MutableLiveData<>(
            new PantryState(Collections.emptyList(), true, null));
    private final MutableLiveData<MutationResult> mutationResult = new MutableLiveData<>();

    public PantryViewModel(PersonalDataRepository repository, Executor ioExecutor) {
        this.repository = repository;
        this.ioExecutor = ioExecutor;
    }

    public LiveData<PantryState> pantryState() { return pantryState; }
    public LiveData<MutationResult> mutationResult() { return mutationResult; }

    public List<PantryIngredient> getItems() {
        PantryState state = pantryState.getValue();
        return state == null ? Collections.emptyList() : state.items;
    }

    public PantryIngredient getItem(String id) {
        if (id == null) return null;
        for (PantryIngredient item : getItems()) if (id.equals(item.id)) return item;
        return null;
    }

    public void refreshItems() {
        PantryState current = pantryState.getValue();
        pantryState.setValue(new PantryState(current == null ? Collections.emptyList() : current.items, true, null));
        ioExecutor.execute(() -> loadItems(null));
    }

    public boolean saveItem(String id, String name, String amount, String unit, String category,
                     String expiryDate, String storageLocation, String purchaseDate) {
        if (name == null || name.trim().isEmpty() || category == null || category.trim().isEmpty()) return false;
        if (amount != null && !amount.trim().isEmpty()) {
            try {
                if (new java.math.BigDecimal(amount.trim().replace(',', '.')).signum() < 0
                        || unit == null || unit.trim().isEmpty()) return false;
            } catch (NumberFormatException invalidAmount) { return false; }
        }
        ioExecutor.execute(() -> {
            try {
                boolean saved = repository.savePantryItem(id, name, amount, unit, category,
                        expiryDate, storageLocation, purchaseDate);
                loadItems(new MutationResult(MutationResult.SAVE, saved, 0,
                        saved ? null : "Vui lòng kiểm tra tên, số lượng, đơn vị và ngày."));
            } catch (RuntimeException error) { postMutationError(MutationResult.SAVE, error); }
        });
        return true;
    }

    public void deleteItem(String id) {
        if (id == null) return;
        List<PantryIngredient> previous = new ArrayList<>(getItems());
        List<PantryIngredient> updated = new ArrayList<>(previous);
        updated.removeIf(item -> id.equals(item.id));
        pantryState.setValue(new PantryState(updated, false, null));
        ioExecutor.execute(() -> {
            try {
                boolean deleted = repository.deletePantryItem(id);
                if (!deleted) pantryState.postValue(new PantryState(previous, false, "Không tìm thấy nguyên liệu trong tủ."));
                loadItems(new MutationResult(MutationResult.DELETE, deleted, 0,
                        deleted ? null : "Không thể xóa nguyên liệu."));
            } catch (RuntimeException error) {
                pantryState.postValue(new PantryState(previous, false, error.getMessage()));
                postMutationError(MutationResult.DELETE, error);
            }
        });
    }

    public void deleteAllItems() {
        List<PantryIngredient> previous = new ArrayList<>(getItems());
        pantryState.setValue(new PantryState(Collections.emptyList(), false, null));
        ioExecutor.execute(() -> {
            try {
                int count = repository.deleteAllPantryItems();
                loadItems(new MutationResult(MutationResult.CLEAR, true, count, null));
            } catch (RuntimeException error) {
                pantryState.postValue(new PantryState(previous, false, error.getMessage()));
                postMutationError(MutationResult.CLEAR, error);
            }
        });
    }

    private void loadItems(MutationResult result) {
        try {
            pantryState.postValue(new PantryState(repository.getPantryItems(), false, null));
            if (result != null) mutationResult.postValue(result);
        } catch (RuntimeException error) {
            postMutationError(result == null ? MutationResult.LOAD : result.operation, error);
        }
    }

    private void postMutationError(String operation, RuntimeException error) {
        mutationResult.postValue(new MutationResult(operation, false, 0,
                error.getMessage() == null ? "Không thể lưu dữ liệu tủ nguyên liệu." : error.getMessage()));
    }

    public void clearMutationResult() { mutationResult.setValue(null); }

    public static final class PantryState {
        public final List<PantryIngredient> items;
        public final boolean loading;
        public final String error;
        PantryState(List<PantryIngredient> items, boolean loading, String error) {
            this.items = Collections.unmodifiableList(new ArrayList<>(items));
            this.loading = loading;
            this.error = error;
        }
    }

    public static final class MutationResult {
        public static final String LOAD = "LOAD", SAVE = "SAVE", DELETE = "DELETE", CLEAR = "CLEAR";
        public final String operation;
        public final boolean success;
        public final int affectedCount;
        public final String error;
        MutationResult(String operation, boolean success, int affectedCount, String error) {
            this.operation = operation;
            this.success = success;
            this.affectedCount = affectedCount;
            this.error = error;
        }
    }

    public String expiryState(String isoDate) {
        return expiryStateForDate(isoDate, System.currentTimeMillis());
    }

    public static String expiryStateForDate(String isoDate, long nowMillis) {
        if (isoDate == null || isoDate.isEmpty()) return "";
        String today = isoDate(nowMillis);
        if (isoDate.compareTo(today) < 0) return "EXPIRED";
        Calendar threshold = Calendar.getInstance();
        threshold.setTimeInMillis(nowMillis);
        threshold.add(Calendar.DAY_OF_YEAR, EXPIRING_WITHIN_DAYS);
        return isoDate.compareTo(isoDate(threshold.getTimeInMillis())) <= 0 ? "EXPIRING" : "";
    }

    public String availabilityLabel(String expiryDate) {
        String state = expiryState(expiryDate);
        if ("EXPIRED".equals(state)) return "Hết hạn";
        if ("EXPIRING".equals(state)) return "Sắp hết hạn";
        return "Sử dụng được";
    }

    private static String isoDate(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date(millis));
    }
}
