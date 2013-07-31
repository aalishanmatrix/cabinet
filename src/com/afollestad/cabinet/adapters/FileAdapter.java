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

    public static class FileViewHolder {
        public ImageView thumbnail;
        public int position;
    }

    public FileAdapter(Context context) {
        super(context);
        thumbnailDimen = context.getResources().getDimensionPixelSize(R.dimen.file_thumbnail);
    }

    private final int thumbnailDimen;

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
        String mime = item.getMimeType();

        title.setText(item.getName());
        String detailsStr = item.getSizeString(getContext());
        if (mime != null) detailsStr += " — " + mime;
        details.setText(detailsStr);

        FileViewHolder holder;
        if(recycled.getTag() != null) {
            holder = (FileViewHolder)recycled.getTag();
        } else {
            holder = new FileViewHolder();
            holder.thumbnail = (ImageView) recycled.findViewById(R.id.image);
            recycled.setTag(holder);
        }

        holder.position = index;
        if (item.isDirectory()) {
            holder.thumbnail.setImageResource(R.drawable.ic_folder);
        } else if (mime != null && mime.startsWith("image/")) {
            BitmapWorkerTask task = new BitmapWorkerTask(thumbnailDimen, index, holder);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, item);
        } else {
            holder.thumbnail.setImageResource(R.drawable.ic_file);
        }

        return recycled;
    }


}
