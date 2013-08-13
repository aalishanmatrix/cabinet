package com.afollestad.cabinet.utils;

import android.app.ProgressDialog;
import android.util.Log;
import com.afollestad.cabinet.cab.DirectoryCAB;
import com.afollestad.cabinet.file.File;
import com.afollestad.cabinet.fragments.DirectoryFragment;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds a list of files being copied or cut, and handles copying or moving them to another directory.
 *
 * @author Aidan Follestad (afollestad)
 */
public class Clipboard {

    public Clipboard() {
        mClipboard = new ArrayList<File>();
    }

    private final List<File> mClipboard;
    private Type mClipboardType = Type.NONE;

    private void log(String message) {
        Log.d("Clipboard", message);
    }

    public static enum Type {
        COPY, CUT, NONE
    }

    public Clipboard add(File clip) {
        log("Adding " + clip.getAbsolutePath() + " to clipboard...");
        mClipboard.add(clip);
        return this;
    }

    public Clipboard setType(Type type) {
        log("Clipboard type set to " + type.toString());
        mClipboardType = type;
        return this;
    }

    public List<File> get() {
        return mClipboard;
    }

    public Type getType() {
        return mClipboardType;
    }

    public boolean canPaste() {
        return mClipboard.size() > 0;
    }

    public void clear() {
        log("Clipboard cleared");
        mClipboard.clear();
        mClipboardType = Type.NONE;
    }

    public void performPaste(final DirectoryFragment fragment, final ProgressDialog dialog) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < mClipboard.size(); i++) {
                    final int index = i;
                    final File newFile = copy(mClipboard.get(i), fragment.getPath(), mClipboardType == Clipboard.Type.CUT);
                    fragment.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (newFile != null) fragment.getAdapter().update(newFile);
                            dialog.setProgress(index);
                        }
                    });
                }

                fragment.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Re-sort the Fragment's list
                        DirectoryCAB.resortFragmentList(fragment);
                        // Remove paste option from action bar
                        fragment.getActivity().invalidateOptionsMenu();
                        // Clear the clipboard
                        clear();
                        // Close the dialog
                        dialog.dismiss();
                    }
                });
            }
        });
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();
    }

    private File copy(File src, File dst, boolean cut) {
        log("Copying '" + src.getAbsolutePath() + "' to '" + dst.getAbsolutePath() + "'...");
        if (src.isDirectory()) {
            File newDir = Utils.checkForExistence(new File(dst, src.getName()), 0);
            log("Created: " + newDir.getAbsolutePath());
            newDir.mkdirs();
            // Recursively copy the source directory into the new directory
            for (File fi : src.listFiles())
                copy(fi, newDir, cut);
            if (cut) {
                log("Deleting: " + src.getAbsolutePath());
                src.delete();
            }
            return newDir;
        }

        // Copy this file into the destination directory
        try {
            dst = Utils.checkForExistence(new File(dst, src.getName()), 0);
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dst);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0)
                out.write(buf, 0, len);
            in.close();
            out.close();
            if (cut) {
                log("Deleting: " + src.getAbsolutePath());
                src.delete();
            }
            return dst;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
