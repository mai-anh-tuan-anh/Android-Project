package com.example.myapplication.di;

import com.example.myapplication.data.repository.LocalPersonalDataRepository;
import com.example.myapplication.domain.repository.PersonalDataRepository;
import com.example.myapplication.domain.repository.SearchHistoryRepository;
import com.example.myapplication.data.local.storage.SharedPreferencesSearchHistoryRepository;
import android.app.Application;


/** Application-scoped dependencies shared by all feature ViewModels. */
public final class AppContainer {
    public final PersonalDataRepository personalDataRepository;
    public final SearchHistoryRepository searchHistoryRepository;

    public AppContainer(Application application) {
        personalDataRepository = new LocalPersonalDataRepository(application);
        searchHistoryRepository = new SharedPreferencesSearchHistoryRepository(application);
    }
}
