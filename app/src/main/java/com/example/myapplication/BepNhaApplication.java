package com.example.myapplication;

import android.app.Application;

import com.example.myapplication.di.AppContainer;

/** Creates one dependency graph for the lifetime of the application process. */
public final class BepNhaApplication extends Application {
    private AppContainer appContainer;

    @Override
    public void onCreate() {
        super.onCreate();
        appContainer = new AppContainer(this);
    }

    public AppContainer getAppContainer() {
        return appContainer;
    }
}
