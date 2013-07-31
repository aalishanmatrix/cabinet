package com.afollestad.cabinet.cab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.ActionMode;
import android.widget.Toast;
import com.afollestad.cabinet.App;
import com.afollestad.cabinet.File;
import com.afollestad.cabinet.R;
import com.afollestad.cabinet.fragments.DirectoryFragment;
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

    private static void performDelete(final DirectoryFragment fragment, final List<File> selectedFiles) {
        String paths = "";
        for (File fi : selectedFiles) paths += fi.getName() + "\n";
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getActivity());
        builder.setTitle(R.string.delete)
                .setMessage(fragment.getActivity().getString(R.string.confirm_delete).replace("{paths}", paths))
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        for (File fi : selectedFiles) {
                            if (fi.delete()) fragment.getAdapter().remove(fi);
                        }
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
}
