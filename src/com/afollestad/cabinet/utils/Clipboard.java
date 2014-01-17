package com.afollestad.cabinet.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;
import android.widget.Toast;
import com.afollestad.cabinet.App;
import com.afollestad.cabinet.cab.DirectoryCAB;
import com.afollestad.cabinet.file.File;
import com.afollestad.cabinet.file.RemoteFile;
import com.afollestad.cabinet.fragments.DirectoryFragment;
import com.jcraft.jsch.ChannelSftp;

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

    private void remove(File fi) {
        for (int i = 0; i < mClipboard.size(); i++) {
            if (!mClipboard.get(i).getAbsolutePath().equals(fi.getAbsolutePath())) {
                mClipboard.remove(i);
                break;
            }
        }
    }

    public boolean canPaste(File dest) throws Exception {
        if (mClipboard.size() == 0) return false;
        // Remove no longer existing files from the clipboard and check for paradoxes
        for (File fi : mClipboard) {
            if (!fi.isRemote() && !fi.exists()) remove(fi);
            else if (dest.getAbsolutePath().equals(fi.getAbsolutePath())) {
                // You cannot copy/cut a directory into itself
                return false;
            }
        }
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
                    try {
                        final File newFile = copy(fragment.getActivity(), mClipboard.get(i), fragment.getPath(), mClipboardType == Clipboard.Type.CUT);
                        fragment.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (newFile != null)
                                    fragment.getAdapter().update(newFile);
                                dialog.setProgress(index);
                            }
                        });
                    } catch (final Exception e) {
                        e.printStackTrace();
                        fragment.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Utils.showErrorDialog(fragment.getActivity(), e);
                            }
                        });
                    }
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

    private Toast toast;

    private File copy(final Activity context, File src, File dst, boolean cut) throws Exception {
        log("Copying '" + src.toString() + "' (" + src.isRemote() + ") to '" + dst.toString() + "' (" + dst.isRemote() + ")...");
        if (src.isDirectory()) {
            dst = Utils.checkForExistence(context,
                    dst.isRemote() ? new RemoteFile(context, (RemoteFile) dst, src.getName()) : new File(dst, src.getName()), 0);
            try {
                dst.mkdir();
            } catch (final Exception e) {
                e.printStackTrace();
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Utils.showErrorDialog(context, e);
                    }
                });
                return null;
            }
            log("Created: " + dst.getAbsolutePath());
            // Recursively copy the source directory into the new directory
            for (File fi : src.listFiles())
                copy(context, fi, dst, cut);
            if (cut) {
                log("Deleting: " + src.getAbsolutePath());
                src.delete();
            }
            return dst;
        }

        // Copy this file into the destination directory
        try {
            dst = Utils.checkForExistence(context,
                    dst.isRemote() ? new RemoteFile(context, (RemoteFile) dst, src.getName()) : new File(dst, src.getName()), 0);
            InputStream in;
            OutputStream out;
            if (src.isRemote() || dst.isRemote()) {
                ChannelSftp channel = App.get(context).getSftpChannel(src.isRemote() ? (RemoteFile) src : (RemoteFile) dst);
                if (src.isRemote()) {
                    log("Opening remote stream to source file: " + src.getAbsolutePath());
                    in = channel.get(src.getAbsolutePath());
                } else {
                    log("Opening local stream to source file: " + src.getAbsolutePath());
                    in = new FileInputStream(src.getFile());
                }
                if (dst.isRemote()) {
                    log("Opening remote stream to destination file: " + dst.getAbsolutePath());
                    out = channel.put(dst.getAbsolutePath());
                } else {
                    log("Opening local stream to destination file: " + dst.getAbsolutePath());
                    out = new FileOutputStream(dst.getFile());
                }
            } else {
                log("Opening local streams to source and destination file: " + src.getAbsolutePath() + ", " + dst.getAbsolutePath());
                in = new FileInputStream(src.getFile());
                out = new FileOutputStream(dst.getFile());
            }
            byte[] buf = new byte[1024];
            long totalLen = 0;
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
                totalLen += len;
            }
            in.close();
            out.close();
            if (cut) {
                log("Deleting: " + src.getAbsolutePath());
                src.delete();
            }
            if (dst.isRemote()) {
                ((RemoteFile) dst).setDirectory(false);
                ((RemoteFile) dst).setSize(totalLen);
            }
            return dst;
        } catch (final Exception e) {
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (toast != null) toast.cancel();
                    toast = Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT);
                    toast.show();
                }
            });
            e.printStackTrace();
            return null;
        }
    }
}
