package com.example.myapplication.testing;

import android.app.Application;
import android.content.Context;

import androidx.test.runner.AndroidJUnitRunner;

/** Keeps the normal instrumentation suite independent of Firebase. */
public final class OfflineTestRunner extends AndroidJUnitRunner {
    @Override
    public Application newApplication(ClassLoader loader, String className, Context context)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return super.newApplication(loader, OfflineTestApplication.class.getName(), context);
    }
}
