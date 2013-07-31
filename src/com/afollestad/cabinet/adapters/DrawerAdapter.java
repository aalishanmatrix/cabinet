package com.afollestad.cabinet.adapters;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import com.afollestad.cabinet.R;
import com.afollestad.silk.adapters.SilkAdapter;
import com.afollestad.silk.cache.SilkComparable;

/**
 * @author Aidan Follestad (afollestad)
 */
public class DrawerAdapter extends SilkAdapter<DrawerAdapter.DrawerItem> {

    public static class DrawerItem implements SilkComparable<DrawerItem> {

        public DrawerItem(String title) {
            mTitle = title;
        }

        private String mTitle;

        public String getTitle() {
            return mTitle;
        }

        @Override
        public boolean isSameAs(DrawerItem another) {
            return mTitle.equals(another.mTitle);
        }

        @Override
        public boolean shouldIgnore() {
            return mTitle.trim().isEmpty();
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
