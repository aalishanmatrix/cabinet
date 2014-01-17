package com.afollestad.cabinet.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
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
import com.afollestad.cabinet.App;
import com.afollestad.cabinet.R;
import com.afollestad.cabinet.adapters.DrawerAdapter;
import com.afollestad.cabinet.file.File;
import com.afollestad.cabinet.fragments.DirectoryFragment;
import com.afollestad.cabinet.fragments.dialogs.AddRemoteDialog;
import com.afollestad.cabinet.utils.Shortcuts;
import com.afollestad.cabinet.utils.Utils;
import com.afollestad.silk.activities.SilkDrawerActivity;
import com.afollestad.silk.fragments.list.SilkListFragment;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import org.sufficientlysecure.rootcommands.RootCommands;

import java.util.List;

public class MainActivity extends SilkDrawerActivity {

    private DrawerAdapter mDrawerAdapter;
    private boolean rootEnabled;
    private boolean mPickMode;
    private int mBaseTheme;
    private int mThemeColor;
    private boolean mShowHiddenFiles;

    public static void setInsets(Activity context, View view) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return;
        SystemBarTintManager tintManager = new SystemBarTintManager(context);
        SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
        view.setPadding(0, config.getPixelInsetTop(true), config.getPixelInsetRight(), config.getPixelInsetBottom());
    }

    public static void setupTransparentTints(Activity context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return;
        SystemBarTintManager tintManager = new SystemBarTintManager(context);
        tintManager.setStatusBarTintEnabled(true);
        int tintColor = getCabinetThemeColor(context);
        if (tintColor == 0) {
            TypedArray a = context.obtainStyledAttributes(new int[]{R.attr.status_tint});
            tintColor = a.getColor(0, android.R.color.black);
            a.recycle();
        }
        tintManager.setStatusBarTintColor(tintColor);
    }

    public static int getCabinetBaseTheme(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        switch (Integer.parseInt(prefs.getString("base_theme", "1"))) {
            default:
                return R.style.Cabinet_Light_DarkActionBar;
            case 1:
                return R.style.Cabinet_Light;
            case 2:
                return R.style.Cabinet_Gray;
            case 3:
                return R.style.Cabinet;
            case 4:
                return R.style.Cabinet_Black;
        }
    }

    public static int getCabinetThemeColor(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String color = prefs.getString("theme_color", "0");
        if (color.equals("0")) {
            String baseTheme = prefs.getString("base_theme", "0");
            if (baseTheme.equals("2"))
                color = "#2d2d2d";
            else if (baseTheme.equals("3"))
                color = "#000000";
            else return 0;
        }
        return Color.parseColor(color);
    }

    @Override
    public int getDrawerIndicatorRes() {
        if (mBaseTheme == R.style.Cabinet || mBaseTheme == R.style.Cabinet_Light_DarkActionBar ||
                mBaseTheme == R.style.Cabinet_Black || mBaseTheme == R.style.Cabinet_Gray) {
            return R.drawable.ic_navigation_drawer_light;
        }
        return R.drawable.ic_navigation_drawer_dark;
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

    private boolean processIntent() {
        Intent i = getIntent();
        return i.getAction() != null && (i.getAction().equals(Intent.ACTION_GET_CONTENT) || i.getAction().equals(Intent.ACTION_PICK));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mBaseTheme = getCabinetBaseTheme(this);
        setTheme(mBaseTheme);
        mThemeColor = getCabinetThemeColor(this);
        mShowHiddenFiles = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("show_hidden_files", false);
        super.onCreate(savedInstanceState);
        if (mThemeColor != 0) getActionBar().setBackgroundDrawable(new ColorDrawable(mThemeColor));
        if (!checkFirstTime()) {
            // If it's not the first time, populate the drawer now, otherwise wait for root prompt
            populateDrawer();
        }
        mPickMode = processIntent();
        if (savedInstanceState == null) {
            navigate(new File(Environment.getExternalStorageDirectory()), false);
        } else ((SilkListFragment) getFragmentManager().findFragmentById(R.id.content_frame)).recreateAdapter();
        setupTransparentTints(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (rootEnabled != prefs.getBoolean("root_enabled", false)) recreate();
        else if (mBaseTheme != getCabinetBaseTheme(this) || mThemeColor != getCabinetThemeColor(this) ||
                mShowHiddenFiles != PreferenceManager.getDefaultSharedPreferences(this).getBoolean("show_hidden_files", false)) {
            recreate();
        }
    }

    public void navigate(File directory, boolean backStack) {
        final FragmentTransaction trans = getFragmentManager().beginTransaction();
        trans.replace(R.id.content_frame, DirectoryFragment.newInstance(directory, mPickMode));
        if (backStack) {
            trans.addToBackStack(null);
        } else {
            getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        trans.commit();
    }

    private boolean checkFirstTime() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("first_time", true)) return false;

        // Add default shortcuts
        File storage = new File(Environment.getExternalStorageDirectory());
        Shortcuts.add(this, storage);
        Shortcuts.add(this, new File(storage, "Download"));
        Shortcuts.add(this, new File(storage, "Music"));
        Shortcuts.add(this, new File(storage, "Pictures"));
        prefs.edit().putBoolean("first_time", false).commit();

        new AlertDialog.Builder(this)
                .setTitle(R.string.enable_root)
                .setMessage(R.string.enable_root_prompt)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (RootCommands.rootAccessGiven()) {
                            prefs.edit().putBoolean("root_enabled", true).commit();
                        } else {
                            Toast.makeText(MainActivity.this, R.string.root_disabled, Toast.LENGTH_LONG).show();
                        }
                        populateDrawer();
                        getDrawerLayout().openDrawer(Gravity.START);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        prefs.edit().putBoolean("root_enabled", false).commit();
                        populateDrawer();
                        getDrawerLayout().openDrawer(Gravity.START);
                    }
                }).show();
        return true;
    }

    private void populateDrawer() {
        ListView drawerList = (ListView) findViewById(R.id.left_drawer);
        drawerList.setEmptyView(findViewById(R.id.drawer_empty));
        setInsets(this, drawerList);
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

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        rootEnabled = prefs.getBoolean("root_enabled", false);
        if (rootEnabled) mDrawerAdapter.add(new DrawerAdapter.DrawerItem(this, new File("/"), false));
        List<File> shortcuts = Shortcuts.getAll(this);
        for (File fi : shortcuts) {
            mDrawerAdapter.add(new DrawerAdapter.DrawerItem(this, fi, !fi.isStorageDirectory()));
        }
    }

    private void selectItem(int position) {
        DrawerAdapter.DrawerItem item = mDrawerAdapter.getItem(position);
        getDrawerLayout().closeDrawers();
        try {
            if (item.getFile().exists()) {
                navigate(item.getFile(), !item.getFile().isStorageDirectory());
            } else Toast.makeText(this, R.string.folder_not_found, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Utils.showErrorDialog(this, e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        App.get(this).disconnectSftp();
    }

    public void addShortcut(File path) {
        mDrawerAdapter.add(new DrawerAdapter.DrawerItem(this, path, true));
        Shortcuts.add(this, path);
    }

    private void removeShortcut(final int position) {
        final DrawerAdapter.DrawerItem shortcut = mDrawerAdapter.getItem(position);
        if (!shortcut.isRemoveable()) {
            Toast.makeText(this, R.string.shortcut_not_removeable, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.remove_shortcut)
                .setMessage(getString(R.string.confirm_remove_shortcut).replace("{path}", shortcut.getFile().getAbsolutePath()))
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Shortcuts.remove(MainActivity.this, position - 1);
                        mDrawerAdapter.remove(shortcut);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.addRemote).setVisible(isDrawerOpen());
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.addRemote:
                new AddRemoteDialog(new AddRemoteDialog.OnaAddedListener() {
                    @Override
                    public void onAdded(File file) {
                        addShortcut(file);
                    }
                }).show(getFragmentManager(), "add_remote_dialog");
                return true;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}