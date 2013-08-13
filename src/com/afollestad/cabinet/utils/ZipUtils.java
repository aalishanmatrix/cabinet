package com.afollestad.cabinet.utils;

import android.util.Log;
import com.afollestad.cabinet.File;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Handles zipping and unzipping compressed ZIP archive files.
 *
 * @author Aidan Follestad (afollestad)
 */
public class ZipUtils {

    private final static int BUFFER_SIZE = 1024;

    public interface ProgressCallback {
        public void onIncrement();
    }

    private static void log(String message) {
        Log.d("ZipUtils", message);
    }

    private static File findTopLevel(File file, File location) {
        if (file.getParentFile().getAbsolutePath().equals(location.getAbsolutePath()))
            return file;
        return findTopLevel(file.getParentFile(), location);
    }

    private static int getTotalFileCount(File dir) {
        if (dir.isDirectory()) {
            int count = 0;
            for (File fi : dir.listFiles())
                count += getTotalFileCount(fi);
            return count;
        }
        return 1;
    }

    public static int getTotalFileCount(List<File> files) {
        int count = 0;
        for (File fi : files)
            count += getTotalFileCount(fi);
        return count;
    }

    private static void zip(File file, ZipOutputStream zos, File parent, ProgressCallback callback) throws Exception {
        log("Zipping file: " + file.getAbsolutePath());
        byte[] readBuffer = new byte[BUFFER_SIZE];
        int bytesIn;
        FileInputStream fis = new FileInputStream(file);
        ZipEntry anEntry = new ZipEntry(file.getAbsolutePath().substring(parent.getAbsolutePath().length()));
        zos.putNextEntry(anEntry);
        while ((bytesIn = fis.read(readBuffer)) != -1)
            zos.write(readBuffer, 0, bytesIn);
        fis.close();
        if (callback != null) callback.onIncrement();
    }

    private static void zipDir(File directory, ZipOutputStream zos, File parent, ProgressCallback callback) throws Exception {
        log("Zipping directory: " + directory.getAbsolutePath());
        for (File f : directory.listFiles()) {
            if (f.isDirectory()) {
                zipDir(f, zos, parent, callback);
                continue;
            }
            zip(f, zos, parent, callback);
        }
    }

    public static File zip(List<File> files, File destination, ProgressCallback callback) throws Exception {
        destination = Utils.checkForExistence(destination, 0);
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destination));
        for (int i = 0; i < files.size(); i++) {
            File f = files.get(i);
            if (f.isDirectory()) zipDir(f, zos, f.getParentFile(), callback);
            else zip(f, zos, f.getParentFile(), callback);
        }
        zos.close();
        return destination;
    }

    public static List<File> unzip(File zipFile, File destination) throws Exception {
        log("Unzipping '" + zipFile.getAbsolutePath() + "' to: " + destination.getAbsolutePath());
        List<File> added = new ArrayList<File>();
        if (!destination.isDirectory()) destination.mkdirs();
        ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile));
        try {
            ZipEntry ze;
            while ((ze = zin.getNextEntry()) != null) {
                File file = Utils.checkForExistence(new File(destination.getAbsolutePath() + ze.getName()), 0);
                log("Writing: " + file.getAbsolutePath());
                file.getParentFile().mkdirs();
                File topLevel = findTopLevel(file, destination);
                if (!added.contains(topLevel)) added.add(topLevel);
                FileOutputStream fout = new FileOutputStream(file, false);
                try {
                    byte data[] = new byte[BUFFER_SIZE];
                    int count;
                    while ((count = zin.read(data, 0, BUFFER_SIZE)) != -1)
                        fout.write(data, 0, count);
                    zin.closeEntry();
                } finally {
                    fout.close();
                }
            }
        } finally {
            zin.close();
        }
        return added;
    }
}