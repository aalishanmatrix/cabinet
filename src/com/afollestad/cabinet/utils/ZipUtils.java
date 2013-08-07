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
        public void onUpdate(int progress);
    }

    private static void log(String message) {
        Log.d("ZipUtils", message);
    }

    private static void zip(File file, ZipOutputStream zos, File parent) throws Exception {
        log("Zipping file: " + file.getAbsolutePath());
        byte[] readBuffer = new byte[BUFFER_SIZE];
        int bytesIn;
        FileInputStream fis = new FileInputStream(file);
        ZipEntry anEntry = new ZipEntry(file.getAbsolutePath().substring(parent.getAbsolutePath().length()));
        zos.putNextEntry(anEntry);
        while ((bytesIn = fis.read(readBuffer)) != -1)
            zos.write(readBuffer, 0, bytesIn);
        fis.close();
    }

    private static void zipDir(File directory, ZipOutputStream zos, File parent) throws Exception {
        log("Zipping directory: " + directory.getAbsolutePath());
        for (File f : directory.listFiles()) {
            if (f.isDirectory()) {
                zipDir(f, zos, parent);
                continue;
            }
            zip(f, zos, parent);
        }
    }

    public static void zip(List<File> files, File zipFile, ProgressCallback callback) throws Exception {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
        for (int i = 0; i < files.size(); i++) {
            File f = files.get(i);
            if (f.isDirectory()) zipDir(f, zos, f.getParentFile());
            else zip(f, zos, f.getParentFile());
            callback.onUpdate(i);
        }
        zos.close();
    }

    private static File findTopLevel(File file, File location) {
        if (file.getParentFile().getAbsolutePath().equals(location.getAbsolutePath()))
            return file;
        return findTopLevel(file.getParentFile(), location);
    }

    public static List<File> unzip(File zipFile, File location) throws Exception {
        log("Unzipping '" + zipFile.getAbsolutePath() + "' to: " + location.getAbsolutePath());
        List<File> added = new ArrayList<File>();
        if (!location.isDirectory()) location.mkdirs();
        ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile));
        try {
            ZipEntry ze;
            while ((ze = zin.getNextEntry()) != null) {
                File file = new File(location.getAbsolutePath() + ze.getName());
                log("Writing: " + file.getAbsolutePath());
                file.getParentFile().mkdirs();
                File topLevel = findTopLevel(file, location);
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