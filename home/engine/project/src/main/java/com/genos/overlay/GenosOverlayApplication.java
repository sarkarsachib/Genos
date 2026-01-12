package com.genos.overlay;

import android.app.Application;
import android.content.Context;

/**
 * Application class for GENOS Overlay system
 */
public class GenosOverlayApplication extends Application {
    private static GenosOverlayApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static GenosOverlayApplication getInstance() {
        return instance;
    }

    public static Context getContext() {
        return instance.getApplicationContext();
    }
}