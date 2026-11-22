package com.example.myapplication.di;

import com.example.myapplication.data.repository.LocalPersonalDataRepository;
import com.example.myapplication.domain.repository.PersonalDataRepository;
import com.example.myapplication.domain.repository.SearchHistoryRepository;
import com.example.myapplication.data.local.storage.SharedPreferencesSearchHistoryRepository;
import android.app.Application;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/** Application-scoped dependencies shared by all feature ViewModels. */
public final class AppContainer {
    public final PersonalDataRepository personalDataRepository;
    public final SearchHistoryRepository searchHistoryRepository;
    public final ExecutorService ioExecutor;

    public AppContainer(Application application) {
        ioExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "bep-nha-data-io");
            thread.setDaemon(true);
            return thread;
        });
        personalDataRepository = new LocalPersonalDataRepository(application);
        searchHistoryRepository = new SharedPreferencesSearchHistoryRepository(application);
    }
}
