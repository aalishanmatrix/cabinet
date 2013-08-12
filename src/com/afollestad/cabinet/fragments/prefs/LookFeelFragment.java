package com.afollestad.cabinet.fragments.prefs;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.Html;
import com.afollestad.cabinet.R;

/**
 * @author Aidan Follestad (afollestad)
 */
public class LookFeelFragment extends PreferenceFragment {

    private String getBaseThemeSummary(String value) {
        String[] names = getResources().getStringArray(R.array.basetheme_entries);
        return names[Integer.parseInt(value)];
    }

    private CharSequence getThemeColorSummary(String value) {
        String[] names = getResources().getStringArray(R.array.color_entries);
        String[] values = getResources().getStringArray(R.array.color_values);
        if (value.equals("0")) return names[0];
        String na = "";
        for (int i = 0; i < values.length; i++) {
            if (value.equals(values[i])) {
                na = names[i];
                break;
            }
        }
        return Html.fromHtml("<font color='" + value + "'>" + na + "</font>");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_lookandfeel);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        Preference baseTheme = findPreference("base_theme");
        baseTheme.setSummary(getBaseThemeSummary(prefs.getString(baseTheme.getKey(), "0")));
        baseTheme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(getBaseThemeSummary(newValue.toString()));
                getActivity().recreate();
                return true;
            }
        });

        Preference themeColor = findPreference("theme_color");
        themeColor.setSummary(getThemeColorSummary(prefs.getString(themeColor.getKey(), "0")));
        themeColor.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(getThemeColorSummary(newValue.toString()));
                getActivity().recreate();
                return true;
            }
        });
    }
}