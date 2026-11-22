package com.example.myapplication.presentation.pantry;



import androidx.lifecycle.ViewModel;
import com.example.myapplication.domain.model.PantryIngredient;
import com.example.myapplication.domain.repository.PersonalDataRepository;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Local pantry state and actions. */
public final class PantryViewModel extends ViewModel {
    private static final int EXPIRING_WITHIN_DAYS = 3;
    private final PersonalDataRepository repository;

    public PantryViewModel(PersonalDataRepository repository) {
        this.repository = repository;
    }

    public List<PantryIngredient> getItems() { return repository.getPantryItems(); }
    public PantryIngredient getItem(String id) { return repository.getPantryItem(id); }
    public boolean saveItem(String id, String name, String amount, String unit, String category,
                     String expiryDate, String storageLocation, String purchaseDate) {
        return repository.savePantryItem(id, name, amount, unit, category, expiryDate, storageLocation, purchaseDate);
    }
    public boolean deleteItem(String id) { return repository.deletePantryItem(id); }
    public int deleteAllItems() { return repository.deleteAllPantryItems(); }

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
