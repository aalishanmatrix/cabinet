package com.afollestad.cabinet.file;

/**
 * @author Aidan Follestad (afollestad)
 */
public class ExtensionComparator implements java.util.Comparator<File> {

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