package model;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

import java.util.Collection;

/**
 * An object representing a vertex within a graph.
 * {@link Vertex} instances have labels which are used to classifiy the vertices. For e.g.
 * there could be vertices in a graph with label <b>VM</b> representing the Virtual machine
 * workloads in a data-center graph.
 * Vertices can have one or more properties. Vertices can also have one more incoming or cutgoing edges
 * The association between {@link Vertex} and the set of properties and incoming or outgoing edges is time aware
 */
public interface Vertex extends PropertyAwareEntity {

    /**
     * Get the label associated with a vertex
     * @return the label associated with the vertex
     */
    String getLabel();

    /**
     * Get the outgoing edges from a vertex at specific timestamp
     * @param timestamp the timestamp at which the outgoing edges need to be retrieved
     * @return the collection of {@link Edge} instances representing outgoing edges
     */
    Collection<Edge> getOutEdgesAtTime(long timestamp);

    /**
     * Get the incoming edges from a vertex at specific timestamp
     * @param timestamp the timestamp at which the incoming edges need to be retrieved
     * @return the collection of {@link Edge} instances representing incoming edges
     */
    Collection<Edge> getInEdgesAtTime(long timestamp);

    /**
     * Add an edge to the  vertex at the specified time.
     * @param e the edge instance to be added
     * @param timestamp the timestamp at which the edge should be added to the vertex
     */
    void addEdgeAtTime(Edge e, long timestamp);

}
