package com.kgjr.uno.screens.fragments.codeHelper.model;

public class ActionNodeData implements NodeData  {

    public enum Mode {
        COMMAND("Command"),
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
    }

    public Mode mode = Mode.COMMAND;
    public String command = "";

    /** What the node draws under its label on the canvas. */
    public String summary() {
        if (mode == Mode.API) return "API — not configured";
        return command.trim().isEmpty() ? "No command" : command.trim();
    }
}