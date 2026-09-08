package com.kgjr.uno;

import android.graphics.Bitmap;

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

    /**
     * Id of the project being edited, or null when this is unsaved work. Saving with an id set
     * updates that project in place; saving without one mints a new id.
     */
    public static String currentProjectId = null;

    /** Name and description of the current project, so the save screen reopens filled in. */
    public static String currentProjectName = "";
    public static String currentProjectDescription = "";

    /** When the current project was first saved, so an update doesn't reset it. */
    public static long currentProjectCreatedAt = 0L;

    /**
     * Canvas snapshot handed from the builder to the save screen. Held here rather than passed
     * as a navigation argument because a bitmap is far too big for a Bundle.
     */
    public static Bitmap pendingThumbnail = null;

    public static boolean isEditingSavedProject() {
        return currentProjectId != null && !currentProjectId.isEmpty();
    }

    /** Forgets which project is open, without touching the canvas, code or sensors. */
    public static void clearCurrentProject() {
        currentProjectId = null;
        currentProjectName = "";
        currentProjectDescription = "";
        currentProjectCreatedAt = 0L;
        releaseThumbnail();
    }

    /**
     * Drops the reference rather than recycling: the save screen may still be showing this
     * bitmap when the write finishes, and drawing a recycled bitmap throws.
     */
    public static void releaseThumbnail() {
        pendingThumbnail = null;
    }

    /** A canvas with nothing but the seeded Start node, and no sensors, counts as empty. */
    public static boolean hasWorkInProgress() {
        return canvasNodes.size() > 1 || !selectedSensors.isEmpty();
    }

    public static void startNewProject() {
        clearCanvas();
        clearSensors();
        clearCurrentProject();
    }
}