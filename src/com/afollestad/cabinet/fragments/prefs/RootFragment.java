package com.afollestad.cabinet.fragments.prefs;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;
import com.afollestad.cabinet.R;
import com.afollestad.cabinet.ui.MainActivity;
import org.sufficientlysecure.rootcommands.RootCommands;

/**
 * @author Aidan Follestad (afollestad)
 */
public class RootFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_root);

        findPreference("root_enabled").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Boolean value = (Boolean) newValue;
                if (value && !RootCommands.rootAccessGiven()) {
                    Toast.makeText(getActivity(), R.string.root_denied, Toast.LENGTH_SHORT).show();
                    return false;
                }
                getActivity().startActivity(new Intent(getActivity(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
                getActivity().finish();
                return true;
            }
        });
    }
}