package com.afollestad.cabinet.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import com.afollestad.cabinet.File;
import com.afollestad.cabinet.R;
import com.afollestad.silk.views.text.SilkEditText;

/**
 * Various convenience methods.
 *
 * @author Aidan Follestad (afollestad)
 */
public class Utils {

    public static interface InputCallback {
        public void onSubmit(String input);
    }

    public static File checkForExistence(File file, int index) {
        Log.d("checkForExistence", file.getAbsolutePath());
        String newName = file.getNameNoExtension();
        String extension = file.getExtension();
        if (!extension.trim().isEmpty()) extension = "." + extension;
        if (index > 0) newName += " (" + index + ")";
        Log.d("checkForExistence", "Name: " + newName);
        Log.d("checkForExistence", "Extension: " + extension);
        file = new File(file.getParentFile(), newName + extension);
        if (file.exists()) {
            return checkForExistence(file, ++index);
        } else {
            return file;
        }
    }

    public static ProgressDialog showProgressDialog(Activity activity, int title, int max) {
        ProgressDialog progress = new ProgressDialog(activity);
        if (title > 0) progress.setTitle(title);
        progress.setIndeterminate(max == -1);
        if (max > 0) {
            progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progress.setMax(max);
        }
        progress.setCancelable(false);
        progress.setMessage(activity.getString(R.string.please_wait));
        progress.show();
        return progress;
    }

    public static void showInputDialog(Activity activity, int title, int hint, String prefillInput, final InputCallback callback) {
        View view = activity.getLayoutInflater().inflate(R.layout.input_edit_text, null);
        final SilkEditText input = (SilkEditText) view.findViewById(R.id.input);
        if (hint > 0) input.setHint(hint);
        if (prefillInput != null) input.append(prefillInput);
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setView(view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                        callback.onSubmit(input.getText().toString().trim());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                }).show();
    }

    public static boolean deleteRecursively(File file) {
        boolean retVal = true;
        if (file.isDirectory()) {
            for (java.io.File f : file.listFiles())
                retVal = retVal && deleteRecursively(new File(f));
            retVal = retVal && file.deleteNonRecursive();
        } else retVal = file.deleteNonRecursive();
        return retVal;
    }
}