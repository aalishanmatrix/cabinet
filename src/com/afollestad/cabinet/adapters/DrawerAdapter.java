package com.afollestad.cabinet.adapters;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import com.afollestad.cabinet.R;
import com.afollestad.cabinet.file.File;
import com.afollestad.silk.adapters.SilkAdapter;
import com.afollestad.silk.cache.SilkComparable;

/**
 * Used for the navigation drawer items.
 *
 * @author Aidan Follestad (afollestad)
 */
public class DrawerAdapter extends SilkAdapter<DrawerAdapter.DrawerItem> {

    public static class DrawerItem implements SilkComparable<DrawerItem> {

        public DrawerItem(Context context, File dir, boolean removeable) {
            mPath = dir;
            if (dir.isStorageDirectory())
                mTitle = context.getString(R.string.sdcard);
            else if (dir.isRootDirectory())
                mTitle = context.getString(R.string.root);
            else mTitle = mPath.getName();
            mRemoveable = removeable;
        }

        private final String mTitle;
        private final File mPath;
        private final boolean mRemoveable;

        public String getTitle() {
            return mTitle;
        }

        public File getFile() {
            return mPath;
        }

        public boolean isRemoveable() {
            return mRemoveable;
        }

        @Override
        public boolean isSameAs(DrawerItem another) {
            return getTitle().equals(another.getTitle());
        }

        @Override
        public boolean shouldIgnore() {
            return false;
        }
    }


    public DrawerAdapter(Context context) {
        super(context);
    }

    @Override
    public int getLayout(int type) {
        return R.layout.list_item_drawer;
    }

    @Override
    public View onViewCreated(int index, View recycled, DrawerItem item) {
        ((TextView) recycled.findViewById(R.id.title)).setText(item.getTitle());
        return recycled;
    }
}
