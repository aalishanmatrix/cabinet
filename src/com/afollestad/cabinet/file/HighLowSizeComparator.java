package com.afollestad.cabinet.file;

/**
 * Sorts files and folders by size, from large to small. Folders are considered large.
 *
 * @author Aidan Follestad (afollestad)
 */
public class HighLowSizeComparator implements java.util.Comparator<File> {

    @Override
    public int compare(File lhs, File rhs) {
        if (lhs.isDirectory() || rhs.length() < lhs.length()) {
            return 1; // move smaller files down
        } else if (rhs.isDirectory() || rhs.length() > lhs.length()) {
            return -1; // move larger files up
        } else {
            return 0; // equal in size
        }
    }
}