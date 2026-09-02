package com.kgjr.uno;

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

    public static void clearCanvas() {
        canvasNodes = new ArrayList<>();
        canvasConnections = new ArrayList<>();
        generatedCode = "";
        flowTree = new ArrayList<>();
    }
}