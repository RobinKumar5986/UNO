package com.kgjr.uno.screens.fragments.helpers;

/** Marks a collapsed region in the editor and remembers the hidden text. */
public final class FoldSpan {
    public final String originalText;
    public FoldSpan(String originalText) {
        this.originalText = originalText;
    }
}