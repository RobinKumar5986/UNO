package com.kgjr.uno.screens.fragments.codeHelper.flow;

import com.kgjr.uno.screens.fragments.codeHelper.model.NodeData;
import com.kgjr.uno.screens.fragments.codeHelper.model.NodeType;

import java.util.ArrayList;
import java.util.List;

/** One node of the parsed flow: the canvas node's type and data, plus whatever it nests. */
public class FlowBlock {

    public final NodeType type;
    public final NodeData data;

    /** REPEAT body, or the true branch of a DECISION. */
    public final List<FlowBlock> body = new ArrayList<>();

    /** False branch of a DECISION. */
    public final List<FlowBlock> elseBody = new ArrayList<>();

    /** Set on a REPEAT that runs until stopped. */
    public boolean forever;

    /** Set when the nested body finished with an End block. */
    public boolean closed;

    public FlowBlock(NodeType type, NodeData data) {
        this.type = type;
        this.data = data;
    }
}