package com.afollestad.cabinet.ui;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.afollestad.cabinet.File;
import com.afollestad.cabinet.R;
import com.afollestad.cabinet.adapters.DrawerAdapter;
import com.afollestad.cabinet.fragments.DirectoryFragment;
import com.afollestad.silk.activities.SilkDrawerActivity;

public class MainActivity extends SilkDrawerActivity {

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

    private void populateDrawer() {
        ListView drawerList = (ListView) findViewById(R.id.left_drawer);
        DrawerAdapter mAdapter = new DrawerAdapter(this);
        drawerList.setAdapter(mAdapter);
        String[] defaultItems = getResources().getStringArray(R.array.drawer_items_default);
        for (int i = 0; i < defaultItems.length; i++) {
            if (i > 1) {
                File dir = new File(Environment.getExternalStorageDirectory(), defaultItems[i]);
                if (!dir.exists()) continue;
            }
            mAdapter.add(new DrawerAdapter.DrawerItem(defaultItems[i]));
        }
        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });
    }

    private void selectItem(int position) {
        if (position == 0) {
            navigate(new File(Environment.getExternalStorageDirectory()), false);
            getDrawerLayout().closeDrawers();
            return;
        }
        String[] defaultItems = getResources().getStringArray(R.array.drawer_items_default);
        File fi = new File(Environment.getExternalStorageDirectory(), defaultItems[position]);
        navigate(fi, true);
        getDrawerLayout().closeDrawers();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }
}