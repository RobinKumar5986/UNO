package com.kgjr.uno.screens.fragments.codeHelper.model;

/**
 * Framing for every payload sent to the board. Markers are stored in their escaped, typed form
 * (e.g. {@code \n}) and only turned into bytes by {@link Escapes#decode} at send time.
 */
public class StartNodeData implements NodeData {

    public static final String DEFAULT_START_MARKER = "";
    public static final String DEFAULT_END_MARKER = "\\n";

    public String startMarker = DEFAULT_START_MARKER;
    public String endMarker = DEFAULT_END_MARKER;

    public boolean isDefault() {
        return DEFAULT_START_MARKER.equals(startMarker) && DEFAULT_END_MARKER.equals(endMarker);
    }

    public void reset() {
        startMarker = DEFAULT_START_MARKER;
        endMarker = DEFAULT_END_MARKER;
    }
}
