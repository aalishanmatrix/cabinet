package com.afollestad.cabinet;

import android.app.Application;
import android.content.Context;

/**
 * @author Aidan Follestad (afollestad)
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static App get(Context context) {
        return (App) context.getApplicationContext();
    }
}
