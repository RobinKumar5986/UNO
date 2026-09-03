package com.kgjr.uno.models.sensors;

import android.hardware.SensorManager;

/**
 * One way a {@link PhoneSensor} can be satisfied by the hardware: every Android sensor type
 * listed here has to be present on the phone.
 *
 * <p>A sensor may declare several requirements, in which case any one of them is enough. The
 * orientation sensor is the reason this exists — it can be derived from a rotation vector, or,
 * on phones without one, from an accelerometer plus a magnetometer.
 */
public class HardwareRequirement {

    /** Android {@code Sensor.TYPE_*} constants that must all be present. */
    public final int[] sensorTypes;

    private HardwareRequirement(int[] sensorTypes) {
        this.sensorTypes = sensorTypes;
    }

    public static HardwareRequirement of(int... sensorTypes) {
        return new HardwareRequirement(sensorTypes);
    }

    public boolean isSatisfiedBy(SensorManager manager) {
        if (manager == null || sensorTypes.length == 0) return false;

        for (int type : sensorTypes) {
            if (manager.getDefaultSensor(type) == null) return false;
        }
        return true;
    }
}
