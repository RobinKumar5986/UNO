package com.kgjr.uno.screens.fragments.codeHelper.model;

public class ActionNodeData implements NodeData  {

    public enum Mode {
        COMMAND("Command"),
        SENSOR("Sensor"),
        API("API");

        public final String label;

        Mode(String label) {
            this.label = label;
        }

        public static Mode fromLabel(String label) {
            for (Mode mode : values()) {
                if (mode.label.equals(label)) return mode;
            }
            return COMMAND;
        }

        /** Modes whose command text is written out and sent to the board. */
        public boolean sendsCommand() {
            return this != API;
        }
    }

    public Mode mode = Mode.COMMAND;
    public String command = "";

    /** Name of the sensor last picked in Sensor mode, so the dialog reopens on it. */
    public String sensorName = "";

    /** What the node draws under its label on the canvas. */
    public String summary() {
        if (mode == Mode.API) return "API — not configured";
        return command.trim().isEmpty() ? "No command" : command.trim();
    }
}
