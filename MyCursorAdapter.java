package com.easy.it.find.easytofind02.helper_classes;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.easy.it.find.easytofind02.R;

/**
 * Created by ante on 2/18/16.
 */
public class MyCursorAdapter extends CursorAdapter {

    public MyCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.autocomplete_item_view, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView locationName = (TextView) view.findViewById(R.id.location_name);
        TextView address = (TextView) view.findViewById(R.id.address);

        locationName.setText(cursor.getString(cursor.getColumnIndexOrThrow("locationName")));
        address.setText(cursor.getString(cursor.getColumnIndexOrThrow("address")));
    }
}
