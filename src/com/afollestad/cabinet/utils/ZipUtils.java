package com.afollestad.cabinet.utils;

import com.afollestad.cabinet.File;

import java.io.*;
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

    public static void zip(List<File> files, File zipFile) throws IOException {
        BufferedInputStream origin;
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
        try {
            byte data[] = new byte[BUFFER_SIZE];
            for (File file : files) {
                FileInputStream fi = new FileInputStream(file);
                origin = new BufferedInputStream(fi, BUFFER_SIZE);
                try {
                    ZipEntry entry = new ZipEntry(file.getName());
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

    public static List<File> unzip(File zipFile, File location) throws IOException {
        List<File> added = new ArrayList<File>();
        try {
            if (!location.isDirectory()) location.mkdirs();
            ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile));
            try {
                ZipEntry ze;
                while ((ze = zin.getNextEntry()) != null) {
                    File file = new File(location, ze.getName());
                    if (ze.isDirectory()) {
                        if (!file.isDirectory()) file.mkdirs();
                    } else {
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
                        added.add(file);
                    }
                }
            } finally {
                zin.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return added;
    }
}
