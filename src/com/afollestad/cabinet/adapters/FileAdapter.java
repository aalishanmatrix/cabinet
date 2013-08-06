package com.afollestad.cabinet.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;
import com.afollestad.cabinet.File;
import com.afollestad.cabinet.R;
import com.afollestad.silk.Silk;
import com.afollestad.silk.adapters.SilkAdapter;

/**
 * The adapter for {@link File} objects, used by the {@link com.afollestad.cabinet.fragments.DirectoryFragment}.
 *
 * @author Aidan Follestad
 */
public class FileAdapter extends SilkAdapter<File> {

    public FileAdapter(Context context, ThumbnailClickListener thumbnailListener) {
        super(context);
        mThumbnailListener = thumbnailListener;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        showHidden = prefs.getBoolean("show_hidden_files", false);
        isTablet = Silk.isTablet(context);
    }

    public static interface ThumbnailClickListener {
        public void onThumbnailClicked(int index);
    }

    private final ThumbnailClickListener mThumbnailListener;

    private boolean showHidden;
    private int sortSetting;
    private final boolean isTablet;

    public boolean invalidate() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean show = prefs.getBoolean("show_hidden_files", false);
        int sort = Integer.parseInt(prefs.getString("file_sorting", "0"));
        boolean different = sort != sortSetting || show != showHidden;
        showHidden = show;
        sortSetting = sort;
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
        if (file.isDirectory()) {
            return R.drawable.ic_folder;
        } else if (mime == null) {
            return R.drawable.ic_file;
        } else if (mime.startsWith("image/")) {
            return R.drawable.ic_picture;
        } else if (mime.startsWith("video/")) {
            return R.drawable.ic_video;
        } else if (mime.startsWith("audio/") || mime.equals("application/ogg")) {
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
                detailsStr += " — " + getContext().getString(R.string.manual_mime).replace("{extension}", item.getExtension());
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
        if (mime != null && (mime.startsWith("image/") || mime.equals("application/vnd.android.package-archive")) &&
                getScrollState() != AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
            new ThumbnailTask(getContext(), index, holder).execute(item);
        } else {
            image.setImageResource(mimeIcon);
        }

        if (!isTablet) {
            image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mThumbnailListener.onThumbnailClicked(index);
                }
            });
        }

        return recycled;
    }

    private static class ThumbnailTask extends AsyncTask<File, Void, Bitmap> {

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

    private static class ViewHolder {
        public ImageView thumbnail;
        public int position;
    }
}
