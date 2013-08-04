package com.afollestad.cabinet.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;
import com.afollestad.cabinet.File;
import com.afollestad.cabinet.R;
import com.afollestad.silk.adapters.SilkAdapter;

import java.util.Locale;

public class FileAdapter extends SilkAdapter<File> {

    public FileAdapter(Context context, ThumbnailClickListener thumbnailListener) {
        super(context);
        mThumbnailListener = thumbnailListener;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        showHidden = prefs.getBoolean("show_hidden_files", false);
    }

    public static interface ThumbnailClickListener {
        public void onThumbnailClicked(int index);
    }

    private final ThumbnailClickListener mThumbnailListener;

    private boolean showHidden;

    public boolean invalidateShowHidden() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean show = prefs.getBoolean("show_hidden_files", false);
        boolean different = show != showHidden;
        if (!different) return false;
        showHidden = show;
        return different;
    }

    @Override
    public void add(File toAdd) {
        if (!showHidden && toAdd.isHidden()) return;
        super.add(toAdd);
    }

    @Override
    public int getLayout(int type) {
        return R.layout.list_item_file;
    }

    private int getMimeIcon(File file, String mime) {
        if (mime == null) return R.drawable.ic_file;
        if (file.isDirectory()) {
            return R.drawable.ic_folder;
        } else if (mime.startsWith("image/")) {
            return R.drawable.ic_picture;
        } else if (mime.startsWith("video/")) {
            return R.drawable.ic_video;
        } else if (mime.startsWith("audio/")) {
            return R.drawable.ic_audio;
        } else {
            return R.drawable.ic_file;
        }
    }

    @Override
    public View onViewCreated(final int index, View recycled, File item) {
        ImageView image = (ImageView) recycled.findViewById(R.id.image);
        TextView title = (TextView) recycled.findViewById(R.id.title);
        TextView details = (TextView) recycled.findViewById(R.id.details);
        String mime = item.getMimeType();

        title.setText(item.getName());
        String detailsStr = item.getSizeString(getContext());
        if (!item.isDirectory()) {
            if (mime != null) {
                detailsStr += " — " + mime;
            } else if (item.getExtension() != null && !item.getExtension().trim().isEmpty()) {
                detailsStr += " — " + getContext().getString(R.string.manual_mime).replace("{extension}", item.getExtension().toUpperCase(Locale.getDefault()));
            }
        }
        details.setText(detailsStr);

        ViewHolder holder;
        if (recycled.getTag() != null) {
            holder = (ViewHolder) recycled.getTag();
        } else {
            holder = new ViewHolder();
            holder.thumbnail = image;
            recycled.setTag(holder);
        }

        holder.position = index;
        int mimeIcon = getMimeIcon(item, mime);
        if (mime != null && mime.startsWith("image/") && getScrollState() != AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
            new ThumbnailTask(getContext(), index, holder).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, item);
        } else {
            image.setImageResource(mimeIcon);
        }
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mThumbnailListener.onThumbnailClicked(index);
            }
        });

        return recycled;
    }

    private static class ThumbnailTask extends AsyncTask<File, Void, Bitmap> {

        private final int mDimen;
        private final int mPosition;
        private final ViewHolder mHolder;

        public ThumbnailTask(Context context, int position, ViewHolder holder) {
            mDimen = context.getResources().getDimensionPixelSize(R.dimen.file_thumbnail);
            mPosition = position;
            mHolder = holder;
        }

        @Override
        protected Bitmap doInBackground(File... params) {
            return decodeFile(params[0], mDimen, mDimen);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (mHolder.position == mPosition)
                mHolder.thumbnail.setImageBitmap(bitmap);
        }

        private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
            // Raw height and width of image
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;
            if (height > reqHeight || width > reqWidth) {
                // Calculate ratios of height and width to requested height and width
                final int heightRatio = Math.round((float) height / (float) reqHeight);
                final int widthRatio = Math.round((float) width / (float) reqWidth);
                // Choose the smallest ratio as inSampleSize value, this will guarantee
                // a final image with both dimensions larger than or equal to the
                // requested height and width.
                inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
            }
            return inSampleSize;
        }

        private Bitmap decodeFile(File file, int reqWidth, int reqHeight) {
            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            options.inPurgeable = true;
            return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        }
    }

    private static class ViewHolder {
        public ImageView thumbnail;
        public int position;
    }
}
