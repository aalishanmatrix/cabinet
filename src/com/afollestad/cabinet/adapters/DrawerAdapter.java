package com.afollestad.cabinet.adapters;

import android.content.Context;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;
import com.afollestad.cabinet.File;
import com.afollestad.cabinet.R;
import com.afollestad.silk.adapters.SilkAdapter;
import com.afollestad.silk.cache.SilkComparable;

/**
 * @author Aidan Follestad (afollestad)
 */
public class DrawerAdapter extends SilkAdapter<DrawerAdapter.DrawerItem> {

    public static class DrawerItem implements SilkComparable<DrawerItem> {

        public DrawerItem(Context context, File dir) {
            mPath = dir;
            if (dir.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath()))
                mTitle = context.getString(R.string.home);
            else mTitle = mPath.getName();
        }

        private final String mTitle;
        private final File mPath;

        public String getTitle() {
            return mTitle;
        }

        public File getFile() {
            return mPath;
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
