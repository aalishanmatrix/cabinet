package com.afollestad.cabinet.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import com.afollestad.cabinet.App;
import com.afollestad.cabinet.R;
import com.afollestad.cabinet.file.CloudFile;
import com.afollestad.cabinet.file.File;
import com.afollestad.cabinet.file.LocalFile;
import com.afollestad.silk.views.text.SilkEditText;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpProgressMonitor;

import java.io.FileOutputStream;
import java.util.List;

/**
 * Various convenience methods.
 *
 * @author Aidan Follestad (afollestad)
 */
public class Utils {

    private static ProgressDialog mDialog;

    public static interface InputCallback {
        public void onSubmit(String input);
    }

    public static java.io.File getDownloadCacheFile(Context context, File forFile) {
        return new java.io.File(context.getExternalCacheDir(), forFile.getAbsolutePath().replace("/", "_"));
    }

    private static void finishOpen(final Activity context, final File item) {
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
                            Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(item.getFile()), type);
                            context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_with)));
                        }
                    }).show();
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(item.getFile()), type);
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_with)));
        }
    }

    public static void openFile(final Activity context, final File item) throws Exception {
        if (item.isRemoteFile()) {
            CloudFile remote = (CloudFile) item;
            ChannelSftp channel = App.get(context).getSftpChannel(remote);
            final java.io.File cacheFile = getDownloadCacheFile(context, item);
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDialog = new ProgressDialog(context);
                    mDialog.setMessage(context.getString(R.string.downloading));
                }
            });
            try {
                channel.get(remote.getAbsolutePath(), new FileOutputStream(cacheFile), new SftpProgressMonitor() {
                    @Override
                    public void init(int i, String s, String s2, long l) {
                        mDialog.setMax((int) l);
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mDialog.show();
                            }
                        });
                    }

                    @Override
                    public boolean count(final long l) {
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mDialog.setProgress((int) l);
                            }
                        });
                        if (!mDialog.isShowing()) {
                            cacheFile.delete();
                            return false;
                        }
                        return true;
                    }

                    @Override
                    public void end() {
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mDialog.dismiss();
                                finishOpen(context, new LocalFile(cacheFile));
                            }
                        });
                    }
                });
            } catch (Exception e) {
                if (!mDialog.isShowing()) return;
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDialog.dismiss();
                    }
                });
                throw e;
            }
        } else {
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    finishOpen(context, item);
                }
            });
        }
    }

    public static File checkForExistence(Context context, File file, int index) {
        String newName = file.getNameNoExtension();
        String extension = file.getExtension();
        if (!extension.trim().isEmpty()) extension = "." + extension;
        if (index > 0) newName += " (" + index + ")";
        File newFile = file.isRemoteFile() ? new CloudFile(context, (CloudFile) file.getParentFile(), newName + extension) :
                new LocalFile((LocalFile) file.getParentFile(), newName + extension);
        try {
            if (newFile.exists()) {
                return checkForExistence(context, file, ++index);
            } else return newFile;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int getTotalFileCount(File root) throws Exception {
        if (root.isDirectory()) return 1;
        int count = 1;
        File[] files = root.listFilesUnthreaded();
        if (files != null) {
            for (File fi : files)
                count += getTotalFileCount(fi);
        }
        return count;
    }

    public static int getTotalFileCount(List<File> files) throws Exception {
        int count = 0;
        for (File fi : files) {
            if (fi.requiresRootAccess() || fi.isRemoteFile()) count++;
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

    public static void showErrorDialog(Activity context, Exception error) {
        new AlertDialog.Builder(context).setTitle(R.string.error)
                .setMessage(error.getMessage())
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
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

    public static boolean deleteRecursively(File file) throws Exception {
        boolean retVal = true;
        if (file.isDirectory()) {
            File[] files = file.listFilesUnthreaded();
            if (files != null) {
                for (File f : files)
                    retVal = retVal && deleteRecursively(f);
            }
            retVal = retVal && file.deleteNonRecursive();
        } else retVal = file.deleteNonRecursive();
        return retVal;
    }
}