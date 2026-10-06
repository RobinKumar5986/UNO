package com.kgjr.uno.models.project;

import java.util.ArrayList;
import java.util.List;

/** One stored trigger flow. Its receive format lives on the Start node's NodeDto. */
public class TriggerDto {

    public String id;

    public List<NodeDto> nodes = new ArrayList<>();
    public List<ConnectionDto> connections = new ArrayList<>();

    public boolean viewportSaved;
    public float scale = 1f;
    public float translateX;
    public float translateY;
}
