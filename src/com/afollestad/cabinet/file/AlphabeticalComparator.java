package com.afollestad.cabinet.file;

/**
 * Sorts files and folders by name, alphabetically.
 *
 * @author Aidan Follestad (afollestad)
 */
public class AlphabeticalComparator implements java.util.Comparator<File> {

    @Override
    public int compare(File lhs, File rhs) {
        return lhs.getName().compareTo(rhs.getName());
    }
}