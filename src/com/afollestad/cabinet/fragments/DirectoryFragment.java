package com.afollestad.cabinet.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import com.afollestad.cabinet.File;
import com.afollestad.cabinet.ui.MainActivity;
import com.afollestad.cabinet.R;
import com.afollestad.cabinet.adapters.FileAdapter;
import com.afollestad.silk.adapters.SilkAdapter;
import com.afollestad.silk.fragments.SilkListFragment;

import java.util.Arrays;

public class DirectoryFragment extends SilkListFragment<File> {

    public DirectoryFragment(File dir) {
        mPath = dir;
    }

    private final File mPath;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getListView().setSelector(R.drawable.selectable_background_cabinet);
        getListView().setDivider(null);
        getListView().setDividerHeight(0);

        java.io.File[] contents = mPath.listFiles();
        Arrays.sort(contents, new File.Comparator());
        getAdapter().clear();
        for (java.io.File fi : contents)
            getAdapter().add(new File(fi));
    }

    @Override
    public int getEmptyText() {
        return R.string.no_files;
    }

    @Override
    protected SilkAdapter<File> initializeAdapter() {
        return new FileAdapter(getActivity());
    }

    @Override
    public void onItemTapped(int index, File item, View view) {
        if (item.isDirectory()) {
            ((MainActivity) getActivity()).navigate(item, true);
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(item), item.getMimeType());
            startActivity(Intent.createChooser(intent, getString(R.string.open_with)));
        }
    }

    @Override
    public boolean onItemLongTapped(int index, File item, View view) {
        return false;
    }

    @Override
    public void onVisibilityChange(boolean visible) {
    }
}
