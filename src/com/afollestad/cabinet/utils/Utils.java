package com.afollestad.cabinet.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import com.afollestad.cabinet.R;

import java.io.File;

/**
 * @author Aidan Follestad (afollestad)
 */
public class Utils {

    public static ProgressDialog showProgressDialog(Activity activity, int max) {
        ProgressDialog progress = new ProgressDialog(activity);
        progress.setMax(max);
        progress.setMessage(activity.getString(R.string.please_wait));
        progress.show();
        return progress;
    }

    public static boolean deleteRecursively(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursively(child);
        return fileOrDirectory.delete();
    }
}