package com.example.myapplication.data.local.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.myapplication.domain.repository.SearchHistoryRepository;

/** SharedPreferences adapter for device-local recent search history. */
public final class SharedPreferencesSearchHistoryRepository implements SearchHistoryRepository {
    private static final String PREFS_NAME = "discover_search_prefs";
    private static final String KEY_RECENT_SEARCHES = "recent_searches";
    private final SharedPreferences preferences;

    public SharedPreferencesSearchHistoryRepository(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public String readSerializedSearchHistory() {
        return preferences.getString(KEY_RECENT_SEARCHES, null);
    }

    @Override
    public void writeSerializedSearchHistory(String value) {
        preferences.edit().putString(KEY_RECENT_SEARCHES, value).apply();
    }
}
