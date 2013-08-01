package com.afollestad.cabinet.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.view.View;
import com.afollestad.cabinet.R;
import com.afollestad.silk.views.text.SilkEditText;

import java.io.File;
import java.io.IOException;

/**
 * @author Aidan Follestad (afollestad)
 */
public class Utils {

    public static interface InputCallback {
        public void onSubmit(String input);
    }


    public static ProgressDialog showProgressDialog(Activity activity, int max) {
        ProgressDialog progress = new ProgressDialog(activity);
        progress.setMax(max);
        progress.setMessage(activity.getString(R.string.please_wait));
        progress.show();
        return progress;
    }

    public static AlertDialog showInputDialog(Activity activity, int title, String prefillInput, final InputCallback callback) {
        View view = activity.getLayoutInflater().inflate(R.layout.input_edit_text, null);
        final SilkEditText input = (SilkEditText) view.findViewById(R.id.input);
        if (prefillInput != null) input.setText(prefillInput);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
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
                });
        AlertDialog dialog = builder.create();
        dialog.show();
        return dialog;
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