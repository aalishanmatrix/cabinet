package com.afollestad.cabinet.fragments.prefs;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import com.afollestad.cabinet.R;

/**
 * @author Aidan Follestad (afollestad)
 */
public class FilesFragment extends PreferenceFragment {

    private int getSortingSummary(String value) {
        switch (Integer.parseInt(value)) {
            default:
                return R.string.sorting_folderfile;
            case 1:
                return R.string.sorting_alphabetical;
            case 2:
                return R.string.sorting_extension;
            case 3:
                return R.string.sorting_size_lowhigh;
            case 4:
                return R.string.sorting_size_highlow;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_files);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Preference sorting = findPreference("file_sorting");
        sorting.setSummary(getSortingSummary(prefs.getString(sorting.getKey(), "0")));
        sorting.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(getSortingSummary(newValue.toString()));
                return true;
            }
        });
    }
}