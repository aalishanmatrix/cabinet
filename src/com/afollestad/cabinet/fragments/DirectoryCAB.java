package com.afollestad.cabinet.fragments;

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
                break;
            case R.id.cut:
                App.get(fragment.getActivity()).getClipboard().clear();
                App.get(fragment.getActivity()).getClipboard().setType(Clipboard.Type.CUT);
                for (File fi : selectedFiles) App.get(fragment.getActivity()).getClipboard().add(fi);
                break;
            case R.id.paste:
                startPaste(fragment);
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

    private static void startPaste(final DirectoryFragment fragment) {
        Activity context = fragment.getActivity();
        final Clipboard cb = App.get(context).getClipboard();
        String paths = "";
        for (File fi : cb.get()) paths += fi.getAbsolutePath() + "\n";
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
        builder.setTitle(R.string.paste).setMessage(message)
                .setPositiveButton(action, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        cb.performPaste(fragment.getPath());
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
