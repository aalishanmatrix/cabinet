package com.afollestad.cabinet;

import android.app.Application;
import android.content.Context;
import com.afollestad.silk.images.SilkImageManager;

/**
 * @author Aidan Follestad (afollestad)
 */
public class App extends Application {

    private SilkImageManager mDrawableManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mDrawableManager = new SilkImageManager();
    }

    public static App get(Context context) {
        return (App) context.getApplicationContext();
    }

    public SilkImageManager getDrawableManager() {
        return mDrawableManager;
    }
}
