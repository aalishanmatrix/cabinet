package com.afollestad.cabinet.fragments.prefs;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import com.afollestad.cabinet.R;

/**
 * @author Aidan Follestad (afollestad)
 */
public class RootFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_root);
    }
}