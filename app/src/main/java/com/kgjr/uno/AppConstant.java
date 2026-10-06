package com.kgjr.uno;

import android.graphics.Bitmap;

import com.kgjr.uno.models.sensors.PhoneSensor;
import com.kgjr.uno.screens.fragments.codeHelper.flow.FlowBlock;
import com.kgjr.uno.screens.fragments.codeHelper.model.FlowDocument;
import com.kgjr.uno.screens.fragments.codeHelper.model.TriggerFlow;
import com.kgjr.uno.screens.fragments.codeHelper.trigger.TriggerProgram;

import java.util.ArrayList;
import java.util.List;

public class AppConstant {

    /** The validated program, e.g. BEGIN / ACTION(Command : ls) / END. */
    public static String generatedCode = "";

    /** The same program as blocks. */
    public static List<FlowBlock> flowTree = new ArrayList<>();

    /** The Stage 1 flow canvas, kept so the builder survives navigating away and back. */
    public static FlowDocument mainFlow = new FlowDocument();

    /** Stage 2: flows fired by data from the board, in the order the user added them. */
    public static List<TriggerFlow> triggers = new ArrayList<>();

    /** The triggers validated by the last Next press, for the execution screen. */
    public static List<TriggerProgram> triggerPrograms = new ArrayList<>();

    public static void clearCanvas() {
        mainFlow = new FlowDocument();
        triggers = new ArrayList<>();
        triggerPrograms = new ArrayList<>();
        generatedCode = "";
        flowTree = new ArrayList<>();
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
        return !mainFlow.isEmpty() || !triggers.isEmpty() || !selectedSensors.isEmpty();
    }

    public static void startNewProject() {
        clearCanvas();
        clearSensors();
        clearCurrentProject();
    }
}