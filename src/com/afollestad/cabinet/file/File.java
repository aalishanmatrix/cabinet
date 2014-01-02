package com.afollestad.cabinet.file;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.MimeTypeMap;
import com.afollestad.cabinet.R;
import com.afollestad.cabinet.utils.Utils;
import com.afollestad.silk.caching.SilkComparable;
import org.sufficientlysecure.rootcommands.RootCommands;
import org.sufficientlysecure.rootcommands.Shell;
import org.sufficientlysecure.rootcommands.Toolbox;
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
public class File implements SilkComparable<File> {

    public File(File dir, String name) {
        mFile = new java.io.File(dir.getFile(), name);
    }

    public File(java.io.File dir, String name) {
        mFile = new java.io.File(dir, name);
    }

    public File(String path) {
        mFile = new java.io.File(path);
    }

    public File(String dirPath, String name) {
        mFile = new java.io.File(dirPath, name);
    }

    public File(URI uri) {
        mFile = new java.io.File(uri);
    }

    public File(java.io.File file) {
        mFile = new java.io.File(file.getAbsolutePath());
    }

    private java.io.File mFile;

    public boolean isStorageDirectory() {
        return mFile.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath());
    }

    public boolean isRootDirectory() {
        return mFile.getAbsolutePath().isEmpty() || mFile.getAbsolutePath().equals("/");
    }

    public boolean requiresRootAccess() {
        return !mFile.getAbsolutePath().startsWith(Environment.getExternalStorageDirectory().getAbsolutePath());
    }

    public java.io.File getFile() {
        return mFile;
    }

    public boolean isDirectory() {
        return mFile.isDirectory();
    }

    public boolean isHidden() {
        return mFile.isHidden();
    }

    public long length() {
        return mFile.length();
    }

    public String getAbsolutePath() {
        return mFile.getAbsolutePath();
    }

    public String getName() {
        return mFile.getName();
    }

    public String getNameNoExtension() {
        if (mFile.isDirectory()) return mFile.getName();
        String name = mFile.getName();
        if (name.startsWith(".") || !name.substring(1).contains(".")) return name;
        return name.substring(0, name.lastIndexOf('.'));
    }

    public String getExtension() {
        if (mFile.isDirectory()) return "";
        String name = mFile.getName().toLowerCase();
        if (name.startsWith(".") || !name.substring(1).contains(".")) return "";
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

    public boolean mount() throws Exception {
        Shell shell = Shell.startRootShell();
        Toolbox tb = new Toolbox(shell);
        boolean success = tb.remount(mFile.getAbsolutePath(), "rw");
        shell.close();
        return success;
    }

    public boolean unmount() throws Exception {
        Shell shell = Shell.startRootShell();
        Toolbox tb = new Toolbox(shell);
        boolean success = tb.remount(mFile.getAbsolutePath(), "ro");
        shell.close();
        return success;
    }

    public String getMountedAs() throws Exception {
        Shell shell = Shell.startRootShell();
        Toolbox tb = new Toolbox(shell);
        String mountedAs = tb.getMountedAs(mFile.getAbsolutePath());
        shell.close();
        return mountedAs;
    }

    void runAsRoot(String cmd) throws Exception {
        Log.d("runAsRoot", cmd);
        if (!RootCommands.rootAccessGiven()) {
            throw new Exception("Root access denied.");
        }
        Shell shell = Shell.startRootShell();
        SimpleCommand lsApp = new SimpleCommand(cmd);
        shell.add(lsApp).waitForFinish();
        shell.close();
        if (lsApp.getExitCode() != 0)
            throw new Exception("Exit code " + lsApp.getExitCode() + ": " + lsApp.getOutput());
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


    public String getSizeString(Context context) {
        if (mFile.isDirectory())
            return context.getString(R.string.directory);
        return humanReadableByteCount(mFile.length(), true);
    }

    private String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public boolean exists() throws Exception {
        return mFile.exists();
    }

    public void mkdir() throws Exception {
        if (requiresRootAccess())
            runAsRoot("mkdir " + mFile.getAbsolutePath());
        else mFile.mkdir();
    }

    public void mkdirs() throws Exception {
        if (requiresRootAccess())
            runAsRoot("mkdir -p " + mFile.getAbsolutePath());
        else mFile.mkdirs();
    }

    public void renameTo(File newPath) throws Exception {
        if (requiresRootAccess())
            runAsRoot("mv \"" + mFile.getAbsolutePath() + "\" \"" + newPath.getAbsolutePath() + "\"");
        else mFile.renameTo(newPath.getFile());
    }

    public File getParentFile() {
        return new File(mFile.getParentFile());
    }

    public boolean delete() throws Exception {
        if (requiresRootAccess()) {
            String cmd = "rm";
            if (mFile.isDirectory()) cmd += " -Rf";
            else cmd += " -f";
            runAsRoot(cmd + " \"" + mFile.getAbsolutePath() + "\"");
            return true;
        } else return Utils.deleteRecursively(this);
    }

    public boolean deleteNonRecursive() {
        return mFile.delete();
    }

    public File[] listFiles() throws Exception {
        if (requiresRootAccess()) {
            if (!RootCommands.rootAccessGiven()) throw new Exception("Root access denied");
            List<File> files = new ArrayList<File>();
            Shell shell = Shell.startRootShell();
            SimpleCommand lsApp = new SimpleCommand("ls \"" + mFile.getAbsolutePath() + "\"");
            shell.add(lsApp).waitForFinish();
            if (lsApp.getExitCode() == 0) {
                String[] splitLines = lsApp.getOutput().split("\n");
                for (String line : splitLines) {
                    if (line == null || line.trim().isEmpty()) continue;
                    files.add(new File(mFile, line));
                }
            } else throw new Exception("Root access command returned " + lsApp.getExitCode());
            shell.close();
            return files.toArray(new File[files.size()]);
        } else {
            java.io.File[] files = mFile.listFiles();
            if (files == null || files.length == 0) return null;
            List<File> cabinets = new ArrayList<File>();
            for (java.io.File fi : files) cabinets.add(new File(fi));
            return cabinets.toArray(new File[cabinets.size()]);
        }
    }

    @Override
    public Object getSilkId() {
        return mFile.getAbsolutePath();
    }

    @Override
    public boolean equalTo(File other) {
        return mFile.getAbsolutePath().equals(mFile.getAbsolutePath());
    }
}