package com.afollestad.cabinet.adapters;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import com.afollestad.cabinet.App;
import com.afollestad.cabinet.File;
import com.afollestad.cabinet.R;
import com.afollestad.silk.adapters.SilkAdapter;
import com.afollestad.silk.images.SilkImageView;

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
        SilkImageView image = (SilkImageView) recycled.findViewById(R.id.image);
        TextView title = (TextView) recycled.findViewById(R.id.title);
        TextView details = (TextView) recycled.findViewById(R.id.details);
        String mime = item.getMimeType();

        title.setText(item.getName());
        String detailsStr = item.getSizeString(getContext());
        if (mime != null) detailsStr += " — " + mime;
        details.setText(detailsStr);

        if (item.isDirectory()) {
            image.setImageResource(R.drawable.ic_folder);
        } else if (mime != null && mime.startsWith("image/")) {
            image.setImageManager(App.get(getContext()).getDrawableManager());
            image.setImageURL(item.getAbsolutePath());
            image.load();
        } else {
            image.setImageResource(R.drawable.ic_file);
        }

        return recycled;
    }


}
