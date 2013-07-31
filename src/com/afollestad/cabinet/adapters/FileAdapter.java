package com.afollestad.cabinet.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;
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
        ImageView image = (ImageView) recycled.findViewById(R.id.image);
        TextView title = (TextView) recycled.findViewById(R.id.title);
        TextView details = (TextView) recycled.findViewById(R.id.details);
        String mime = item.getMimeType();

        title.setText(item.getName());
        String detailsStr = item.getSizeString(getContext());
        if (mime != null) {
            detailsStr += " — " + mime;
        } else if (!item.isDirectory()) {
            detailsStr += " — " + getContext().getString(R.string.manual_mime).replace("{extension}", item.getExtension());
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
        if (item.isDirectory())
            image.setImageResource(R.drawable.ic_folder);
        else if (mime != null && mime.startsWith("image/"))
            new ThumbnailTask(getContext(), index, holder).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, item);
        else
            image.setImageResource(R.drawable.ic_file);
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
