package com.afollestad.cabinet.ui;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import com.afollestad.cabinet.App;
import com.afollestad.cabinet.File;
import com.afollestad.cabinet.R;
import com.afollestad.cabinet.fragments.DirectoryFragment;
import com.afollestad.silk.activities.SilkDrawerActivity;
import com.afollestad.silk.images.SilkImageManager;

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
        navigate(new File(Environment.getExternalStorageDirectory().getAbsolutePath()), false);
    }

    public void navigate(File directory, boolean backStack) {
        FragmentTransaction trans = getFragmentManager().beginTransaction();
        trans.replace(R.id.content_frame, new DirectoryFragment(directory));
        if (backStack) trans.addToBackStack(null);
        else getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        trans.commit();
    }

    @Override
    public void onBackPressed() {
        SilkImageManager dm = App.get(this).getDrawableManager();
        if (dm != null) dm.clear();
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }
}