package com.afollestad.cabinet;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.MimeTypeMap;
import com.afollestad.cabinet.utils.Utils;
import com.afollestad.silk.cache.SilkComparable;
import org.sufficientlysecure.rootcommands.RootCommands;
import org.sufficientlysecure.rootcommands.Shell;
import org.sufficientlysecure.rootcommands.command.SimpleCommand;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A wrapper around Java's File class, with convenience methods and root access methods.
 *
 * @author Aidan Follestad (afollestad)
 */
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
        if (name.startsWith(".") || !name.substring(1).contains(".")) return "";
        return name.substring(name.lastIndexOf('.') + 1);
    }

    @Override
    public File[] listFiles() {
        java.io.File[] files = super.listFiles();
        List<File> cabinets = new ArrayList<File>();
        for (java.io.File fi : files) cabinets.add(new File(fi));
        return cabinets.toArray(new File[cabinets.size()]);
    }

    @Override
    public File getParentFile() {
        return new File(super.getParentFile());
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
        if (requiresRootAccess()) {
            if (!RootCommands.rootAccessGiven()) return false;
            try {
                Shell shell = Shell.startRootShell();
                String cmd = "rm";
                if (isDirectory()) cmd += " -rf";
                else cmd += " -f";
                SimpleCommand lsApp = new SimpleCommand(cmd + " \"" + getAbsolutePath() + "\"");
                Log.d("File.delete", cmd + " \"" + getAbsolutePath() + "\"");
                shell.add(lsApp).waitForFinish();
                shell.close();
                return lsApp.getExitCode() == 0;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return Utils.deleteRecursively(this);
    }

    public boolean deleteNonRecursive() {
        if (requiresRootAccess()) {
            if (!RootCommands.rootAccessGiven()) return false;
            try {
                Shell shell = Shell.startRootShell();
                SimpleCommand lsApp = new SimpleCommand("rm -f \"" + getAbsolutePath() + "\"");
                Log.d("File.delete", "rm -f \"" + getAbsolutePath() + "\"");
                shell.add(lsApp).waitForFinish();
                shell.close();
                return lsApp.getExitCode() == 0;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return super.delete();
    }

    /**
     * Checks whether or not this file represents the SD card directory
     */
    public boolean isStorageDirectory() {
        return getAbsolutePath().equals(App.getStorageDirectory().getAbsolutePath());
    }

    public boolean isRootDirectory() {
        return getAbsolutePath().isEmpty() || getAbsolutePath().equals("/");
    }

    public boolean requiresRootAccess() {
        return !getAbsolutePath().startsWith(App.getStorageDirectory().getAbsolutePath());
    }

    public File[] listFilesAsRoot() throws Exception {
        if (!RootCommands.rootAccessGiven()) throw new Exception("Root access denied");
        List<File> files = new ArrayList<File>();
        Shell shell = Shell.startRootShell();
        SimpleCommand lsApp = new SimpleCommand("ls \"" + getAbsolutePath() + "\"");
        shell.add(lsApp).waitForFinish();

        if (lsApp.getExitCode() == 0) {
            String[] splitLines = lsApp.getOutput().split("\n");
            for (String line : splitLines) {
                if (line == null || line.trim().isEmpty()) continue;
                files.add(new File(this, line));
            }
        } else {
            throw new Exception("Root access command returned " + lsApp.getExitCode());
        }

        shell.close();
        return files.toArray(new File[files.size()]);
    }

    private static class AlphabeticalComparator implements java.util.Comparator<File> {
        @Override
        public int compare(File lhs, File rhs) {
            return lhs.getName().compareTo(rhs.getName());
        }
    }

    private static class ExtensionComparator implements java.util.Comparator<File> {
        @Override
        public int compare(File lhs, File rhs) {
            // First, folders always come before files
            if (lhs.isDirectory() && !rhs.isDirectory()) {
                return -1;
            } else if (lhs.isDirectory() && rhs.isDirectory()) {
                return lhs.getName().compareTo(rhs.getName());
            } else if (!lhs.isDirectory() && rhs.isDirectory()) {
                return 1;
            } else {
                // Once folders are sorted, sort files by extension
                return lhs.getExtension().compareTo(rhs.getExtension());
            }
        }
    }

    private static class FoldersFirstComparator implements java.util.Comparator<File> {
        @Override
        public int compare(File lhs, File rhs) {
            // First, folders always come before files
            if (lhs.isDirectory() && !rhs.isDirectory()) {
                return -1;
            } else if (lhs.isDirectory() && rhs.isDirectory()) {
                // Once folders and files are separate, sort alphabetically
                return lhs.getName().compareTo(rhs.getName());
            } else {
                return 1;
            }
        }
    }

    public static Comparator<File> getComparator(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int sortSetting = Integer.parseInt(prefs.getString("file_sorting", "0"));
        switch (sortSetting) {
            default:
                return new File.FoldersFirstComparator();
            case 1:
                return new File.AlphabeticalComparator();
            case 2:
                return new File.ExtensionComparator();
        }
    }
}
