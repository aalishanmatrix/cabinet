package com.afollestad.cabinet.cab;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.Gravity;
import android.widget.Toast;
import com.afollestad.cabinet.App;
import com.afollestad.cabinet.R;
import com.afollestad.cabinet.file.File;
import com.afollestad.cabinet.file.LocalFile;
import com.afollestad.cabinet.fragments.DirectoryFragment;
import com.afollestad.cabinet.ui.MainActivity;
import com.afollestad.cabinet.utils.Clipboard;
import com.afollestad.cabinet.utils.Utils;
import com.afollestad.cabinet.utils.ZipUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Convenience methods for the contextual action bar that's displayed for files.
 *
 * @author Aidan Follestad (afollestad)
 */
public class DirectoryCAB {

    public static boolean handleAction(DirectoryFragment fragment, int actionId, List<File> selectedFiles, ActionMode mode) {
        switch (actionId) {
            case R.id.add_shortcut:
                MainActivity activity = (MainActivity) fragment.getActivity();
                for (File fi : selectedFiles) activity.addShortcut(fi);
                activity.getDrawerLayout().openDrawer(Gravity.START);
                break;
            case R.id.share:
                fragment.getActivity().startActivity(getShareIntent(fragment.getActivity(), selectedFiles));
                break;
            case R.id.copy:
                App.get(fragment.getActivity()).getClipboard().clear();
                for (File fi : selectedFiles) App.get(fragment.getActivity()).getClipboard().add(fi);
                App.get(fragment.getActivity()).getClipboard().setType(Clipboard.Type.COPY);
                break;
            case R.id.cut:
                App.get(fragment.getActivity()).getClipboard().clear();
                App.get(fragment.getActivity()).getClipboard().setType(Clipboard.Type.CUT);
                for (File fi : selectedFiles) App.get(fragment.getActivity()).getClipboard().add(fi);
                break;
            case R.id.rename:
                performRename(fragment, selectedFiles);
                break;
            case R.id.select_all:
                selectAll(fragment);
                return true;
            case R.id.delete:
                performDelete(fragment, selectedFiles, true);
                break;
            case R.id.zip:
                performZip(fragment, selectedFiles);
                break;
            case R.id.unzip:
                performUnzip(fragment, selectedFiles);
                break;
            default:
                return false;
        }
        mode.finish();
        fragment.getActivity().invalidateOptionsMenu();
        return true;
    }

    private static void performRename(final DirectoryFragment fragment, final List<File> selectedFiles) {
        Utils.showInputDialog(fragment.getActivity(), R.string.rename, R.string.new_names, null,
                new Utils.InputCallback() {
                    @Override
                    public void onSubmit(String input) {
                        if (input == null || input.trim().isEmpty()) return;
                        input = input.trim();
                        if (selectedFiles.size() == 1) {
                            final File fi = selectedFiles.get(0);
                            fragment.getAdapter().remove(fi);
                            final String ext = fi.getExtension();
                            if (!TextUtils.isEmpty(ext)) input += "." + ext;
                            final File newFile = Utils.checkForExistence(fragment.getActivity(), new LocalFile((LocalFile) fi.getParentFile(), input), 0);
                            try {
                                fi.renameTo(newFile);
                            } catch (final Exception e) {
                                e.printStackTrace();
                                Utils.showErrorDialog(fragment.getActivity(), e);
                                return;
                            }
                            fragment.getAdapter().update(newFile);
                            return;
                        }
                        for (final File fi : selectedFiles) {
                            fragment.getAdapter().remove(fi);
                            final String ext = fi.getExtension();
                            if (!TextUtils.isEmpty(ext)) input += "." + ext;
                            final File newFile = Utils.checkForExistence(fragment.getActivity(), new LocalFile((LocalFile) fi.getParentFile(), input), 0);
                            try {
                                fi.renameTo(newFile);
                            } catch (final Exception e) {
                                e.printStackTrace();
                                Utils.showErrorDialog(fragment.getActivity(), e);
                                return;
                            }
                            fragment.getAdapter().update(newFile);
                        }
                        fragment.getAdapter().notifyDataSetChanged();
                    }
                });
    }

    private static Intent getShareIntent(Activity context, List<File> files) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("*/*");
        if (files.size() == 1) {
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(files.get(0).getFile()));
        } else {
            ArrayList<Uri> attachments = new ArrayList<Uri>();
            for (File fi : files) attachments.add(Uri.fromFile(fi.getFile()));
            shareIntent.putExtra(Intent.EXTRA_STREAM, attachments);
        }
        return Intent.createChooser(shareIntent, context.getString(R.string.send_using));
    }

    private static void performDelete(final DirectoryFragment fragment, final List<File> selectedFiles, boolean inParent) {
        String paths = "";
        for (File fi : selectedFiles) paths += "<i>" + fi.getName() + "</i><br/>";
        Spanned message = Html.fromHtml(fragment.getString(R.string.confirm_delete).replace("{paths}", paths)
                .replace("{dest}", (inParent ? fragment.getPath() : fragment.getPath().getParentFile()).getAbsolutePath()));

        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getActivity());
        builder.setTitle(R.string.delete).setMessage(message)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        final ProgressDialog progress = Utils.showProgressDialog(fragment.getActivity(), R.string.delete, selectedFiles.size());
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                for (int i = 0; i < selectedFiles.size(); i++) {
                                    try {
                                        selectedFiles.get(i).delete();
                                    } catch (final Exception e) {
                                        e.printStackTrace();
                                        fragment.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Utils.showErrorDialog(fragment.getActivity(), e);
                                            }
                                        });
                                        break;
                                    }
                                    final int fi = i;
                                    fragment.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            progress.setProgress(fi);
                                        }
                                    });
                                }
                                fragment.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // Close the dialog
                                        progress.dismiss();
                                        if (selectedFiles.size() == 1 && selectedFiles.get(0).getAbsolutePath().equals(fragment.getPath().getAbsolutePath())) {
                                            // From the Fragment's menu, pop the fragment back stack
                                            fragment.getActivity().getFragmentManager().popBackStack();
                                        } else fragment.load();
                                    }
                                });
                            }
                        }).start();
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

    private static void selectAll(DirectoryFragment fragment) {
        int len = fragment.getListView().getCount();
        for (int i = 0; i < len; i++)
            fragment.getListView().setItemChecked(i, true);
    }

    public static void resortFragmentList(DirectoryFragment fragment) {
        List<File> items = fragment.getAdapter().getItems();
        Collections.sort(items, File.getComparator(fragment.getActivity()));
        fragment.getAdapter().notifyDataSetChanged();
    }

    private static void performZip(final DirectoryFragment fragment, final List<File> selectedFiles) {
        String prefill = null;
        if (selectedFiles.size() == 1) {
            prefill = selectedFiles.get(0).getName();
            if (!selectedFiles.get(0).isDirectory())
                prefill = prefill.substring(0, prefill.indexOf('.'));
            prefill += ".zip";
        }
        Utils.showInputDialog(fragment.getActivity(), R.string.zip, R.string.zip_hint, prefill, new Utils.InputCallback() {
            @Override
            public void onSubmit(String input) {
                if (input == null || input.trim().isEmpty())
                    input = fragment.getString(R.string.zip_hint);
                else if (!input.trim().endsWith(".zip"))
                    input = input.trim() + ".zip";
                final File zipFile = new LocalFile((LocalFile) fragment.getPath(), input.trim());
                final ProgressDialog progress;
                try {
                    progress = Utils.showProgressDialog(fragment.getActivity(), R.string.zip,
                            ZipUtils.getTotalFileCount(selectedFiles));
                } catch (Exception e) {
                    e.printStackTrace();
                    Utils.showErrorDialog(fragment.getActivity(), e);
                    return;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final File result = ZipUtils.zip(selectedFiles, zipFile, new ZipUtils.ProgressCallback() {
                                @Override
                                public void onIncrement() {
                                    fragment.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            progress.setProgress(progress.getProgress() + 1);
                                        }
                                    });
                                }
                            });
                            fragment.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    fragment.getAdapter().update(result);
                                    DirectoryCAB.resortFragmentList(fragment);
                                }
                            });
                        } catch (final Exception e) {
                            e.printStackTrace();
                            fragment.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progress.dismiss();
                                    Toast.makeText(fragment.getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                            return;
                        }
                        fragment.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                            }
                        });
                    }
                }).start();
            }
        });
    }

    private static void performUnzip(final DirectoryFragment fragment, final List<File> selectedFiles) {
        final ProgressDialog progress = Utils.showProgressDialog(fragment.getActivity(), R.string.unzip, selectedFiles.size());
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < selectedFiles.size(); i++) {
                    final int fi = i;
                    fragment.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progress.setProgress(fi);
                        }
                    });
                    try {
                        final List<File> added = ZipUtils.unzip(selectedFiles.get(i), fragment.getPath());
                        fragment.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                for (File a : added) fragment.getAdapter().update(a);
                                DirectoryCAB.resortFragmentList(fragment);
                            }
                        });
                    } catch (final Exception e) {
                        e.printStackTrace();
                        fragment.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(fragment.getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
                fragment.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.dismiss();
                    }
                });
            }
        }).start();
    }
}