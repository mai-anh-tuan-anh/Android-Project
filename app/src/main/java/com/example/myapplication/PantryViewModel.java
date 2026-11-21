package com.example.myapplication;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Local pantry state and actions. */
public final class PantryViewModel extends AndroidViewModel {
    private static final int EXPIRING_WITHIN_DAYS = 3;
    private final PersonalDataRepository repository;

    public PantryViewModel(@NonNull Application application) {
        super(application);
        repository = PersonalDataRepository.getInstance(application);
    }

    List<PersonalDataDbHelper.PantryItem> getItems() { return repository.getPantryItems(); }
    PersonalDataDbHelper.PantryItem getItem(String id) { return repository.getPantryItem(id); }
    boolean saveItem(String id, String name, String amount, String unit, String category,
                     String expiryDate, String storageLocation, String purchaseDate) {
        return repository.savePantryItem(id, name, amount, unit, category, expiryDate, storageLocation, purchaseDate);
    }
    boolean deleteItem(String id) { return repository.deletePantryItem(id); }
    int deleteAllItems() { return repository.deleteAllPantryItems(); }

    String expiryState(String isoDate) {
        return expiryStateForDate(isoDate, System.currentTimeMillis());
    }

    static String expiryStateForDate(String isoDate, long nowMillis) {
        if (isoDate == null || isoDate.isEmpty()) return "";
        String today = isoDate(nowMillis);
        if (isoDate.compareTo(today) < 0) return "EXPIRED";
        Calendar threshold = Calendar.getInstance();
        threshold.setTimeInMillis(nowMillis);
        threshold.add(Calendar.DAY_OF_YEAR, EXPIRING_WITHIN_DAYS);
        return isoDate.compareTo(isoDate(threshold.getTimeInMillis())) <= 0 ? "EXPIRING" : "";
    }

    String availabilityLabel(String expiryDate) {
        String state = expiryState(expiryDate);
        if ("EXPIRED".equals(state)) return "Hết hạn";
        if ("EXPIRING".equals(state)) return "Sắp hết hạn";
        return "Sử dụng được";
    }

    private static String isoDate(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date(millis));
    }
}
