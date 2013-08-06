package com.afollestad.cabinet.utils;

import android.app.ProgressDialog;
import android.util.Log;
import com.afollestad.cabinet.File;
import com.afollestad.cabinet.cab.DirectoryCAB;
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

    public boolean canPaste(File dest) {
        if (mClipboard.size() == 0) return false;
        String parent = mClipboard.get(0).getParentFile().getAbsolutePath();
        return !dest.getAbsolutePath().equals(parent);
    }

    public void clear() {
        log("Clipboard cleared");
        mClipboard.clear();
        mClipboardType = Type.NONE;
    }

    public void performPaste(final DirectoryFragment fragment, final ProgressDialog dialog) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < mClipboard.size(); i++) {
                    final File fi = mClipboard.get(i);
                    final int index = i;
                    final boolean success = copy(fi, fragment.getPath(), mClipboardType == Clipboard.Type.CUT);
                    fragment.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (success) fragment.getAdapter().update(fi);
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
        }).start();
    }

    private boolean copy(File src, File dst, boolean cut) {
        log("Copying '" + src.getAbsolutePath() + "' to '" + dst.getAbsolutePath() + "'...");
        if (src.isDirectory()) {
            File newDir = new File(dst, src.getName());
            log("Created: " + newDir.getAbsolutePath());
            boolean success = newDir.mkdirs();
            // Recursively copy the source directory into the new directory
            for (File fi : src.listFiles())
                success = success && copy(fi, newDir, cut);
            if (cut) {
                log("Deleting: " + src.getAbsolutePath());
                src.delete();
            }
            return success;
        }

        // Copy this file into the destination directory
        try {
            dst = new File(dst, src.getName());
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
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
