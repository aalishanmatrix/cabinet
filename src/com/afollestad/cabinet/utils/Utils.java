package com.afollestad.cabinet.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import com.afollestad.cabinet.R;
import com.afollestad.cabinet.file.File;
import com.afollestad.silk.views.text.SilkEditText;

import java.util.List;

/**
 * Various convenience methods.
 *
 * @author Aidan Follestad (afollestad)
 */
public class Utils {

    public static interface InputCallback {
        public void onSubmit(String input);
    }

    public static void openFile(final Activity context, final File item) {
        String type = item.getMimeType();
        if (type == null || type.trim().isEmpty()) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.open_as)
                    .setItems(R.array.open_as_options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String type;
                            switch (which) {
                                default:
                                    type = "text/*";
                                    break;
                                case 1:
                                    type = "audio/*";
                                    break;
                                case 2:
                                    type = "image/*";
                                    break;
                                case 3:
                                    type = "video/*";
                                    break;
                                case 4:
                                    type = "*/*";
                                    break;
                            }
                            Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(item), type);
                            context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_with)));
                        }
                    }).show();
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(item), type);
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_with)));
        }
    }

    public static File checkForExistence(File file, int index) {
        String newName = file.getNameNoExtension();
        String extension = file.getExtension();
        if (!extension.trim().isEmpty()) extension = "." + extension;
        if (index > 0) newName += " (" + index + ")";
        File newFile = new File(file.getParentFile(), newName + extension);
        if (newFile.exists()) {
            return checkForExistence(file, ++index);
        } else {
            return newFile;
        }
    }

    public static int getTotalFileCount(File root) {
        int count = 1;
        File[] files = root.listFiles();
        if (files != null) {
            for (File fi : files)
                count += getTotalFileCount(fi);
        }
        return count;
    }

    public static int getTotalFileCount(List<File> files) {
        int count = 0;
        for (File fi : files) {
            if (fi.requiresRootAccess()) count++;
            else count += getTotalFileCount(fi);
        }
        return count;
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