package com.afollestad.cabinet.ui;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import com.afollestad.cabinet.File;
import com.afollestad.cabinet.R;
import com.afollestad.cabinet.adapters.DrawerAdapter;
import com.afollestad.cabinet.fragments.DirectoryFragment;
import com.afollestad.cabinet.utils.Shortcuts;
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
        if (directory.isStorageDirectory())
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
        // Add default shortcuts
        Shortcuts.add(this, new File(Environment.getExternalStorageDirectory()));
        Shortcuts.add(this, new File(Environment.getExternalStorageDirectory(), "Download"));
        Shortcuts.add(this, new File(Environment.getExternalStorageDirectory(), "Music"));
        Shortcuts.add(this, new File(Environment.getExternalStorageDirectory(), "Pictures"));
        getDrawerLayout().openDrawer(Gravity.START);
        prefs.edit().putBoolean("first_time", false).commit();
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
        drawerList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                removeShortcut(position);
                return true;
            }
        });

        List<File> shortcuts = Shortcuts.getAll(this);
        for (File fi : shortcuts) {
            mDrawerAdapter.add(new DrawerAdapter.DrawerItem(this, fi));
        }
    }

    private void selectItem(int position) {
        DrawerAdapter.DrawerItem item = mDrawerAdapter.getItem(position);
        if (item.getFile().exists()) {
            boolean backStack = true;
            if (item.getFile().isStorageDirectory())
                backStack = false;
            navigate(item.getFile(), backStack);
        } else Toast.makeText(this, R.string.folder_not_found, Toast.LENGTH_SHORT).show();
        getDrawerLayout().closeDrawers();
    }

    public void addShortcut(File path) {
        mDrawerAdapter.add(new DrawerAdapter.DrawerItem(this, path));
        Shortcuts.add(this, path);
    }

    private void removeShortcut(final int position) {
        final DrawerAdapter.DrawerItem shortcut = mDrawerAdapter.getItem(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.remove_shortcut)
                .setMessage(getString(R.string.confirm_remove_shortcut).replace("{path}", shortcut.getFile().getAbsolutePath()))
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Shortcuts.remove(MainActivity.this, position);
                        mDrawerAdapter.remove(shortcut);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:
                About.showDialog(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}