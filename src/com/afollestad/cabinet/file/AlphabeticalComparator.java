package com.afollestad.cabinet.file;

/**
 * @author Aidan Follestad (afollestad)
 */
public class AlphabeticalComparator implements java.util.Comparator<File> {

    @Override
    public int compare(File lhs, File rhs) {
        return lhs.getName().compareTo(rhs.getName());
    }
}