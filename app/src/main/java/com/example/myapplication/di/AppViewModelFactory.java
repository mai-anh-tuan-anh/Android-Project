package com.example.myapplication.di;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.BepNhaApplication;
import com.example.myapplication.presentation.cookingjournal.CookingJournalViewModel;
import com.example.myapplication.presentation.discover.DiscoverSearchViewModel;
import com.example.myapplication.presentation.discover.DiscoverViewModel;
import com.example.myapplication.presentation.mealplan.MealPlanViewModel;
import com.example.myapplication.presentation.pantry.PantryViewModel;
import com.example.myapplication.presentation.personaldata.PersonalDataViewModel;
import com.example.myapplication.presentation.shopping.ShoppingViewModel;

/** Supplies feature ViewModels with application-scoped repository contracts. */
public final class AppViewModelFactory implements ViewModelProvider.Factory {
    private final AppContainer appContainer;

    public AppViewModelFactory(BepNhaApplication application) {
        this.appContainer = application.getAppContainer();
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass == PersonalDataViewModel.class) {
            return (T) new PersonalDataViewModel(appContainer.personalDataRepository);
        }
        if (modelClass == MealPlanViewModel.class) {
            return (T) new MealPlanViewModel(appContainer.personalDataRepository, appContainer.ioExecutor);
        }
        if (modelClass == ShoppingViewModel.class) {
            return (T) new ShoppingViewModel(appContainer.personalDataRepository, appContainer.ioExecutor);
        }
        if (modelClass == PantryViewModel.class) {
            return (T) new PantryViewModel(appContainer.personalDataRepository, appContainer.ioExecutor);
        }
        if (modelClass == CookingJournalViewModel.class) {
            return (T) new CookingJournalViewModel(appContainer.personalDataRepository);
        }
        if (modelClass == DiscoverSearchViewModel.class) {
            return (T) new DiscoverSearchViewModel(appContainer.searchHistoryRepository);
        }
        if (modelClass == DiscoverViewModel.class) {
            return (T) new DiscoverViewModel();
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
