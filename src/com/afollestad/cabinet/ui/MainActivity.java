package com.afollestad.cabinet.ui;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import com.afollestad.cabinet.File;
import com.afollestad.cabinet.R;
import com.afollestad.cabinet.Shortcuts;
import com.afollestad.cabinet.adapters.DrawerAdapter;
import com.afollestad.cabinet.fragments.DirectoryFragment;
import com.afollestad.silk.activities.SilkDrawerActivity;

import java.util.List;

public class MainActivity extends SilkDrawerActivity {

    private DrawerAdapter mDrawerAdapter;

    @Override
    public int getDrawerIndicatorRes() {
        return R.drawable.ic_navigation_drawer;
    }

    @Override
    public int getDrawerShadowRes() {
        return R.drawable.drawer_shadow;
    }

    @Override
    public int getLayout() {
        return R.layout.main;
    }

    @Override
    public DrawerLayout getDrawerLayout() {
        return (DrawerLayout) findViewById(R.id.drawer_layout);
    }

    @Override
    public int getOpenedTextRes() {
        return R.string.shortcuts;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkFirstTime();
        populateDrawer();
        selectItem(0);
    }

    public void navigate(File directory, boolean backStack) {
        if (directory.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath()))
            setTitle(R.string.app_name);
        else setTitle(directory.getName());

        FragmentTransaction trans = getFragmentManager().beginTransaction();
        trans.replace(R.id.content_frame, new DirectoryFragment(directory));
        if (backStack) trans.addToBackStack(null);
        else getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        trans.commit();
    }

    private void checkFirstTime() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("first_time", true)) return;

        Shortcuts.add(this, new File(Environment.getExternalStorageDirectory()));
        Shortcuts.add(this, new File(Environment.getExternalStorageDirectory(), "Download"));
        Shortcuts.add(this, new File(Environment.getExternalStorageDirectory(), "Music"));
        Shortcuts.add(this, new File(Environment.getExternalStorageDirectory(), "Pictures"));
    }

    private void populateDrawer() {
        ListView drawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerAdapter = new DrawerAdapter(this);
        drawerList.setAdapter(mDrawerAdapter);
        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });

        List<File> shortcuts = Shortcuts.getAll(this);
        for (File fi : shortcuts) {
            mDrawerAdapter.add(new DrawerAdapter.DrawerItem(this, fi));
        }
    }

    private void selectItem(int position) {
        DrawerAdapter.DrawerItem item = mDrawerAdapter.getItem(position);
        if (item.getFile().exists()) navigate(item.getFile(), true);
        else Toast.makeText(this, R.string.folder_not_found, Toast.LENGTH_SHORT).show();
        getDrawerLayout().closeDrawers();
    }

    public void addShortcut(File path) {
        mDrawerAdapter.add(new DrawerAdapter.DrawerItem(this, path));
        Shortcuts.add(this, path);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }
}