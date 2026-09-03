package com.kgjr.uno;

import com.kgjr.uno.models.sensors.PhoneSensor;
import com.kgjr.uno.screens.fragments.codeHelper.flow.FlowBlock;
import com.kgjr.uno.screens.fragments.codeHelper.model.CanvasNode;
import com.kgjr.uno.screens.fragments.codeHelper.model.Connection;

import java.util.ArrayList;
import java.util.List;

public class AppConstant {

    /** The validated program, e.g. BEGIN / ACTION(Command : ls) / END. */
    public static String generatedCode = "";

    /** The same program as blocks. */
    public static List<FlowBlock> flowTree = new ArrayList<>();

    /** Canvas contents, kept so the builder survives navigating away and back. */
    public static List<CanvasNode> canvasNodes = new ArrayList<>();
    public static List<Connection> canvasConnections = new ArrayList<>();

    /** Canvas viewport (pinch zoom + pan), kept so the view isn't reset on navigating back. */
    public static boolean canvasViewportSaved = false;
    public static float canvasScale = 1f;
    public static float canvasTranslateX = 0f;
    public static float canvasTranslateY = 0f;

    public static void clearCanvas() {
        canvasNodes = new ArrayList<>();
        canvasConnections = new ArrayList<>();
        generatedCode = "";
        flowTree = new ArrayList<>();
        clearCanvasViewport();
    }

    /** Drops the saved zoom/pan so the canvas opens at 1x, centred. */
    public static void clearCanvasViewport() {
        canvasViewportSaved = false;
        canvasScale = 1f;
        canvasTranslateX = 0f;
        canvasTranslateY = 0f;
    }

    /**
     * Sensors the user picked on the sensors screen, in the order they were picked. Kept here so
     * the selection survives navigating away and back, and so later screens can read it.
     */
    public static List<PhoneSensor> selectedSensors = new ArrayList<>();

    public static boolean isSensorSelected(PhoneSensor sensor) {
        return sensor != null && selectedSensors.contains(sensor);
    }

    /** Adds or removes the sensor. Returns true when it ends up selected. */
    public static boolean toggleSensor(PhoneSensor sensor) {
        if (sensor == null) return false;

        if (selectedSensors.remove(sensor)) return false;
        selectedSensors.add(sensor);
        return true;
    }

    public static void deselectSensor(PhoneSensor sensor) {
        selectedSensors.remove(sensor);
    }

    public static void clearSensors() {
        selectedSensors = new ArrayList<>();
    }
}