package com.kgjr.uno.screens.fragments.codeHelper.flow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kgjr.uno.screens.fragments.codeHelper.model.ActionNodeData;
import com.kgjr.uno.screens.fragments.codeHelper.model.CanvasNode;
import com.kgjr.uno.screens.fragments.codeHelper.model.Connection;
import com.kgjr.uno.screens.fragments.codeHelper.model.ConnectionPoint;
import com.kgjr.uno.screens.fragments.codeHelper.model.DecisionNodeData;
import com.kgjr.uno.screens.fragments.codeHelper.model.EndNodeData;
import com.kgjr.uno.screens.fragments.codeHelper.model.NodeType;
import com.kgjr.uno.screens.fragments.codeHelper.model.RepeatNodeData;
import com.kgjr.uno.screens.fragments.codeHelper.model.WaitNodeData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Parses the canvas graph into {@link FlowBlock}s and renders them to the code string.
 *
 * <p>End blocks close a section for the user's benefit and produce no code, except the one that
 * terminates the whole program: that either emits END or, when set to loop, wraps everything in
 * LOOP(FOREVER).</p>
 */
public final class FlowCode {

    // Indexes into NodeType.connectionPoints.
    private static final int START_OUT = 0;
    private static final int OUT = 1;
    private static final int YES = 2;
    private static final int NO = 3;
    private static final int LOOP = 2;

    private final List<CanvasNode> nodes;
    private final List<Connection> connections;
    private final Set<String> onPath = new HashSet<>();

    private boolean loopsBackToStart;

    /** Whether the run that just returned from walk() finished on an End block. */
    private boolean walkClosed;

    private FlowCode(List<CanvasNode> nodes, List<Connection> connections) {
        this.nodes = nodes != null ? nodes : new ArrayList<CanvasNode>();
        this.connections = connections != null ? connections : new ArrayList<Connection>();
    }

    @NonNull
    public static List<FlowBlock> parse(List<CanvasNode> nodes, List<Connection> connections) {
        return new FlowCode(nodes, connections).build();
    }

    /** The End block that terminates the program, i.e. the one on the main chain. */
    @Nullable
    public static CanvasNode finalEnd(List<CanvasNode> nodes, List<Connection> connections) {
        FlowCode flow = new FlowCode(nodes, connections);

        CanvasNode node = flow.findStart();
        Set<String> seen = new HashSet<>();

        while (node != null && seen.add(node.id)) {
            if (node.type == NodeType.END) return node;
            node = flow.next(node, node.type == NodeType.START ? START_OUT : OUT);
        }
        return null;
    }

    public static boolean isFinalEnd(CanvasNode node, List<CanvasNode> nodes, List<Connection> connections) {
        return node != null && node.type == NodeType.END && node == finalEnd(nodes, connections);
    }

    // ----------------------------------------------------------------- parse

    @NonNull
    private List<FlowBlock> build() {
        CanvasNode start = findStart();
        if (start == null) return new ArrayList<>();

        List<FlowBlock> body = walk(next(start, START_OUT), true);

        List<FlowBlock> tree = new ArrayList<>();
        tree.add(new FlowBlock(NodeType.START, start.data));

        if (loopsBackToStart) {
            FlowBlock forever = new FlowBlock(NodeType.REPEAT, null);
            forever.forever = true;
            forever.closed = true;
            forever.body.addAll(body);
            tree.add(forever);
        } else {
            tree.addAll(body);
        }
        return tree;
    }

    @NonNull
    private List<FlowBlock> walk(@Nullable CanvasNode node, boolean topLevel) {
        List<FlowBlock> result = new ArrayList<>();
        List<String> claimed = new ArrayList<>();
        boolean closed = false;

        try {
            while (node != null) {
                if (!onPath.add(node.id)) break; // cycle
                claimed.add(node.id);

                FlowBlock block = new FlowBlock(node.type, node.data);

                switch (node.type) {
                    case REPEAT:
                        block.body.addAll(walk(next(node, LOOP), false));
                        block.closed = walkClosed;
                        result.add(block);
                        node = next(node, OUT);
                        break;

                    case DECISION: {
                        block.body.addAll(walk(next(node, YES), false));
                        boolean trueClosed = walkClosed;
                        block.elseBody.addAll(walk(next(node, NO), false));
                        block.closed = trueClosed && walkClosed;
                        result.add(block);
                        node = next(node, OUT);
                        break;
                    }

                    case END:
                        // Only the program's own End produces code; the rest just close a section.
                        if (topLevel) {
                            if (node.data instanceof EndNodeData && ((EndNodeData) node.data).loop) {
                                loopsBackToStart = true;
                            } else {
                                result.add(block);
                            }
                        }
                        closed = true;
                        node = null;
                        break;

                    case START:
                        node = next(node, START_OUT);
                        break;

                    default: // ACTION, WAIT
                        result.add(block);
                        node = next(node, OUT);
                        break;
                }
            }
        } finally {
            onPath.removeAll(claimed);
        }

        walkClosed = closed;
        return result;
    }

    @Nullable
    private CanvasNode findStart() {
        for (CanvasNode node : nodes) {
            if (node.type == NodeType.START) return node;
        }
        return null;
    }

    /** The node wired to a connection point, accepting a link stored in either direction. */
    @Nullable
    private CanvasNode next(@Nullable CanvasNode node, int pointIndex) {
        if (node == null || node.type == null) return null;

        List<ConnectionPoint> points = node.type.connectionPoints;
        if (points == null || pointIndex >= points.size()) return null;
        ConnectionPoint point = points.get(pointIndex);

        for (Connection c : connections) {
            if (c == null) continue;
            if (c.from == node && c.fromPoint == point) return c.to;
            if (c.to == node && c.toPoint == point) return c.from;
        }
        return null;
    }

    // -------------------------------------------------------------- generate

    @NonNull
    public static String generate(List<FlowBlock> blocks) {
        StringBuilder sb = new StringBuilder();
        write(blocks, sb, 0);
        return sb.toString().trim();
    }

    private static void write(List<FlowBlock> blocks, StringBuilder sb, int indent) {
        if (blocks == null) return;

        for (FlowBlock b : blocks) {
            indent(sb, indent);
            switch (b.type) {
                case START:
                    sb.append("BEGIN\n");
                    break;

                case END:
                    sb.append("END\n");
                    break;

                case ACTION: {
                    ActionNodeData d = b.data instanceof ActionNodeData ? (ActionNodeData) b.data : null;
                    String mode = d != null && d.mode != null ? d.mode.label : ActionNodeData.Mode.COMMAND.label;
                    String command = d != null && d.command != null ? d.command.trim() : "";
                    sb.append("ACTION(").append(mode).append(" : ").append(command).append(")\n");
                    break;
                }

                case WAIT:
                    sb.append("WAIT(").append(waitMillis(b)).append(")\n");
                    break;

                case REPEAT:
                    sb.append("LOOP(").append(b.forever ? "FOREVER" : repeatTimes(b)).append(") {\n");
                    write(b.body, sb, indent + 1);
                    indent(sb, indent);
                    sb.append("}\n");
                    break;

                case DECISION:
                    sb.append("IF (").append(decisionExpression(b)).append(") {\n");
                    write(b.body, sb, indent + 1);
                    if (!b.elseBody.isEmpty()) {
                        indent(sb, indent);
                        sb.append("} ELSE {\n");
                        write(b.elseBody, sb, indent + 1);
                    }
                    indent(sb, indent);
                    sb.append("}\n");
                    break;
            }
        }
    }

    private static void indent(StringBuilder sb, int level) {
        for (int i = 0; i < level; i++) sb.append("    ");
    }

    public static long waitMillis(FlowBlock b) {
        if (!(b.data instanceof WaitNodeData)) return 0L;
        WaitNodeData d = (WaitNodeData) b.data;
        return d.hours * 3600000L + d.minutes * 60000L + d.seconds * 1000L + d.millis;
    }

    public static int repeatTimes(FlowBlock b) {
        return b.data instanceof RepeatNodeData ? Math.max(((RepeatNodeData) b.data).times, 0) : 0;
    }

    public static String decisionExpression(FlowBlock b) {
        return b.data instanceof DecisionNodeData ? ((DecisionNodeData) b.data).expression() : "";
    }

    // -------------------------------------------------------------- validate

    /** Returns null when the program is runnable, otherwise the reason it isn't. */
    @Nullable
    public static String validate(String code, List<FlowBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return "Nothing to run. Add blocks and connect them to Start.";
        }
        if (!hasWork(blocks)) {
            return "Nothing is connected to Start yet.";
        }
        if (code == null || code.trim().isEmpty()) {
            return "The flow produced no code.";
        }

        String blockError = checkBlocks(blocks);
        if (blockError != null) return blockError;

        if (!endsProperly(blocks)) {
            return "The flow needs a final End block. Add one at the bottom and set it to End or Loop.";
        }
        return checkBrackets(code);
    }

    private static boolean hasWork(List<FlowBlock> blocks) {
        for (FlowBlock b : blocks) {
            if (b.type == NodeType.START || b.type == NodeType.END) continue;
            if (b.type == NodeType.REPEAT && b.forever) {
                if (hasWork(b.body)) return true;
                continue;
            }
            return true;
        }
        return false;
    }

    /** Every block must be configured, and every nested section closed by an End block. */
    @Nullable
    private static String checkBlocks(List<FlowBlock> blocks) {
        for (FlowBlock b : blocks) {
            switch (b.type) {
                case DECISION: {
                    DecisionNodeData d = b.data instanceof DecisionNodeData ? (DecisionNodeData) b.data : null;
                    if (d == null || !d.condition.isSet()) {
                        return "A Decision block has no condition. Open it and set one.";
                    }
                    if (!d.condition.hasValue()) {
                        return "A Decision condition has no value to compare against.";
                    }
                    if (b.body.isEmpty() && b.elseBody.isEmpty()) {
                        return "A Decision block has no branches. Wire Yes or No to the next step.";
                    }
                    String yes = checkBlocks(b.body);
                    if (yes != null) return yes;
                    String no = checkBlocks(b.elseBody);
                    if (no != null) return no;
                    break;
                }

                case ACTION: {
                    ActionNodeData d = b.data instanceof ActionNodeData ? (ActionNodeData) b.data : null;
                    boolean sendsCommand = d == null || d.mode == null || d.mode.sendsCommand();
                    if (sendsCommand && (d == null || d.command == null || d.command.trim().isEmpty())) {
                        return "An Action block has no command. Open it and set one.";
                    }
                    break;
                }

                case REPEAT: {
                    if (!b.forever) {
                        if (repeatTimes(b) < 1) {
                            return "A Loop block is set to 0. Open it and set how many times it repeats.";
                        }
                        if (b.body.isEmpty()) {
                            return "A Loop block is empty. Add blocks to it or remove it.";
                        }
                        if (!b.closed) {
                            return "A Loop block is not closed. Add an End block at the end of its body.";
                        }
                    }
                    String nested = checkBlocks(b.body);
                    if (nested != null) return nested;
                    break;
                }

                default:
                    break;
            }
        }
        return null;
    }

    private static boolean endsProperly(List<FlowBlock> blocks) {
        FlowBlock last = blocks.get(blocks.size() - 1);
        return last.type == NodeType.END || (last.type == NodeType.REPEAT && last.forever);
    }

    /** ACTION lines are skipped: a shell command or URL may hold unbalanced brackets of its own. */
    @Nullable
    private static String checkBrackets(String code) {
        Deque<Character> stack = new ArrayDeque<>();

        for (String line : code.split("\n", -1)) {
            if (line.trim().startsWith("ACTION(")) continue;

            for (int i = 0; i < line.length(); i++) {
                char ch = line.charAt(i);
                if (ch == '(' || ch == '{') {
                    stack.push(ch);
                } else if (ch == ')') {
                    if (stack.isEmpty() || stack.peek() != '(') return "Unmatched ')' in: " + line.trim();
                    stack.pop();
                } else if (ch == '}') {
                    if (stack.isEmpty() || stack.peek() != '{') return "Unmatched '}' in: " + line.trim();
                    stack.pop();
                }
            }
        }
        return stack.isEmpty() ? null : "A block was left open in the generated code.";
    }
}