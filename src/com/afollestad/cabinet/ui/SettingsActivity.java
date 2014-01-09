
package com.afollestad.cabinet.ui;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.afollestad.cabinet.R;

import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 */
public class SettingsActivity extends PreferenceActivity {

    private int mBaseTheme;
    private int mThemeColor;

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mBaseTheme = MainActivity.getCabinetBaseTheme(this);
        setTheme(mBaseTheme);
        mThemeColor = MainActivity.getCabinetThemeColor(this);
        super.onCreate(savedInstanceState);
        if (mThemeColor != 0) getActionBar().setBackgroundDrawable(new ColorDrawable(mThemeColor));
        getActionBar().setDisplayHomeAsUpEnabled(true);

        MainActivity.setupTransparentTints(this);

        final FrameLayout content = (FrameLayout) findViewById(android.R.id.content);
        content.setClipToPadding(false);
        MainActivity.setInsets(this, content);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBaseTheme != MainActivity.getCabinetBaseTheme(this) || mThemeColor != MainActivity.getCabinetThemeColor(this))
            recreate();
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}