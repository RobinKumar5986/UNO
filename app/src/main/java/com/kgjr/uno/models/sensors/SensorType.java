package com.kgjr.uno.models.sensors;

/** Which way data flows through a sensor, seen from the program's side. */
public enum SensorType {

    /** Reports readings into the program — orientation, accelerometer, camera. */
    INPUT("Input"),

    /** Consumes values from the program — screen, speaker, vibration motor. */
    OUTPUT("Output");

    public final String label;

    SensorType(String label) {
        this.label = label;
    }

    public static SensorType fromLabel(String label) {
        for (SensorType type : values()) {
            if (type.label.equalsIgnoreCase(label)) return type;
        }
        return INPUT;
    }
}
