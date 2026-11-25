package com.example.myapplication;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.example.myapplication.di.AppContainer;

/** Creates one dependency graph for the lifetime of the application process. */
public class BepNhaApplication extends Application {
    private AppContainer appContainer;
    private volatile int startedActivities;

    @Override
    public void onCreate() {
        super.onCreate();
        appContainer = createAppContainer();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityStarted(Activity activity) {
                if (++startedActivities == 1) onAppForeground();
            }

            @Override public void onActivityStopped(Activity activity) { --startedActivities; }
            @Override public void onActivityCreated(Activity activity, Bundle state) {}
            @Override public void onActivityResumed(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }

    protected AppContainer createAppContainer() { return new AppContainer(this); }

    protected void onAppForeground() {
        appContainer.catalogSync.onForeground(this::isInForeground);
    }

    protected final boolean isInForeground() { return startedActivities > 0; }

    public AppContainer getAppContainer() {
        return appContainer;
    }
}
