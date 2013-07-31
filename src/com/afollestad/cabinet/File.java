package com.afollestad.cabinet;

import android.content.Context;
import android.webkit.MimeTypeMap;
import com.afollestad.silk.cache.SilkComparable;

import java.net.URI;
import java.util.Locale;

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

    public String getMimeType() {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(getAbsolutePath().toLowerCase(Locale.getDefault()));
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        return type;
    }

    public static class Comparator implements java.util.Comparator<java.io.File> {
        @Override
        public int compare(java.io.File lhs, java.io.File rhs) {
            return lhs.getName().compareTo(rhs.getName());
        }
    }
}
