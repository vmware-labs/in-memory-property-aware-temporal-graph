package model;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

/**
 * Contract representing an edge between two vertices. Edge gave a direction as specified by
 * the {@link Direction} enum and can have one or more properties and a label.
 * A label represents a classifier for an edge. For e.g. there could be edges with\
 * the label as <b>NetworkConnectivity</b> which is the set of all edges that represent
 * a network communication between two workloads or IP addresses.
 * The association between an edge, and it's set of properties is time aware.
 * It exposes mechanisms to retrieve the value of a given property at a particular time.
 * Similarly, it is also exposes mechanism to retrieve a list of properties at a particular time instant.
 */
public interface Edge extends PropertyAwareEntity {

    String getLabel();

    Vertex getSrcVertex();

    Vertex getDestVertex();

    Direction getDirection();
}
