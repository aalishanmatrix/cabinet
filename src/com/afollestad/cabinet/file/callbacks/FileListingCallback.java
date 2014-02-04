package com.afollestad.cabinet.file.callbacks;

import com.afollestad.cabinet.file.File;

/**
 * @author Aidan Follestad
 */
public interface FileListingCallback {

    public abstract void onResults(File[] files);

    public abstract void onError(Exception ex);
}
