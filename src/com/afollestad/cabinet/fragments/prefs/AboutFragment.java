package com.afollestad.cabinet.fragments.prefs;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.afollestad.cabinet.R;

/**
 * @author Aidan Follestad (afollestad)
 */
public class AboutFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_about);

        try {
            PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            findPreference("app_version").setSummary(pInfo.versionName + " (build " + pInfo.versionCode + ")");
        } catch (Exception e) {
            e.printStackTrace();
        }

        findPreference("app_dev").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://gplus.to/afollestad"));
                startActivity(Intent.createChooser(intent, getString(R.string.open_with)));
                return true;
            }
        });
        findPreference("app_silk").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://github.com/afollestad/Silk"));
                startActivity(Intent.createChooser(intent, getString(R.string.open_with)));
                return true;
            }
        });
    }

}