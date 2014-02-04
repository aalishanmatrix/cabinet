package com.afollestad.cabinet.file;

import android.os.Environment;
import com.afollestad.cabinet.utils.Utils;
import eu.chainfire.libsuperuser.Shell;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 */
public class LocalFile extends File {

    protected LocalFile() {
    }

    public LocalFile(LocalFile dir, String name) {
        mFile = new java.io.File(dir.getFile(), name);
    }

    public LocalFile(java.io.File dir, String name) {
        mFile = new java.io.File(dir, name);
    }

    public LocalFile(String path) {
        mFile = new java.io.File(path);
    }

    public LocalFile(String dirPath, String name) {
        mFile = new java.io.File(dirPath, name);
    }

    public LocalFile(URI uri) {
        mFile = new java.io.File(uri);
    }

    public LocalFile(java.io.File file) {
        mFile = file;
    }

    protected java.io.File mFile;

    @Override
    public String getName() {
        return mFile.getName();
    }

    @Override
    public String getDisplayName() {
        return mFile.getName();
    }

    @Override
    public String getAbsolutePath() {
        return mFile.getAbsolutePath();
    }

    @Override
    public File getParentFile() {
        return new LocalFile(mFile.getAbsoluteFile());
    }

    @Override
    public java.io.File getFile() {
        return mFile;
    }

    @Override
    public boolean exists() throws Exception {
        return mFile.exists();
    }

    @Override
    public boolean mkdir() throws Exception {
        if (requiresRootAccess()) {
            runAsRoot("mkdir " + mFile.getAbsolutePath());
            return true;
        } else return mFile.mkdir();
    }

    @Override
    public boolean mkdirs() throws Exception {
        if (requiresRootAccess()) {
            runAsRoot("mkdir -p " + mFile.getAbsolutePath());
            return true;
        } else return mFile.mkdirs();
    }

    @Override
    public boolean renameTo(File to) throws Exception {
        if (requiresRootAccess()) {
            runAsRoot("mv \"" + mFile.getAbsolutePath() + "\" \"" + to.getAbsolutePath() + "\"");
            return true;
        } else return mFile.renameTo(to.getFile());
    }

    @Override
    public boolean deleteNonRecursive() throws Exception {
        if (requiresRootAccess()) {
            runAsRoot("rm \"" + mFile.getAbsolutePath() + "\"");
            return true;
        } else return mFile.delete();
    }

    @Override
    public boolean delete() throws Exception {
        if (requiresRootAccess()) {
            String cmd = "rm";
            if (mFile.isDirectory()) cmd += " -Rf";
            else cmd += " -f";
            runAsRoot(cmd + " \"" + mFile.getAbsolutePath() + "\"");
            return true;
        } else return Utils.deleteRecursively(this);
    }

    @Override
    public File[] listFilesUnthreaded() throws Exception {
        if (requiresRootAccess()) {
            if (!Shell.SU.available())
                throw new Exception("Root access unavailable.");
            List<String> output = runAsRoot("ls \"" + getAbsolutePath() + "\"");
            List<File> files = new ArrayList<File>();
            for (String line : output) {
                if (line == null || line.trim().isEmpty()) continue;
                files.add(new LocalFile(mFile, line));
            }
            return files.toArray(new File[files.size()]);
        }
        java.io.File[] results = mFile.listFiles();
        if (results == null) return null;
        List<File> files = new ArrayList<File>();
        for (java.io.File fi : results)
            files.add(new LocalFile(fi));
        return files.toArray(new File[files.size()]);
    }

    @Override
    public boolean isHidden() {
        return mFile.isHidden();
    }

    @Override
    public boolean isDirectory() {
        return mFile.isDirectory();
    }

    @Override
    public long length() {
        return mFile.length();
    }

    @Override
    public boolean isRemoteFile() {
        return false;
    }

    public boolean isStorageDirectory() {
        return mFile.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath());
    }

    public boolean isRootDirectory() {
        return mFile.getAbsolutePath().isEmpty() || mFile.getAbsolutePath().equals("/");
    }

    public boolean requiresRootAccess() {
        return !mFile.getAbsolutePath().startsWith(Environment.getExternalStorageDirectory().getAbsolutePath());
    }

    @Override
    public String getMountedAs() {
        return null; //TODO
    }
}