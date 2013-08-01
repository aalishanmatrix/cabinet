package com.afollestad.cabinet.cab;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.text.Spanned;
import android.view.ActionMode;
import android.view.Gravity;
import com.afollestad.cabinet.App;
import com.afollestad.cabinet.File;
import com.afollestad.cabinet.R;
import com.afollestad.cabinet.fragments.DirectoryFragment;
import com.afollestad.cabinet.ui.MainActivity;
import com.afollestad.cabinet.utils.Clipboard;
import com.afollestad.cabinet.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
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
                fragment.getActivity().invalidateOptionsMenu();
                break;
            case R.id.cut:
                App.get(fragment.getActivity()).getClipboard().clear();
                App.get(fragment.getActivity()).getClipboard().setType(Clipboard.Type.CUT);
                for (File fi : selectedFiles) App.get(fragment.getActivity()).getClipboard().add(fi);
                fragment.getActivity().invalidateOptionsMenu();
                break;
            case R.id.select_all:
                selectAll(fragment);
                return true;
            case R.id.delete:
                performDelete(fragment, selectedFiles);
                break;
            default:
                return false;
        }
        mode.finish();
        return true;
    }

    private static Intent getShareIntent(Activity context, List<File> files) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType(files.get(0).getMimeType()); //TODO multiple mime types?
        if (files.size() == 1) {
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(files.get(0)));
        } else {
            ArrayList<Uri> attachments = new ArrayList<Uri>();
            for (File fi : files) attachments.add(Uri.fromFile(fi));
            shareIntent.putExtra(Intent.EXTRA_STREAM, attachments);
        }
        return Intent.createChooser(shareIntent, context.getString(R.string.send_using));
    }

    public static void performDelete(final DirectoryFragment fragment, final List<File> selectedFiles) {
        String paths = "";
        for (File fi : selectedFiles) paths += "<i>" + fi.getName() + "</i><br/>";
        Spanned message = Html.fromHtml(fragment.getString(R.string.confirm_delete).replace("{paths}", paths).replace("{dest}", fragment.getPath().getAbsolutePath()));

        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getActivity());
        builder.setTitle(R.string.delete).setMessage(message)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        final ProgressDialog progress = Utils.showProgressDialog(fragment.getActivity(), selectedFiles.size());
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                for (int i = 0; i < selectedFiles.size(); i++) {
                                    selectedFiles.get(i).delete();
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
                                        // Remove the deleted files from the adapter
                                        fragment.getAdapter().remove(selectedFiles.toArray(new File[selectedFiles.size()]));
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
}