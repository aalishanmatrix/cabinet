package com.afollestad.cabinet;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import com.afollestad.cabinet.utils.Clipboard;

/**
 * Contains variables that are kept in memory throughout the life cycle of the application, even as activities open and close.
 *
 * @author Aidan Follestad (afollestad)
 */
public class App extends Application {

    private Clipboard mClipboard;

    public static App get(Context context) {
        return (App) context.getApplicationContext();
    }

    public Clipboard getClipboard() {
        if (mClipboard == null)
            mClipboard = new Clipboard();
        return mClipboard;
    }

    public static File getStorageDirectory() {
        File sdTest = new File("/sdcard");
        if (!sdTest.exists()) sdTest = new File(Environment.getExternalStorageDirectory());
        return sdTest;
    }
}
