package com.afollestad.cabinet;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import com.afollestad.cabinet.adapters.FileAdapter;

/**
 * @author Aidan Follestad (afollestad)
 */
public class BitmapWorkerTask extends AsyncTask<File, Void, Bitmap> {

    private final int mPosition;
    private final FileAdapter.FileViewHolder mHolder;
    private final int mDimen;

    public BitmapWorkerTask(int thumbnailDimen, int position, FileAdapter.FileViewHolder holder) {
        mPosition = position;
        mHolder = holder;
        mDimen = thumbnailDimen;
    }

    @Override
    protected Bitmap doInBackground(File... params) {
        return decodeSampledBitmap(params[0], mDimen, mDimen);
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (mHolder.position == mPosition) {
            Log.d("BitmapWorkerTask", "Position unchanged, setting!");
            mHolder.thumbnail.setImageBitmap(bitmap);
        } else {
            Log.d("BitmapWorkerTask", "Position changed, cancelling...");
        }
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

    private Bitmap decodeSampledBitmap(File file, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }
}
