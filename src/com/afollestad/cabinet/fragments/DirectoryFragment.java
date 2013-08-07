package com.afollestad.cabinet.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.SparseBooleanArray;
import android.view.*;
import android.widget.AbsListView;
import android.widget.ListView;
import com.afollestad.cabinet.App;
import com.afollestad.cabinet.File;
import com.afollestad.cabinet.R;
import com.afollestad.cabinet.adapters.FileAdapter;
import com.afollestad.cabinet.cab.DirectoryCAB;
import com.afollestad.cabinet.ui.MainActivity;
import com.afollestad.cabinet.utils.Clipboard;
import com.afollestad.cabinet.utils.Shortcuts;
import com.afollestad.cabinet.utils.Utils;
import com.afollestad.silk.Silk;
import com.afollestad.silk.adapters.SilkAdapter;
import com.afollestad.silk.fragments.SilkListFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Lists files in a directory.
 *
 * @author Aidan Follestad
 */
public class DirectoryFragment extends SilkListFragment<File> implements FileAdapter.ThumbnailClickListener {

    public DirectoryFragment() {
    }

    public DirectoryFragment(File dir, boolean pickMode) {
        mPath = dir;
        mPickMode = pickMode;
    }

    private File mPath;
    private boolean mPickMode;

    public File getPath() {
        return mPath;
    }

    @Override
    public int getLayout() {
        if (Silk.isTablet(getActivity()))
            return R.layout.fragment_grid;
        return super.getLayout();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public void onVisibilityChange(boolean visible) {
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

        if (((FileAdapter) getAdapter()).invalidate()) {
            // Reload the list if the user has changed settings
            load();
        }
    }

    private void load() {
        if (mPath == null) return;
        setLoading(true);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final File[] contents = mPath.requiresRootAccess() ?
                            mPath.listFilesAsRoot() : mPath.listFiles();
                    Arrays.sort(contents, File.getComparator(getActivity()));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            getAdapter().clear();
                            for (java.io.File fi : contents)
                                getAdapter().add(new File(fi));
                        }
                    });
                } catch (final Exception e) {
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

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setSelector(R.drawable.selectable_background_cabinet);
        getListView().setFastScrollEnabled(true);
        setupCab(getListView());
        if (getAdapter().getCount() == 0) load();
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
            String type = item.getMimeType();
            if (type == null || type.trim().isEmpty()) type = "*/*";
            Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(item), type);
            startActivity(Intent.createChooser(intent, getString(R.string.open_with)));
        }
    }

    @Override
    public boolean onItemLongTapped(int index, File item, View view) {
        getListView().setItemChecked(index, !getListView().isItemChecked(index));
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_directory, menu);
        menu.findItem(R.id.add_shortcut).setVisible(!Shortcuts.contains(getActivity(), mPath));
        menu.findItem(R.id.paste).setVisible(
                App.get(getActivity()).getClipboard().canPaste(mPath));
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
