package com.kgjr.uno.models.project;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One saved project: everything needed to put the editor back as the user left it. The id is
 * generated when the project is first saved and never changes, so saving again overwrites it.
 */
public class Project {

    public static final int NAME_MAX = 15;
    public static final int DESCRIPTION_MAX = 100;

    /** Bumped when the on-disk shape changes, so old files can be migrated or skipped. */
    public static final int CURRENT_VERSION = 1;

    public int version = CURRENT_VERSION;

    public String id;
    public String name = "";
    public String description = "";

    public long createdAt;
    public long updatedAt;

    /** The C++ sketch from the code editor. */
    public String sourceCode = "";

    /** The flow program the canvas generated, e.g. BEGIN / ACTION(...) / END. */
    public String generatedCode = "";

    /** Stable sensor names, resolved back through SensorCatalog on load. */
    public List<String> sensorNames = new ArrayList<>();

    public List<NodeDto> nodes = new ArrayList<>();
    public List<ConnectionDto> connections = new ArrayList<>();

    public boolean viewportSaved;
    public float scale = 1f;
    public float translateX;
    public float translateY;

    public static Project create() {
        Project project = new Project();
        project.id = UUID.randomUUID().toString();
        project.createdAt = System.currentTimeMillis();
        project.updatedAt = project.createdAt;
        return project;
    }

    public int nodeCount() {
        return nodes == null ? 0 : nodes.size();
    }

    public int connectionCount() {
        return connections == null ? 0 : connections.size();
    }

    public int sensorCount() {
        return sensorNames == null ? 0 : sensorNames.size();
    }

    public int sourceLineCount() {
        if (sourceCode == null || sourceCode.isEmpty()) return 0;
        return sourceCode.split("\n", -1).length;
    }
}
