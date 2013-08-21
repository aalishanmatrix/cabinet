package com.afollestad.cabinet.file;

/**
 * Sorts files and folders by size, from small to large. Folders are considered large.
 *
 * @author Aidan Follestad (afollestad)
 */
public class LowHighSizeComparator implements java.util.Comparator<File> {

    @Override
    public int compare(File lhs, File rhs) {
        if (lhs.isDirectory() || rhs.length() < lhs.length()) {
            return -1; // move smaller files up
        } else if (rhs.isDirectory() || rhs.length() > lhs.length()) {
            return 1; // move larger files down
        } else {
            return 0; // equal in size
        }
    }
}