package model;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

import exceptions.PropertyNotFoundException;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Contract representing a time-aware graph. Clients are expected to interact with implementations
 * of this contract to create vertices and edges with time-awareness <br>
 * The values returned by the addXXX methods represent the internal graph identifiers assigned to the vertex
 * and edge entity. It is the responsibility of the clients consuming the graph to maintain a mapping between
 * the returned identifier and the identifier for the entity as present within their model. Such an enforcement
 * ensures simplicity of the graph interface without having the client to maintain various representations of the
 * graph where the domain entities are the same but domain entity identifiers could be different based on the
 * use case.
 * Generally, it is recommended to embed the domain identifier of the entity as a property within the vertex or
 * the edge itself so that it is readily available in the desired format.<br>
 * The getVertices and getEdges (including the direction aware methods) methods return an iterator instead of the
 * entire vertex or edge identifier set.
 * This allows the clients to consume the vertex or edge entities in an on-demand manner while also alleviating
 * memory
 * consumption concerns when the graph has a high cardinality of vertices or edges. <br>
 */
public interface Graph {

    /**
     * add a vertex to the graph with the specified {@link Property} collection and time.
     *
     * @param properties the properties associated with the vertex
     * @param timestamp  the timestamp at which the vertex would be inserted in the graph
     * @return an identifier for the vertex as present within the graph storage layer
     */
    int addVertex(Collection<Property> properties, long timestamp);

    /**
     * add an edge to the graph between  source and destination vertices with the specified properties and time
     *
     * @param srcVertexId  the identifier of the source vertex as known to the graph storage layer
     * @param destVertexId the identifier of the destination vertex as known to the graph storage layer
     * @param properties   the properties associated with the edge
     * @param timestamp    the timestamp at which the edge would be inserted in the graph
     * @return an identifier for the edge as present within the graph storage layer
     */
    int addEdge(int srcVertexId, int destVertexId, Collection<Property> properties, long timestamp);

    /**
     * get the vertices present in the graph at a specified time.
     *
     * @param timestamp the timesatmp at which the vertex information needs to be retrieved
     * @return An {@link Iterator} that can be used to iterate through the vertex ids as known to the graph
     * storage layer
     */
    Iterator<Integer> getVerticesAtTime(long timestamp);

    /**
     * get the edges present in the graph between a source vertex and a destination vertex at a specified time
     *
     * @param srcVertexId  the id of the source vertex as known to the graph storage layer
     * @param destVertexId the id of the destination vertex as known to the graph storage layer
     * @param timestamp    the timestamp at which the edges need to be retrieved
     * @return An {@link Iterator} that can be used to iterate through the edge ids as known to the graph
     * storage layer
     */
    Iterator<Integer> getEdgesAtTime(int srcVertexId, int destVertexId, long timestamp);

    /**
     * get the outgoing edges present in the graph for a given source vertex at a specified
     * time
     *
     * @param vertexId  the id of the source vertex as known to the graph storage layer
     * @param timestamp the timestamp at which the edges need to be retrieved
     * @return An {@link Iterator} that can be used to iterate through the edge ids as known to the graph
     * storage layer
     */
    Iterator<Integer> getOutEdgesAtTime(int vertexId, long timestamp);

    /**
     * get the outgoing edges present in the graph for a given source vertex at a specified
     * time
     *
     * @param vertexId  the id of the destination vertex as known to the graph storage layer
     * @param timestamp the timestamp at which the edges need to be retrieved
     * @return An {@link Iterator} that can be used to iterate through the edge ids as known to the graph
     * storage layer
     */
    Iterator<Integer> getInEdgesAtTime(int vertexId, long timestamp);

    /**
     * get the properties associated with the vertex at the specified timestamp
     *
     * @param vertexId  the identifier of the vertex as known to the graph storage layer
     * @param timestamp the timestamp at which the properties need to be retrieved
     * @return a {@link Collection} of {@link Property} associated with the vertex at the specified time
     */
    Collection<TimestampedPropertyValue> getVertexPropertiesAtTime(int vertexId, long timestamp);

    /**
     * get the properties associated with the vertex at the specified timestamp
     *
     * @param vertexId     the identifier of the vertex as known to the graph storage layer
     * @param propertyName the name of the property for which the value needs to be retrieved.
     * @param timestamp    the timestamp at which the properties need to be retrieved
     * @return a {@link Collection} of {@link Property} associated with the vertex at the specified time
     */
    TimestampedPropertyValue getVertexPropertyAtTime(int vertexId, String propertyName, long timestamp);

    /**
     * get the properties associated with the edge at the specified timestamp
     *
     * @param edgeId    the identifier of the vertex as known to the graph storage layer
     * @param timestamp the timestamp at which the properties need to be retrieved
     * @return a {@link Collection} of {@link Property} associated with the edge at the specified time
     */
    Collection<TimestampedPropertyValue> getEdgePropertiesAtTime(int edgeId, long timestamp);

    /**
     * get the properties associated with the edge at the specified timestamp
     *
     * @param edgeId       the identifier of the vertex as known to the graph storage layer
     * @param propertyName the name of the property for which the value needs to be retrieved.
     * @param timestamp    the timestamp at which the properties need to be retrieved
     * @return a {@link Collection} of {@link Property} associated with the edge at the specified time
     */
    TimestampedPropertyValue getEdgePropertyAtTime(int edgeId, String propertyName, long timestamp);

    /**
     * get the properties associated with the edge at the specified timestamp
     *
     * @param srcVertexId  the identifier of the source vertex as known to the graph storage layer
     * @param destVertexId the identifier of the destination vertex as known to the graph storage layer
     * @param timestamp    the timestamp at which the properties need to be retrieved
     * @return a {@link Map} of  {@link Collection} of {@link Property} associated with the corresponding edge
     * id at the specified time.
     */
    Map<Integer, Collection<TimestampedPropertyValue>> getEdgePropertiesAtTime(int srcVertexId, int destVertexId, long timestamp);

    /**
     * get the property associated with the edge at the specified timestamp
     *
     * @param srcVertexId  the identifier of the source vertex as known to the graph storage layer
     * @param destVertexId the identifier of the destination vertex as known to the graph storage layer
     * @param propertyName the name of the property which needs to be retrieved
     * @param timestamp    the timestamp at which the properties need to be retrieved
     * @return a {@link Map} of {@link Property} associated with the corresponding edge
     * id at the specified time.
     */
    Map<Integer, TimestampedPropertyValue> getEdgePropertyAtTime(int srcVertexId, int destVertexId,
                                                                 String propertyName, long timestamp);

    /**
     * add the new value at the new timestamp to the existing property of vertex.
     *
     * @param vertexId     the identifier of the vertex as known to the graph storage layer
     * @param propertyName the property name for which the new value needs to be added
     * @param value        property value that needs to be added
     * @param timestamp    the timestamp at which the property value needs to be added
     */
    void addVertexProperty(int vertexId, String propertyName, Object value, long timestamp) throws PropertyNotFoundException;

    /**
     * add the new value at the new timestamp to the existing property of edge.
     *
     * @param edgeId       the identifier of the edge as known to the graph storage layer
     * @param propertyName the property name for which the new value needs to be added
     * @param value        property value that needs to be added
     * @param timestamp    the timestamp at which the property value needs to be added
     */
    void addEdgeProperty(int edgeId, String propertyName, Object value, long timestamp) throws PropertyNotFoundException;

    /**
     * get all the edges at a time.
     *
     * @param timestamp the timestamp at which the edges needs to be returned.
     */
    Iterator<Integer> getAllEdgesAtTime(long timestamp);

    /**
     * Purge the graph till the timestamp provided as a parameter.
     * This would compute intensive method and would be used if required only.
     * @param timestamp the timestamp till which graph needs to be purged.
     */
    void purgeAtTime(long timestamp);
}
