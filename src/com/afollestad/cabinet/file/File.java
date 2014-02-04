package com.afollestad.cabinet.file;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.MimeTypeMap;
import com.afollestad.cabinet.R;
import com.afollestad.cabinet.file.callbacks.FileListingCallback;
import com.afollestad.cabinet.file.comparators.*;
import com.afollestad.silk.caching.SilkComparable;
import eu.chainfire.libsuperuser.Shell;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * @author Aidan Follestad
 */
public abstract class File implements SilkComparable<File> {

    public abstract String getName();

    public abstract String getDisplayName();

    public abstract String getAbsolutePath();

    public abstract File getParentFile();

    public abstract java.io.File getFile();

    public abstract boolean exists() throws Exception;

    public abstract boolean mkdir() throws Exception;

    public abstract boolean mkdirs() throws Exception;

    public abstract boolean renameTo(File to) throws Exception;

    public abstract boolean deleteNonRecursive() throws Exception;

    public abstract boolean delete() throws Exception;

    public final void listFiles(final Activity context, final FileListingCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File[] files = listFilesUnthreaded();
                    if (files != null)
                        Arrays.sort(files, File.getComparator(context));
                    if (callback != null) {
                        final File[] fFiles = files;
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onResults(fFiles);
                            }
                        });
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                    if (callback != null) {
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(e);
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public abstract File[] listFilesUnthreaded() throws Exception;

    public abstract boolean isHidden();

    public abstract boolean isDirectory();

    public abstract long length();

    public abstract boolean isRemoteFile();

    public abstract boolean isStorageDirectory();

    public abstract boolean isRootDirectory();

    public abstract boolean requiresRootAccess();

    public abstract String getMountedAs();

    public final void mount() throws Exception {

    }

    public final void unmount() throws Exception {

    }

    public final String getNameNoExtension() {
        if (isDirectory()) return getName();
        String name = getName();
        if (name.startsWith(".") || !name.substring(1).contains(".")) return name;
        return name.substring(0, name.lastIndexOf('.'));
    }

    public final String getExtension() {
        if (isDirectory()) return "";
        String name = getName().toLowerCase();
        if (name.startsWith(".") || !name.substring(1).contains(".")) return "";
        return name.substring(name.lastIndexOf('.') + 1);
    }

    public final String getMimeType() {
        String type = null;
        String extension = getExtension();
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        return type;
    }

    public static Comparator<File> getComparator(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int sortSetting = prefs.getInt("sort_mode", 0);
        switch (sortSetting) {
            default:
                return new FoldersFirstComparator();
            case 1:
                return new AlphabeticalComparator();
            case 2:
                return new ExtensionComparator();
            case 3:
                return new LowHighSizeComparator();
            case 4:
                return new HighLowSizeComparator();
        }
    }

    protected final List<String> runAsRoot(String cmd) throws Exception {
        Log.d("RootFile", cmd);
        if (!Shell.SU.available())
            throw new Exception("Root access denied.");
        return Shell.SU.run(cmd);
    }

    public final String getSizeString(Context context) {
        if (isDirectory())
            return context.getString(R.string.directory);
        return humanReadableByteCount(length(), true);
    }

    private String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    @Override
    public Object getSilkId() {
        return getAbsolutePath();
    }

    @Override
    public boolean equalTo(File other) {
        return getAbsolutePath().equals(other.getAbsolutePath()) &&
                isRemoteFile() == other.isRemoteFile();
    }

    @Override
    public String toString() {
        return getAbsolutePath();
    }
}
