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

    public static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null && files.length > 0) {
            for (File fi : files) {
                if (fi.isDirectory()) {
                    // Recursively delete a directory inside the current directory
                    deleteDirectory(fi);
                } else {
                    // Delete a file inside the current directory
                    fi.delete();
                }
            }
        }
        // Finally, delete the original directory after deleting its files and directories
        directory.delete();
    }
}