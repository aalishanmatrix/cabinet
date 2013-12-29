package com.afollestad.cabinet.fragments.prefs;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.ListView;

/**
 * @author Aidan Follestad (afollestad)
 */
public class BasePreferenceFragment extends PreferenceFragment {

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ListView list = (ListView) view.findViewById(android.R.id.list);
        list.setClipToPadding(false);
    }
}
