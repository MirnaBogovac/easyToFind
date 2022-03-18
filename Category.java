package com.easy.it.find.easytofind02.helper_classes;

import android.graphics.Bitmap;

/**
 * Created by ante on 1/15/16.
 */
public class Category {

    private String category_id;
    private String category_name;
    private String category_marker;
    private String super_category;
    private transient Bitmap markerIcon;

    public String getCategory_id() {
        return this.category_id;
    }

    public String getCategory_name() {
        return this.category_name;
    }

    public String getCategory_marker() {
        return this.category_marker;
    }

    public String getSuper_category() {
        return this.super_category;
    }

    public Bitmap getMarkerIcon() {
        return this.markerIcon;
    }

    public void setMarkerIcon(Bitmap icon) {
        this.markerIcon = icon;
    }
}
