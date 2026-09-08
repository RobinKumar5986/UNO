package com.kgjr.uno.models.project;

/**
 * A CanvasNode flattened for storage. Every node data field is spelled out here and the mapper
 * reads back only the ones the node's type uses, which avoids a polymorphic Gson adapter.
 */
public class NodeDto {

    public String id;

    /** NodeType.name(), e.g. "DECISION". */
    public String type;

    public float x;
    public float y;
    public float width;
    public float height;

    public String actionMode;
    public String actionCommand;
    public String actionSensorName;

    public String decisionSensorName;
    public String conditionSensorName;
    public String conditionChannelKey;
    public String conditionOperator;
    public String conditionValue;

    public int waitHours;
    public int waitMinutes;
    public int waitSeconds;
    public int waitMillis;

    public int repeatTimes;

    public boolean endLoop;
}
