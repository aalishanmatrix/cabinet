package com.afollestad.cabinet.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import com.afollestad.cabinet.R;

import java.io.File;
import java.io.IOException;

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
        String deleteCmd = "rm -r " + fileOrDirectory.getAbsolutePath();
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec(deleteCmd);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}