package com.afollestad.cabinet.file;

/**
 * Sorts folders and files by name, alphabetically. Folders will always be at the top.
 *
 * @author Aidan Follestad (afollestad)
 */
class FoldersFirstComparator implements java.util.Comparator<File> {

    @Override
    public int compare(File lhs, File rhs) {
        if (lhs.isDirectory() && !rhs.isDirectory()) {
            // Folders before files
            return -1;
        } else if (lhs.isDirectory() && rhs.isDirectory() ||
                !lhs.isDirectory() && !rhs.isDirectory()) {
            // Once folders and files are separate, sort alphabetically
            return lhs.getName().compareTo(rhs.getName());
        } else if (!lhs.isDirectory() && rhs.isDirectory()) {
            // Files below folders
            return 1;
        } else return 0;
    }
}