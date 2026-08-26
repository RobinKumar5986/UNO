package com.kgjr.uno.models;

public class ActionItem {
    private final String title;
    private final int iconResId;
    private final int tintColorResId;

    public ActionItem(String title, int iconResId, int tintColorResId) {
        this.title = title;
        this.iconResId = iconResId;
        this.tintColorResId = tintColorResId;
    }

    public String getTitle() {
        return title;
    }

    public int getIconResId() {
        return iconResId;
    }

    public int getTintColorResId() {
        return tintColorResId;
    }
}