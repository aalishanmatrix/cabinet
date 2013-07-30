package com.afollestad.cabinet.adapters;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import com.afollestad.cabinet.File;
import com.afollestad.cabinet.R;
import com.afollestad.silk.adapters.SilkAdapter;

public class FileAdapter extends SilkAdapter<File> {

    public FileAdapter(Context context) {
        super(context);
    }

    @Override
    public void add(File toAdd) {
        if (toAdd.isHidden()) return;
        super.add(toAdd);
    }

    @Override
    public int getLayout(int type) {
        return R.layout.list_item_file;
    }

    @Override
    public View onViewCreated(int index, View recycled, File item) {
        TextView title = (TextView) recycled.findViewById(R.id.title);
        TextView details = (TextView) recycled.findViewById(R.id.details);
        title.setText(item.getName());
        String detailsStr = item.getSizeString(getContext());
        if (item.getMimeType() != null) {
            detailsStr += " — " + item.getMimeType();
        }
        details.setText(detailsStr);
        return recycled;
    }


}
