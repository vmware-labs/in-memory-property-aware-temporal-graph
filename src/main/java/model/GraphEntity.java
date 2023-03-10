package model;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

/**
 * An object that represents a core entity within a graph whoch could be one of: <ul>Vertex</ul>, <ul>Edge</ul>
 * <ul>Property</ul>
 * <br/>
 * Every graph entity object has a unique identifier and provides the time-instant at which
 * it got created
 */
public interface GraphEntity {

    /**
     * Gets the Identifier associated with the graph entity.
     * @return the entity identifier
     */
    int getId();

    /**
     * Gets the time instance at which the graph entity was created.
     * @return the time instant in milliseconds at which the entity got created.
     */
    long getTime();

}
