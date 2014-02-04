package com.afollestad.cabinet.file.comparators;

import com.afollestad.cabinet.file.File;

/**
 * Sorts files and folders by name, alphabetically.
 *
 * @author Aidan Follestad (afollestad)
 */
public class AlphabeticalComparator implements java.util.Comparator<File> {

    @Override
    public int compare(File lhs, File rhs) {
        return lhs.getName().compareToIgnoreCase(rhs.getName());
    }
}