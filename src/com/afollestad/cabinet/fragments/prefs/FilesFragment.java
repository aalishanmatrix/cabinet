package com.afollestad.cabinet.fragments.prefs;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.afollestad.cabinet.R;

/**
 * @author Aidan Follestad (afollestad)
 */
public class FilesFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_files);

        findPreference("file_sorting").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                switch (Integer.parseInt(newValue.toString())) {
                    default:
                        preference.setSummary(R.string.sorting_folderfile);
                        break;
                    case 1:
                        preference.setSummary(R.string.sorting_alphabetical);
                        break;
                    case 2:
                        preference.setSummary(R.string.sorting_extension);
                        break;
                }
                return true;
            }
        });
    }
}