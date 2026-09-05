package com.kgjr.uno.screens.fragments.codeHelper.model;

/**
 * DECISION node: one sensor comparison. Combining tests is done by nesting Decision blocks on
 * the canvas rather than stacking conditions here, so a node stays a single readable question.
 *
 * <p>The condition renders as {@code [sensor: channel] op value}, the same token spelling the
 * Action dialog writes, so the runner can resolve it against a live reading.
 */
public class DecisionNodeData implements NodeData {

    public static final String[] OPERATORS = {"<", ">", "==", "!="};

    /**
     * Always present so the dialog's operator buttons have something to write to before a
     * channel is picked. {@link #isSet()} is what says whether the user has filled it in.
     */
    public static class Condition {

        /** Stable sensor name, e.g. "orientation". Empty until a channel is picked. */
        public String sensorName = "";

        /** Channel wire name, e.g. "azimuth". Empty until a channel is picked. */
        public String channelKey = "";

        public String operator = OPERATORS[1];
        public String value = "";

        /** Points the condition at a new channel, dropping a value measured in the old unit. */
        public void pointAt(String sensorName, String channelKey) {
            this.sensorName = sensorName;
            this.channelKey = channelKey;
            this.value = "";
        }

        public boolean isSet() {
            return channelKey != null && !channelKey.isEmpty();
        }

        public String token() {
            return "[" + sensorName + ": " + channelKey + "]";
        }

        public String expression() {
            return token() + " " + operator + " " + (hasValue() ? value.trim() : "0");
        }

        public boolean hasValue() {
            if (value == null || value.trim().isEmpty()) return false;
            try {
                Float.parseFloat(value.trim());
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    /** Sensor last picked in the dialog, so it reopens on the same one. */
    public String sensorName = "";

    public final Condition condition = new Condition();

    /** e.g. {@code [orientation: azimuth] > 90}, or empty when no channel is picked yet. */
    public String expression() {
        return condition.isSet() ? condition.expression() : "";
    }
}
