package com.kgjr.uno.data;

import com.kgjr.uno.models.project.ConnectionDto;
import com.kgjr.uno.models.project.NodeDto;
import com.kgjr.uno.screens.fragments.codeHelper.model.ActionNodeData;
import com.kgjr.uno.screens.fragments.codeHelper.model.CanvasNode;
import com.kgjr.uno.screens.fragments.codeHelper.model.Connection;
import com.kgjr.uno.screens.fragments.codeHelper.model.ConnectionPoint;
import com.kgjr.uno.screens.fragments.codeHelper.model.DecisionNodeData;
import com.kgjr.uno.screens.fragments.codeHelper.model.EndNodeData;
import com.kgjr.uno.screens.fragments.codeHelper.model.NodeData;
import com.kgjr.uno.screens.fragments.codeHelper.model.NodeType;
import com.kgjr.uno.screens.fragments.codeHelper.model.RepeatNodeData;
import com.kgjr.uno.screens.fragments.codeHelper.model.WaitNodeData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts the canvas graph to and from its stored form. A node's configuration is written out
 * with the node rather than left to be filled in when a dialog is next opened, so a loaded
 * project is fully configured the moment it appears on screen.
 */
public final class ProjectMapper {

    private ProjectMapper() {
    }

    public static List<NodeDto> toNodeDtos(List<CanvasNode> nodes) {
        List<NodeDto> dtos = new ArrayList<>();
        if (nodes == null) return dtos;

        for (CanvasNode node : nodes) {
            if (node != null) dtos.add(toDto(node));
        }
        return dtos;
    }

    public static NodeDto toDto(CanvasNode node) {
        NodeDto dto = new NodeDto();
        dto.id = node.id;
        dto.type = node.type.name();
        dto.x = node.x;
        dto.y = node.y;
        dto.width = node.width;
        dto.height = node.height;

        if (node.data instanceof ActionNodeData) {
            ActionNodeData data = (ActionNodeData) node.data;
            dto.actionMode = data.mode == null ? ActionNodeData.Mode.COMMAND.name() : data.mode.name();
            dto.actionCommand = data.command;
            dto.actionSensorName = data.sensorName;

        } else if (node.data instanceof DecisionNodeData) {
            DecisionNodeData data = (DecisionNodeData) node.data;
            dto.decisionSensorName = data.sensorName;
            dto.conditionSensorName = data.condition.sensorName;
            dto.conditionChannelKey = data.condition.channelKey;
            dto.conditionOperator = data.condition.operator;
            dto.conditionValue = data.condition.value;

        } else if (node.data instanceof WaitNodeData) {
            WaitNodeData data = (WaitNodeData) node.data;
            dto.waitHours = data.hours;
            dto.waitMinutes = data.minutes;
            dto.waitSeconds = data.seconds;
            dto.waitMillis = data.millis;

        } else if (node.data instanceof RepeatNodeData) {
            dto.repeatTimes = ((RepeatNodeData) node.data).times;

        } else if (node.data instanceof EndNodeData) {
            dto.endLoop = ((EndNodeData) node.data).loop;
        }
        return dto;
    }

    public static List<ConnectionDto> toConnectionDtos(List<Connection> connections) {
        List<ConnectionDto> dtos = new ArrayList<>();
        if (connections == null) return dtos;

        for (Connection connection : connections) {
            if (connection == null) continue;

            ConnectionDto dto = new ConnectionDto();
            dto.fromNodeId = connection.from.id;
            dto.fromPointId = connection.fromPoint.id;
            dto.toNodeId = connection.to.id;
            dto.toPointId = connection.toPoint.id;
            dtos.add(dto);
        }
        return dtos;
    }

    public static List<CanvasNode> toNodes(List<NodeDto> dtos) {
        List<CanvasNode> nodes = new ArrayList<>();
        if (dtos == null) return nodes;

        for (NodeDto dto : dtos) {
            if (dto != null) nodes.add(toNode(dto));
        }
        return nodes;
    }

    public static CanvasNode toNode(NodeDto dto) {
        NodeType type = NodeType.fromName(dto.type);

        NodeData data = CanvasNode.createDefaultData(type);
        if (data == null) data = CanvasNode.createDefaultData(NodeType.ACTION);

        applyData(data, dto);
        return new CanvasNode(dto.id, type, dto.x, dto.y, dto.width, dto.height, data);
    }

    private static void applyData(NodeData data, NodeDto dto) {
        if (data instanceof ActionNodeData) {
            ActionNodeData action = (ActionNodeData) data;
            action.mode = ActionNodeData.Mode.valueOf(
                    safeMode(dto.actionMode, ActionNodeData.Mode.COMMAND.name()));
            action.command = text(dto.actionCommand);
            action.sensorName = text(dto.actionSensorName);

        } else if (data instanceof DecisionNodeData) {
            // condition is final, so it is populated rather than replaced.
            DecisionNodeData decision = (DecisionNodeData) data;
            decision.sensorName = text(dto.decisionSensorName);
            decision.condition.sensorName = text(dto.conditionSensorName);
            decision.condition.channelKey = text(dto.conditionChannelKey);
            decision.condition.value = text(dto.conditionValue);
            if (isOperator(dto.conditionOperator)) {
                decision.condition.operator = dto.conditionOperator;
            }

        } else if (data instanceof WaitNodeData) {
            WaitNodeData wait = (WaitNodeData) data;
            wait.hours = dto.waitHours;
            wait.minutes = dto.waitMinutes;
            wait.seconds = dto.waitSeconds;
            wait.millis = dto.waitMillis;

        } else if (data instanceof RepeatNodeData) {
            ((RepeatNodeData) data).times = dto.repeatTimes;

        } else if (data instanceof EndNodeData) {
            ((EndNodeData) data).loop = dto.endLoop;
        }
    }

    /**
     * Rebuilds the links, skipping anything that no longer resolves so an old project still
     * opens with as much of its flow intact as possible. A point holds at most one link, which
     * CanvasGraph enforces when the canvas restores; the same rule is applied here so the list
     * handed to the parser matches what the canvas will draw.
     */
    public static List<Connection> toConnections(List<ConnectionDto> dtos, List<CanvasNode> nodes) {
        List<Connection> connections = new ArrayList<>();
        if (dtos == null || nodes == null) return connections;

        Map<String, CanvasNode> byId = new HashMap<>();
        for (CanvasNode node : nodes) byId.put(node.id, node);

        Set<String> claimedPoints = new HashSet<>();

        for (ConnectionDto dto : dtos) {
            if (dto == null) continue;

            CanvasNode from = byId.get(dto.fromNodeId);
            CanvasNode to = byId.get(dto.toNodeId);
            if (from == null || to == null) continue;

            ConnectionPoint fromPoint = from.type.pointById(dto.fromPointId);
            ConnectionPoint toPoint = to.type.pointById(dto.toPointId);
            if (fromPoint == null || toPoint == null) continue;

            String fromKey = from.id + "#" + fromPoint.id;
            String toKey = to.id + "#" + toPoint.id;
            if (claimedPoints.contains(fromKey) || claimedPoints.contains(toKey)) continue;

            claimedPoints.add(fromKey);
            claimedPoints.add(toKey);
            connections.add(new Connection(from, fromPoint, to, toPoint));
        }
        return connections;
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private static String safeMode(String value, String fallback) {
        if (value == null) return fallback;
        for (ActionNodeData.Mode mode : ActionNodeData.Mode.values()) {
            if (mode.name().equals(value)) return value;
        }
        return fallback;
    }

    private static boolean isOperator(String value) {
        if (value == null) return false;
        for (String operator : DecisionNodeData.OPERATORS) {
            if (operator.equals(value)) return true;
        }
        return false;
    }
}
