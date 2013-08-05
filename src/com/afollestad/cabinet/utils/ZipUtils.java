package com.afollestad.cabinet.utils;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author Aidan Follestad (afollestad)
 */
public class ZipUtils {

    private final static int BUFFER_SIZE = 1024;

    public static void zip(File[] files, String zipFile) throws IOException {
        BufferedInputStream origin;
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
        try {
            byte data[] = new byte[BUFFER_SIZE];
            for (int i = 0; i < files.length; i++) {
                FileInputStream fi = new FileInputStream(files[i]);
                origin = new BufferedInputStream(fi, BUFFER_SIZE);
                try {
                    ZipEntry entry = new ZipEntry(files[i].getName());
                    out.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1)
                        out.write(data, 0, count);
                } finally {
                    origin.close();
                }
            }
        } finally {
            out.close();
        }
    }

    public static void unzip(File zipFile, File location) throws IOException {
        try {
            if (!location.isDirectory()) location.mkdirs();
            ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile));
            try {
                ZipEntry ze;
                while ((ze = zin.getNextEntry()) != null) {
                    String path = new File(location, ze.getName()).getAbsolutePath();
                    if (ze.isDirectory()) {
                        File unzipFile = new File(path);
                        if (!unzipFile.isDirectory()) unzipFile.mkdirs();
                    } else {
                        FileOutputStream fout = new FileOutputStream(path, false);
                        try {
                            for (int c = zin.read(); c != -1; c = zin.read())
                                fout.write(c);
                            zin.closeEntry();
                        } finally {
                            fout.close();
                        }
                    }
                }
            } finally {
                zin.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
