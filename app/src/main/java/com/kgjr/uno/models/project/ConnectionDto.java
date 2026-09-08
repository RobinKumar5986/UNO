package com.kgjr.uno.models.project;

/**
 * A Connection flattened for storage. The live model holds object references to nodes and to the
 * shared ConnectionPoint singletons; both are stored as ids and re-resolved on load so the
 * restored graph keeps the identity comparisons the canvas and parser rely on.
 */
public class ConnectionDto {

    public String fromNodeId;
    public String fromPointId;
    public String toNodeId;
    public String toPointId;
}
