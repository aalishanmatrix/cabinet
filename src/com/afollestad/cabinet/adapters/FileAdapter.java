package com.afollestad.cabinet.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.afollestad.cabinet.BitmapWorkerTask;
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
        ImageView image = (ImageView) recycled.findViewById(R.id.image);
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
            BitmapWorkerTask task = new BitmapWorkerTask(getContext(), R.dimen.file_thumbnail, image);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, item);
        } else {
            image.setImageResource(R.drawable.ic_file);
        }

        return recycled;
    }


}
