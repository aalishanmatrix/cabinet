package com.afollestad.cabinet.fragments.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.afollestad.cabinet.R;
import com.afollestad.cabinet.file.File;
import com.afollestad.cabinet.file.RemoteFile;

/**
 * @author Aidan Follestad (afollestad)
 */
public class AddRemoteDialog extends DialogFragment {

    public final OnaAddedListener mAddedListener;

    public AddRemoteDialog(OnaAddedListener listener) {
        mAddedListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View rootView = layoutInflater.inflate(R.layout.dialog_add_remote, null);
        final TextView host = (TextView) rootView.findViewById(R.id.host);
        final TextView port = (TextView) rootView.findViewById(R.id.port);
        final TextView user = (TextView) rootView.findViewById(R.id.user);
        final TextView pass = (TextView) rootView.findViewById(R.id.pass);
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.add_remote)
                .setView(rootView)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                        if (mAddedListener != null) {
                            mAddedListener.onAdded(
                                    new RemoteFile(getActivity(), host.getText().toString().trim(), port.getText().toString().trim(),
                                            user.getText().toString().trim(), pass.getText().toString().trim(), "/"));
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
    }

    public interface OnaAddedListener {
        public abstract void onAdded(File file);
    }
}