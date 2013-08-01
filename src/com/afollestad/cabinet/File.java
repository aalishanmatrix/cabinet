package com.afollestad.cabinet;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;
import com.afollestad.cabinet.utils.Utils;
import com.afollestad.silk.cache.SilkComparable;

import java.net.URI;

public class File extends java.io.File implements SilkComparable<File> {

    public File(java.io.File dir, String name) {
        super(dir, name);
    }

    public File(String path) {
        super(path);
    }

    public File(String dirPath, String name) {
        super(dirPath, name);
    }

    public File(URI uri) {
        super(uri);
    }

    public File(java.io.File file) {
        super(file.getAbsolutePath());
    }

    @Override
    public boolean isSameAs(File another) {
        return getAbsolutePath().equals(another.getAbsolutePath());
    }

    @Override
    public boolean shouldIgnore() {
        return isHidden();
    }

    public String getSizeString(Context context) {
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

    public String getExtension() {
        String name = getName().toLowerCase();
        if (!name.contains(".")) return null;
        return name.substring(name.lastIndexOf('.') + 1);
    }

    public String getMimeType() {
        String type = null;
        String extension = getExtension();
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        return type;
    }

    /**
     * Overridden to recursively delete directories
     */
    @Override
    public boolean delete() {
        return Utils.deleteRecursively(this);
    }

    /**
     * Checks whether or not this file represents the SD card directory
     */
    public boolean isStorageDirectory() {
        return getAbsolutePath().equals(App.getStorageDirectory().getAbsolutePath());
    }

    public static class Comparator implements java.util.Comparator<java.io.File> {
        @Override
        public int compare(java.io.File lhs, java.io.File rhs) {
            return lhs.getName().compareTo(rhs.getName());
        }
    }
}
