package com.afollestad.cabinet.adapters;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.widget.ImageView;
import com.afollestad.cabinet.R;
import com.afollestad.cabinet.file.File;

/**
 * Used by the {@link FileAdapter} to load and display file thumbnails.
 *
 * @author Aidan Follestad (afollestad)
 */
public class ThumbnailTask extends AsyncTask<File, Void, Bitmap> {

    public static class ViewHolder {
        public ImageView thumbnail;
        public int position;
    }

    private final Context mContext;
    private final int mDimen;
    private final int mPosition;
    private final ViewHolder mHolder;

    public ThumbnailTask(Context context, int position, ViewHolder holder) {
        mContext = context;
        mDimen = context.getResources().getDimensionPixelSize(R.dimen.file_thumbnail);
        mPosition = position;
        mHolder = holder;
    }

    @Override
    protected Bitmap doInBackground(File... params) {
        if (params.length == 0 || params[0] == null) return null;
        if (params[0].getMimeType().equals("application/vnd.android.package-archive")) {
            PackageManager pm = mContext.getPackageManager();
            PackageInfo pi = pm.getPackageArchiveInfo(params[0].getAbsolutePath(), 0);
            pi.applicationInfo.sourceDir = params[0].getAbsolutePath();
            pi.applicationInfo.publicSourceDir = params[0].getAbsolutePath();
            return ((BitmapDrawable) pi.applicationInfo.loadIcon(pm)).getBitmap();
        }
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
