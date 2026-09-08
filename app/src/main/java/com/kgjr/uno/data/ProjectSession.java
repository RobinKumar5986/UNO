package com.kgjr.uno.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.kgjr.uno.AppConstant;
import com.kgjr.uno.models.project.Project;
import com.kgjr.uno.models.sensors.PhoneSensor;
import com.kgjr.uno.models.sensors.SensorCatalog;
import com.kgjr.uno.screens.fragments.codeHelper.flow.FlowBlock;
import com.kgjr.uno.screens.fragments.codeHelper.flow.FlowCode;
import com.kgjr.uno.screens.fragments.codeHelper.model.CanvasNode;
import com.kgjr.uno.screens.fragments.codeHelper.model.Connection;
import com.kgjr.uno.screens.fragments.helpers.CodeModeHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Bridges the live editor session — {@link AppConstant} plus the code editor's stored sketch —
 * and a {@link Project} on disk.
 *
 * <p>Loading is eager: nodes come back with their commands, conditions and durations already
 * filled in, sensors are restored, the sketch is written back to the editor's store, and the
 * flow program is re-parsed up front, so a reopened project needs no waking up.
 */
public final class ProjectSession {

    private static final String TAG = "ProjectSession";

    private ProjectSession() {
    }

    /**
     * Snapshots the session into a project, reusing the id of the one currently open so saving
     * again updates it in place, or minting a new one for unsaved work.
     */
    public static Project capture(Context context, String name, String description) {
        Project project;

        if (AppConstant.isEditingSavedProject()) {
            project = new Project();
            project.id = AppConstant.currentProjectId;
            project.createdAt = AppConstant.currentProjectCreatedAt > 0L
                    ? AppConstant.currentProjectCreatedAt
                    : System.currentTimeMillis();
        } else {
            project = Project.create();
        }

        project.name = trim(name, Project.NAME_MAX);
        project.description = trim(description, Project.DESCRIPTION_MAX);

        project.sourceCode = CodeModeHelper.loadSource(context);
        project.generatedCode = currentProgram();

        project.nodes = ProjectMapper.toNodeDtos(AppConstant.canvasNodes);
        project.connections = ProjectMapper.toConnectionDtos(AppConstant.canvasConnections);

        project.sensorNames = new ArrayList<>();
        for (PhoneSensor sensor : AppConstant.selectedSensors) {
            if (sensor != null) project.sensorNames.add(sensor.name);
        }

        project.viewportSaved = AppConstant.canvasViewportSaved;
        project.scale = AppConstant.canvasScale;
        project.translateX = AppConstant.canvasTranslateX;
        project.translateY = AppConstant.canvasTranslateY;

        return project;
    }

    /**
     * Captures and writes the project, including the thumbnail waiting on AppConstant, then
     * marks it as the open project. Null when the write failed.
     */
    @Nullable
    public static Project save(Context context, String name, String description) {
        Project project = capture(context, name, description);

        if (!ProjectRepository.save(context, project)) {
            Log.e(TAG, "Save failed for " + project.id);
            return null;
        }
        ProjectRepository.saveThumbnail(context, project.id, AppConstant.pendingThumbnail);

        AppConstant.currentProjectId = project.id;
        AppConstant.currentProjectName = project.name;
        AppConstant.currentProjectDescription = project.description;
        AppConstant.currentProjectCreatedAt = project.createdAt;
        AppConstant.releaseThumbnail();

        return project;
    }

    /** Replaces the whole session, writing everything the editor reads when it starts up. */
    public static void apply(Context context, Project project) {
        List<CanvasNode> nodes = ProjectMapper.toNodes(project.nodes);
        List<Connection> connections = ProjectMapper.toConnections(project.connections, nodes);

        AppConstant.canvasNodes = nodes;
        AppConstant.canvasConnections = connections;

        AppConstant.canvasViewportSaved = project.viewportSaved;
        AppConstant.canvasScale = project.scale <= 0f ? 1f : project.scale;
        AppConstant.canvasTranslateX = project.translateX;
        AppConstant.canvasTranslateY = project.translateY;

        AppConstant.selectedSensors = resolveSensors(project.sensorNames);

        CodeModeHelper.saveSource(context, project.sourceCode);
        rebuildFlow(project);

        AppConstant.currentProjectId = project.id;
        AppConstant.currentProjectName = project.name;
        AppConstant.currentProjectDescription = project.description;
        AppConstant.currentProjectCreatedAt = project.createdAt;
        AppConstant.releaseThumbnail();
    }

    /** Loads by id and applies it. False when the project could not be read. */
    public static boolean load(Context context, String projectId) {
        Project project = ProjectRepository.load(context, projectId);
        if (project == null) return false;

        apply(context, project);
        return true;
    }

    /** Resets to a blank project: empty canvas, no sensors, no identity, the sample sketch. */
    public static void startNew(Context context) {
        AppConstant.startNewProject();
        CodeModeHelper.saveSource(context, CodeModeHelper.DEFAULT_SKETCH);
    }

    /**
     * Re-derives the program from the restored graph rather than trusting the stored text, so
     * the tree and the canvas can never disagree.
     */
    private static void rebuildFlow(Project project) {
        try {
            List<FlowBlock> tree = FlowCode.parse(AppConstant.canvasNodes,
                    AppConstant.canvasConnections);
            AppConstant.flowTree = tree;
            AppConstant.generatedCode = FlowCode.generate(tree);
        } catch (RuntimeException e) {
            Log.w(TAG, "Could not re-parse the saved flow; keeping the stored program", e);
            AppConstant.flowTree = new ArrayList<>();
            AppConstant.generatedCode = project.generatedCode;
        }
    }

    /**
     * The program the canvas describes right now. AppConstant is only refreshed when the user
     * presses Continue, so saving straight after an edit would otherwise store a stale program.
     */
    private static String currentProgram() {
        try {
            List<FlowBlock> tree = FlowCode.parse(AppConstant.canvasNodes,
                    AppConstant.canvasConnections);
            return FlowCode.generate(tree);
        } catch (RuntimeException e) {
            Log.w(TAG, "Canvas does not parse yet; saving without a program", e);
            return "";
        }
    }

    /** Maps stored names back to catalog entries, dropping any this build no longer offers. */
    private static List<PhoneSensor> resolveSensors(List<String> names) {
        List<PhoneSensor> sensors = new ArrayList<>();
        if (names == null) return sensors;

        for (String name : names) {
            PhoneSensor sensor = SensorCatalog.byName(name);
            if (sensor == null) Log.w(TAG, "Unknown sensor in saved project: " + name);
            else if (!sensors.contains(sensor)) sensors.add(sensor);
        }
        return sensors;
    }

    private static String trim(String value, int max) {
        if (value == null) return "";

        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
