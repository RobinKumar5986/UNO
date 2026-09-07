package com.kgjr.uno.screens.fragments.sensorHelper;

import android.content.Context;

import androidx.activity.result.ActivityResultLauncher;

import com.kgjr.uno.models.sensors.PhoneSensor;

import java.util.ArrayList;
import java.util.List;

/**
 * The runtime permissions a set of sensors needs, gathered from the catalog rather than hardcoded.
 *
 * <p>Most sensors need none — the heart rate monitor is the only guarded one today, and location
 * or the microphone would slot in the same way by declaring
 * {@link PhoneSensor.Builder#permission(String)}. Screens ask here instead of naming permissions
 * themselves, so adding a guarded sensor to the catalog is all it takes.
 */
public final class SensorPermissions {

    private SensorPermissions() {
    }

    /** Permissions these sensors need that the user hasn't granted yet, without duplicates. */
    public static String[] missingFor(Context context, List<PhoneSensor> sensors) {
        List<String> missing = new ArrayList<>();
        if (sensors == null) return new String[0];

        for (PhoneSensor sensor : sensors) {
            if (sensor == null || !sensor.needsPermission()) continue;
            if (sensor.hasPermission(context)) continue;
            if (!missing.contains(sensor.permission)) missing.add(sensor.permission);
        }
        return missing.toArray(new String[0]);
    }

    /**
     * Asks for whatever these sensors still need. Does nothing when nothing is missing, so it is
     * safe to call on every resume.
     *
     * @return true when a request was actually launched
     */
    public static boolean request(ActivityResultLauncher<String[]> launcher, Context context,
                                  List<PhoneSensor> sensors) {
        if (launcher == null) return false;

        String[] missing = missingFor(context, sensors);
        if (missing.length == 0) return false;

        launcher.launch(missing);
        return true;
    }

    /** The single-sensor case, for the moment a card is tapped. */
    public static boolean request(ActivityResultLauncher<String[]> launcher, Context context,
                                  PhoneSensor sensor) {
        List<PhoneSensor> one = new ArrayList<>();
        one.add(sensor);
        return request(launcher, context, one);
    }
}
