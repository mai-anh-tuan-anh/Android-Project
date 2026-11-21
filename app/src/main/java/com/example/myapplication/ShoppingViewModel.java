package com.example.myapplication;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Screen state and validation for module 7's current local shopping list. */
public final class ShoppingViewModel extends AndroidViewModel {
    static final String NEED_TO_BUY = "NEED_TO_BUY";
    static final String AVAILABLE = "AVAILABLE";
    static final String PURCHASED = "PURCHASED";

    private final PersonalDataRepository repository;

    public ShoppingViewModel(@NonNull Application application) {
        super(application);
        repository = PersonalDataRepository.getInstance(application);
    }

    List<PersonalDataDbHelper.ShoppingItem> getItems() { return repository.getShoppingItems(); }

    PersonalDataDbHelper.ShoppingItem findItem(String id) {
        if (id == null) return null;
        for (PersonalDataDbHelper.ShoppingItem item : getItems()) {
            if (item.id.equals(id)) return item;
        }
        return null;
    }

    boolean saveItem(String id, String name, String amount, String unit, String status) {
        if (name == null || name.trim().isEmpty() || amount == null || amount.trim().isEmpty()
                || unit == null || unit.trim().isEmpty()) return false;
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
        repository.saveShoppingItem(id, name, amount, unit, status);
        return true;
    }

    void setStatus(String id, String status) {
        if (id != null && isValidStatus(status)) repository.setShoppingItemStatus(id, status);
    }

    void deleteItem(String id) {
        if (id != null) repository.deleteShoppingItem(id);
    }

    String nextStatus(String currentStatus) {
        if (NEED_TO_BUY.equals(currentStatus)) return AVAILABLE;
        if (AVAILABLE.equals(currentStatus)) return PURCHASED;
        return NEED_TO_BUY;
    }

    private boolean isValidStatus(String status) {
        return NEED_TO_BUY.equals(status) || AVAILABLE.equals(status) || PURCHASED.equals(status);
    }
}
