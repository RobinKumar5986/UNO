package com.kgjr.uno.screens.fragments.codeHelper.model;

/**
 * Start node of a trigger flow: the shape of the message from the board that fires it, and how
 * the trigger's own commands are framed on the way back. Markers use the same escaped form as
 * {@link StartNodeData}; the payload is a printf-style pattern such as {@code %d, %f, %s}.
 */
public class TriggerStartData implements NodeData {

    public static final String DEFAULT_START_MARKER = "";
    public static final String DEFAULT_END_MARKER = "\\n";
    public static final String DEFAULT_PAYLOAD = "%d";

    public String startMarker = DEFAULT_START_MARKER;
    public String endMarker = DEFAULT_END_MARKER;
    public String payload = DEFAULT_PAYLOAD;

    public String sendStartMarker = StartNodeData.DEFAULT_START_MARKER;
    public String sendEndMarker = StartNodeData.DEFAULT_END_MARKER;
}
