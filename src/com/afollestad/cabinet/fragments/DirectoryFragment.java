package com.afollestad.cabinet.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;
import com.afollestad.cabinet.File;
import com.afollestad.cabinet.R;
import com.afollestad.cabinet.adapters.FileAdapter;
import com.afollestad.cabinet.ui.MainActivity;
import com.afollestad.silk.adapters.SilkAdapter;
import com.afollestad.silk.fragments.SilkListFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DirectoryFragment extends SilkListFragment<File> {

    public DirectoryFragment(File dir) {
        mPath = dir;
    }

    private final File mPath;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setSelector(R.drawable.selectable_background_cabinet);
        setupCab(getListView());
        java.io.File[] contents = mPath.listFiles();
        Arrays.sort(contents, new File.Comparator());
        getAdapter().clear();
        for (java.io.File fi : contents)
            getAdapter().add(new File(fi));
    }

    private void setupCab(ListView listView) {
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

            private List<File> getSelectedFiles() {
                List<File> files = new ArrayList<File>();
                int len = getListView().getCount();
                SparseBooleanArray checked = getListView().getCheckedItemPositions();
                for (int i = 0; i < len; i++) {
                    if (checked.get(i)) files.add(getAdapter().getItem(i));
                }
                return files;
            }

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                int count = getListView().getCheckedItemCount();
                if (count == 1)
                    mode.setTitle(getString(R.string.one_file_selected));
                else mode.setTitle(getString(R.string.x_files_selected).replace("{X}", count + ""));
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                final List<File> selectedFiles = getSelectedFiles();
                switch (item.getItemId()) {
                    case R.id.add_shortcut:
                        for (File fi : selectedFiles) ((MainActivity) getActivity()).addShortcut(fi);
                        mode.finish();
                        Toast.makeText(getActivity(), R.string.shorts_updated, Toast.LENGTH_SHORT).show();
                        return true;
                    case R.id.delete:
                        for (File fi : selectedFiles) {
                            if (fi.delete()) getAdapter().remove(fi);
                        }
                        mode.finish();
                        Toast.makeText(getActivity(), R.string.files_deleted, Toast.LENGTH_SHORT).show();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.contextual_ab_file, menu);
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                // Here you can make any necessary updates to the activity when
                // the CAB is removed. By default, selected items are deselected/unchecked.
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                // Here you can perform updates to the CAB due to
                // an invalidate() request
                return false;
            }
        });
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
        getListView().setItemChecked(index, !getListView().isItemChecked(index));
        return true;
    }

    @Override
    public void onVisibilityChange(boolean visible) {
        // Makes sure title gets updated when you go back in the fragment back stack
        if (mPath.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath()))
            getActivity().setTitle(R.string.app_name);
        else getActivity().setTitle(mPath.getName());
    }
}
