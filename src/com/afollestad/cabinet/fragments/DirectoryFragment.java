package com.afollestad.cabinet.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.SparseBooleanArray;
import android.view.*;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ListView;
import com.afollestad.cabinet.App;
import com.afollestad.cabinet.R;
import com.afollestad.cabinet.adapters.FileAdapter;
import com.afollestad.cabinet.cab.DirectoryCAB;
import com.afollestad.cabinet.file.File;
import com.afollestad.cabinet.ui.MainActivity;
import com.afollestad.cabinet.utils.Clipboard;
import com.afollestad.cabinet.utils.Shortcuts;
import com.afollestad.cabinet.utils.Utils;
import com.afollestad.silk.Silk;
import com.afollestad.silk.adapters.SilkAdapter;
import com.afollestad.silk.fragments.list.SilkListFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Lists files in a directory.
 *
 * @author Aidan Follestad
 */
public class DirectoryFragment extends SilkListFragment<File> implements FileAdapter.ThumbnailClickListener {

    private File mPath;
    private boolean mPickMode;

    public DirectoryFragment() {
    }

    public DirectoryFragment(File dir, boolean pickMode) {
        mPath = dir;
        mPickMode = pickMode;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("path", mPath.getAbsolutePath());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey("path"))
            mPath = new File(savedInstanceState.getString("path"));
    }

    public File getPath() {
        return mPath;
    }

    @Override
    public int getLayout() {
        if (Silk.isTablet(getActivity())) {
            // If the device is a tablet, a GridView layout is used instead of a ListView
            return R.layout.fragment_grid;
        }
        return R.layout.fragment_list_root;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey("path"))
            mPath = new File(savedInstanceState.getString("path"));
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Update the activity title with the directory name
        if (mPath.isStorageDirectory())
            getActivity().setTitle(R.string.sdcard);
        else if (mPath.isRootDirectory())
            getActivity().setTitle(R.string.root);
        else getActivity().setTitle(mPath.getName());

        load();
    }

    private void load() {
        if (mPath == null) return;
        setLoading(true);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final File[] contents = mPath.listFiles();
                    Arrays.sort(contents, File.getComparator(getActivity()));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            getAdapter().clear();
                            for (java.io.File fi : contents)
                                getAdapter().add(new File(fi));
                        }
                    });
                } catch (final RuntimeException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setEmptyText(e.getMessage());
                        }
                    });
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLoadComplete(false);
                    }
                });
            }
        });
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();
    }

    private void invalidateMountedAs() {
        if (getView() == null) return;
        Button mount = (Button) getView().findViewById(R.id.mount);
        mount.setText(getString(R.string.mounted_as_x).replace("{X}", getPath().getMountedAs().toUpperCase()));
    }

    private boolean isMounted() {
        return !getPath().getMountedAs().equalsIgnoreCase("ro");
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setFastScrollEnabled(true);
        setupCab(getListView());
        if (getAdapter().getCount() == 0) load();

        Button mount = (Button) view.findViewById(R.id.mount);
        if (getPath().requiresRootAccess()) {
            mount.setVisibility(View.VISIBLE);
            invalidateMountedAs();
            mount.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isMounted()) {
                        getPath().unmount();
                    } else getPath().mount();
                    invalidateMountedAs();
                }
            });
        } else mount.setVisibility(View.GONE);

        getListView().setClipToPadding(false);
        MainActivity.setInsets(getActivity(), getListView());
    }

    private void setupCab(AbsListView listView) {
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
                mode.invalidate();
                int count = getListView().getCheckedItemCount();
                if (count == 1)
                    mode.setTitle(getString(R.string.one_file_selected));
                else mode.setTitle(getString(R.string.x_files_selected).replace("{X}", count + ""));
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return DirectoryCAB.handleAction(DirectoryFragment.this, item.getItemId(), getSelectedFiles(), mode);
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.contextual_ab_file, menu);
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                List<File> selectedFiles = getSelectedFiles();
                boolean hasFolders = false;
                boolean hasFiles = false;
                for (File fi : selectedFiles) {
                    if (fi.isDirectory()) hasFolders = true;
                    else hasFiles = true;
                    if (hasFiles && hasFolders) break;
                }
                menu.findItem(R.id.add_shortcut).setVisible(!hasFiles);
                menu.findItem(R.id.share).setVisible(!hasFolders);
                menu.findItem(R.id.unzip).setVisible(shouldShowUnzip(selectedFiles));
                return true;
            }

            private boolean shouldShowUnzip(List<File> selectedFiles) {
                boolean show = true;
                for (File fi : selectedFiles) {
                    if (fi.getExtension() != null && !fi.getExtension().equals("zip")) {
                        show = false;
                        break;
                    }
                }
                return show;
            }
        });
    }

    @Override
    public int getEmptyText() {
        return R.string.no_files;
    }

    @Override
    protected SilkAdapter<File> initializeAdapter() {
        return new FileAdapter(getActivity(), this);
    }

    @Override
    public void onItemTapped(int index, File item, View view) {
        if (item.isDirectory()) {
            ((MainActivity) getActivity()).navigate(item, true);
        } else {
            if (mPickMode) {
                getActivity().setResult(Activity.RESULT_OK, new Intent().setData(Uri.fromFile(item)));
                getActivity().finish();
                return;
            }
            Utils.openFile(getActivity(), item);
        }
    }

    @Override
    public boolean onItemLongTapped(int index, File item, View view) {
        getListView().setItemChecked(index, !getListView().isItemChecked(index));
        return true;
    }

    private void setSortMode(int mode) {
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putInt("sort_mode", mode).commit();
        load();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_directory, menu);
        menu.findItem(R.id.add_shortcut).setVisible(!Shortcuts.contains(getActivity(), mPath));
        menu.findItem(R.id.paste).setVisible(App.get(getActivity()).getClipboard().canPaste());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        switch (prefs.getInt("sort_mode", 0)) {
            default:
                menu.findItem(R.id.sortNameFoldersTop).setChecked(true);
                break;
            case 1:
                menu.findItem(R.id.sortName).setChecked(true);
                break;
            case 2:
                menu.findItem(R.id.sortExtension).setChecked(true);
                break;
            case 3:
                menu.findItem(R.id.sortSizeLowHigh).setChecked(true);
                break;
            case 4:
                menu.findItem(R.id.sortSizeHighLow).setChecked(true);
                break;
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_shortcut:
                MainActivity activity = (MainActivity) getActivity();
                activity.addShortcut(mPath);
                activity.getDrawerLayout().openDrawer(Gravity.START);
                return true;
            case R.id.paste:
                startPaste(this);
                return true;
            case R.id.new_folder:
                newFolder();
                return true;
            case R.id.sortNameFoldersTop:
                setSortMode(0);
                item.setChecked(true);
                return true;
            case R.id.sortName:
                setSortMode(1);
                item.setChecked(true);
                return true;
            case R.id.sortExtension:
                setSortMode(2);
                item.setChecked(true);
                return true;
            case R.id.sortSizeLowHigh:
                setSortMode(3);
                item.setChecked(true);
                return true;
            case R.id.sortSizeHighLow:
                setSortMode(4);
                item.setChecked(true);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void newFolder() {
        Utils.showInputDialog(getActivity(), R.string.new_folder, 0, null, new Utils.InputCallback() {
            @Override
            public void onSubmit(String name) {
                if (name.isEmpty()) name = getActivity().getString(R.string.untitled);
                File newFile = new File(mPath, name);
                if (newFile.mkdir()) {
                    getAdapter().add(newFile);
                    DirectoryCAB.resortFragmentList(DirectoryFragment.this);
                }
            }
        });
    }

    private void startPaste(final DirectoryFragment fragment) {
        Activity context = fragment.getActivity();
        final Clipboard cb = App.get(context).getClipboard();
        String paths = "";
        for (File fi : cb.get()) paths += "<i>" + fi.getName() + "</i><br/>";
        String message;
        int action;
        if (cb.getType() == Clipboard.Type.COPY) {
            message = context.getString(R.string.confirm_copy_paste);
            action = R.string.copy;
        } else {
            message = context.getString(R.string.confirm_cut_paste);
            action = R.string.move;
        }
        message = message.replace("{paths}", paths).replace("{dest}", fragment.getPath().getAbsolutePath());

        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getActivity());
        builder.setTitle(R.string.paste).setMessage(Html.fromHtml(message))
                .setPositiveButton(action, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        ProgressDialog progress = Utils.showProgressDialog(fragment.getActivity(), R.string.paste, cb.get().size());
                        cb.performPaste(fragment, progress);
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
    public void onThumbnailClicked(int index) {
        getListView().setItemChecked(index, !getListView().isItemChecked(index));
    }
}
