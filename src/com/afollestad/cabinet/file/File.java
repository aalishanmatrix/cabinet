package com.afollestad.cabinet.file;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.MimeTypeMap;
import com.afollestad.cabinet.R;
import com.afollestad.cabinet.utils.Utils;
import com.afollestad.silk.cache.SilkComparable;
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


    public boolean isStorageDirectory() {
        return getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath());
    }

    public boolean isRootDirectory() {
        return getAbsolutePath().isEmpty() || getAbsolutePath().equals("/");
    }

    public boolean requiresRootAccess() {
        return !getAbsolutePath().startsWith(Environment.getExternalStorageDirectory().getAbsolutePath());
    }

    public String getNameNoExtension() {
        if (isDirectory()) return super.getName();
        String name = super.getName();
        if (name.startsWith(".") || !name.substring(1).contains(".")) return name;
        return name.substring(0, name.lastIndexOf('.'));
    }

    public String getExtension() {
        if (isDirectory()) return "";
        String name = super.getName().toLowerCase();
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

    public boolean mount() {
        try {
            Shell shell = Shell.startRootShell();
            Toolbox tb = new Toolbox(shell);
            boolean success = tb.remount(getAbsolutePath(), "rw");
            shell.close();
            if (!success) throw new RuntimeException("Unable to mount " + getAbsolutePath());
            return success;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getMountedAs() {
        try {
            Shell shell = Shell.startRootShell();
            Toolbox tb = new Toolbox(shell);
            String mountedAs = tb.getMountedAs(getAbsolutePath());
            shell.close();
            return mountedAs;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean runAsRoot(String cmd) {
        Log.d("runAsRoot", cmd);
        if (!RootCommands.rootAccessGiven()) return false;
        try {
            Shell shell = Shell.startRootShell();
            SimpleCommand lsApp = new SimpleCommand(cmd);
            shell.add(lsApp).waitForFinish();
            shell.close();
            if (lsApp.getExitCode() != 0)
                throw new RuntimeException("Exit code " + lsApp.getExitCode() + ": " + lsApp.getOutput());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Comparator<File> getComparator(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int sortSetting = Integer.parseInt(prefs.getString("file_sorting", "0"));
        switch (sortSetting) {
            default:
                return new FoldersFirstComparator();
            case 1:
                return new AlphabeticalComparator();
            case 2:
                return new ExtensionComparator();
        }
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

    @Override
    public File[] listFiles() {
        if (requiresRootAccess()) {
            try {
                return listFilesAsRoot();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        java.io.File[] files = super.listFiles();
        List<File> cabinets = new ArrayList<File>();
        for (java.io.File fi : files) cabinets.add(new File(fi));
        return cabinets.toArray(new File[cabinets.size()]);
    }

//    @Override
//    public boolean exists() {
//        if (requiresRootAccess()) {
//            try {
//                Shell shell = Shell.startRootShell();
//                Toolbox tb = new Toolbox(shell);
//                boolean exists = tb.fileExists(getAbsolutePath());
//                shell.close();
//                return exists;
//            } catch (Exception e) {
//                e.printStackTrace();
//                return false;
//            }
//        }
//        return super.exists();
//    }

    @Override
    public boolean mkdir() {
        if (requiresRootAccess()) {
            return runAsRoot("mkdir " + getAbsolutePath());
        }
        return super.mkdir();
    }

    @Override
    public boolean mkdirs() {
        if (requiresRootAccess()) {
            return runAsRoot("mkdir -p " + getAbsolutePath());
        }
        return super.mkdirs();
    }

    @Override
    public boolean renameTo(java.io.File newPath) {
        if (requiresRootAccess()) {
            return RootCommands.rootAccessGiven() && runAsRoot("mv \"" + getAbsolutePath() + "\" \"" + newPath.getAbsolutePath() + "\"");
        }
        return super.renameTo(newPath);
    }

    @Override
    public File getParentFile() {
        return new File(super.getParentFile());
    }

    @Override
    public boolean delete() {
        if (requiresRootAccess()) {
            if (!RootCommands.rootAccessGiven()) return false;
            String cmd = "rm";
            if (isDirectory()) cmd += " -rf";
            else cmd += " -f";
            return runAsRoot(cmd);
        }
        return Utils.deleteRecursively(this);
    }

    public boolean deleteNonRecursive() {
        if (requiresRootAccess()) {
            return RootCommands.rootAccessGiven() && runAsRoot("rm -f \"" + getAbsolutePath() + "\"");
        }
        return super.delete();
    }

    private File[] listFilesAsRoot() throws Exception {
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
}