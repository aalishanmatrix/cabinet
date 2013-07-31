package com.afollestad.cabinet.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.ActionMode;
import android.widget.Toast;
import com.afollestad.cabinet.App;
import com.afollestad.cabinet.File;
import com.afollestad.cabinet.R;
import com.afollestad.cabinet.ui.MainActivity;
import com.afollestad.cabinet.utils.Clipboard;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 */
public class DirectoryCAB {

    public static boolean handleAction(DirectoryFragment fragment, int actionId, List<File> selectedFiles, ActionMode mode) {
        switch (actionId) {
            case R.id.add_shortcut:
                for (File fi : selectedFiles) ((MainActivity) fragment.getActivity()).addShortcut(fi);
                Toast.makeText(fragment.getActivity(), R.string.shorts_updated, Toast.LENGTH_SHORT).show();
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
            case R.id.delete:
                int count = 0;
                for (File fi : selectedFiles) {
                    if (fi.delete()) {
                        count++;
                        fragment.getAdapter().remove(fi);
                    }
                }
                Toast.makeText(fragment.getActivity(), fragment.getActivity().getString(R.string.x_files_deleted).replace("{X}", count + ""), Toast.LENGTH_SHORT).show();
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
        if (files.size() == 1) {
            shareIntent.setType(files.get(0).getMimeType());
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(files.get(0)));
        } else {
            ArrayList<Uri> attachments = new ArrayList<Uri>();
            for (File fi : files) attachments.add(Uri.fromFile(fi));
            shareIntent.putExtra(Intent.EXTRA_STREAM, attachments);
        }
        return Intent.createChooser(shareIntent, context.getString(R.string.send_using));
    }
}
