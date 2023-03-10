package core;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import core.propertystore.PropertyStore;
import exceptions.PropertyNotFoundException;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSortedSet;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import model.Graph;
import model.Property;
import model.TimestampedPropertyValue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@EqualsAndHashCode
@AllArgsConstructor
public class TemporalGraph implements Graph {
    static final int MAX_GRAPH_STORAGE_DURATION = Integer.MAX_VALUE;
    private static final String TIME_RANGE_CROSSED_ERR_MSG_TEMPLATE = "Timestamp supplied %s falls beyond supported range.";
    private static final String SRC_DEST_NOT_FOUND_ERR_MSG_TEMPLATE = "Source vertex Id %d or destination vertex Id %d not found at time: %d.";
    private static final String VERTEX_NOT_FOUND_ERR_MSG_TEMPLATE = "Vertex with Id %d not found at time: %d.";
    private static final String EDGE_NOT_FOUND_ERR_MSG_TEMPLATE = "Edge with Id %d not found at time: %d.";
    private static final String NULL_EMPTY_PROPERTIES_ERR_MSG_TEMPLATE = "Properties can't be empty or null";
    private static final String PROPERTIES_TIME_ERR_MSG_TEMPLATE = "Property addition time: %d should be greater than %s addition time %d for property %s";
    private final long initTs;
    // atomic running counter for a vertex index
    private final AtomicInteger vertexIndex = new AtomicInteger(0);

    // atomic running counter for an edge index
    private final AtomicInteger edgeIndex = new AtomicInteger(0);

    // map maintaining vertices as per time Map<timeDifferential, Set<>>
    private final TreeMap<Integer, IntSet> verticesByTime = new TreeMap<>();

    // stores outgoing edges of a vertex map<vertexId, TreeMap<time, <EdgeIds>>>
    private final Map<Integer, TreeMap<Integer, IntSet>> outgoingEdgesByTimeForVertex = new Int2ObjectOpenHashMap<>();
    // stores incoming edges of a vertex
    private final Map<Integer, TreeMap<Integer, IntSet>> incomingEdgesByTimeForVertex = new Int2ObjectOpenHashMap<>();

    // time differential to edges Ids set
    private final Int2ObjectAVLTreeMap<IntSet> edgesByTime = new Int2ObjectAVLTreeMap<>();

    // vertex to property map <vertexId, Set<Property>>
    private final Map<Integer, IntOpenHashSet> vertexProperties = new Int2ObjectOpenHashMap<>();

    // edge to property map <EdgeId, Set<Property>>
    private final Map<Integer, IntOpenHashSet> edgeProperties = new Int2ObjectOpenHashMap<>();

    private final PropertyStore propertyStore = new PropertyStore();

    // This needs to be checked while finding time differential from the user specified timestamp
    @VisibleForTesting
    protected boolean validateTimestamp(final long ts) {
        if (ts < initTs) {
            return false;
        }
        return ts - initTs < MAX_GRAPH_STORAGE_DURATION;
    }

    @Override
    public int addVertex(final Collection<Property> properties, final long timestamp) {
        Preconditions.checkArgument(validateTimestamp(timestamp),
                String.format(TIME_RANGE_CROSSED_ERR_MSG_TEMPLATE, timestamp));
        // Preconditions.checkArgument(null != properties && !properties.isEmpty(), NULL_EMPTY_PROPERTIES_ERR_MSG_TEMPLATE);
        final int vertexId = vertexIndex.getAndIncrement();
        IntSet vertexIds = verticesByTime.computeIfAbsent((int) (timestamp - initTs), t -> new IntOpenHashSet(1));
        vertexIds.add(vertexId);
        //adding property for vertex if properties are specified. first compress the property
        if (properties != null && !properties.isEmpty()) {
            for (final Property p : properties) {
                Preconditions.checkArgument(timestamp <= p.getTime(),
                        String.format(PROPERTIES_TIME_ERR_MSG_TEMPLATE, p.getTime(), "Vertex", timestamp, p.getName()));
            }
            IntOpenHashSet propertyIds = new IntOpenHashSet(properties.size());
            properties.stream().forEach( p -> {
                propertyIds.add(p.getId());
                propertyStore.put(p.getId(), p);
            });
            vertexProperties.put(vertexId, propertyIds);
        }

        return vertexId;
    }

    @Override
    public int addEdge(int srcVertexId, int destVertexId, Collection<Property> properties, long timestamp) {
        Preconditions.checkArgument(validateTimestamp(timestamp),
                String.format(TIME_RANGE_CROSSED_ERR_MSG_TEMPLATE, timestamp));
        Preconditions.checkArgument((srcVertexId < vertexIndex.get() && destVertexId < vertexIndex.get()),
                String.format(SRC_DEST_NOT_FOUND_ERR_MSG_TEMPLATE, srcVertexId, destVertexId, timestamp));

        final int edgeId = edgeIndex.getAndIncrement();
        final int timeDifferential = (int) (timestamp - initTs);
        final IntSet edgeIds = edgesByTime.computeIfAbsent(timeDifferential, t -> new IntOpenHashSet(1));
        edgeIds.add(edgeId);

        //add outgoing edges in the map
        addEdgeForVertices(srcVertexId, timeDifferential, edgeId, outgoingEdgesByTimeForVertex);

        //add incoming edges in the map
        addEdgeForVertices(destVertexId, timeDifferential, edgeId, incomingEdgesByTimeForVertex);

        //adding property for edge if properties are specified
        if (properties != null && !properties.isEmpty()) {
            for (final Property p : properties) {
                Preconditions.checkArgument(timestamp <= p.getTime(),
                        String.format(PROPERTIES_TIME_ERR_MSG_TEMPLATE, p.getTime(), "Edge", timestamp, p.getName()));
            }
            IntOpenHashSet propertyIds = new IntOpenHashSet(properties.size());
            properties.stream().forEach( p -> {
                propertyIds.add(p.getId());
                propertyStore.put(p.getId(), p);
            });
            edgeProperties.put(edgeId, propertyIds);
        }

        return edgeId;
    }

    private void addEdgeForVertices(final int vertexId,
                                    final int timeDifferential,
                                    final int edgeId,
                                    final Map<Integer, TreeMap<Integer, IntSet>> edgesByTimeForVertex) {
        final TreeMap<Integer, IntSet> edgeMap = edgesByTimeForVertex.computeIfAbsent(vertexId, t -> new TreeMap<>());
        final Set<Integer> outgoingEdges = edgeMap.computeIfAbsent(timeDifferential, t -> new IntOpenHashSet(1));
        outgoingEdges.add(edgeId);
    }

    @Override
    public Iterator<Integer> getVerticesAtTime(final long timestamp) {
        Preconditions.checkArgument(validateTimestamp(timestamp),
                String.format(TIME_RANGE_CROSSED_ERR_MSG_TEMPLATE, timestamp));
        final int timeDifferential = (int) (timestamp - initTs);
        final Map.Entry<Integer, IntSet> floorEntry = verticesByTime.floorEntry(timeDifferential);
        if (null == floorEntry || null == floorEntry.getKey()) {
            return Collections.emptyIterator();
        }
        return getIntIterator(verticesByTime.firstKey(), floorEntry.getKey(), verticesByTime).iterator();
    }

    @Override
    public Iterator<Integer> getEdgesAtTime(final int srcVertexId,
                                            final int destVertexId,
                                            final long timestamp) {
        Preconditions.checkArgument(validateTimestamp(timestamp),
                String.format(TIME_RANGE_CROSSED_ERR_MSG_TEMPLATE, timestamp));
        Preconditions.checkArgument((srcVertexId < vertexIndex.get() && destVertexId < vertexIndex.get()),
                String.format(SRC_DEST_NOT_FOUND_ERR_MSG_TEMPLATE, srcVertexId, destVertexId, timestamp));
        Set<Integer> outEdges = Sets.newHashSet(getOutEdgesAtTime(srcVertexId, timestamp));
        if (outEdges.isEmpty()) {
            return Collections.emptyIterator();
        }
        Set<Integer> inEdges = Sets.newHashSet(getInEdgesAtTime(destVertexId, timestamp));
        if (inEdges.isEmpty()) {
            return Collections.emptyIterator();
        }
        outEdges.retainAll(inEdges);
        return outEdges.iterator();
    }

    @Override
    public Iterator<Integer> getOutEdgesAtTime(final int vertexId, final long timestamp) {
        Preconditions.checkArgument(validateTimestamp(timestamp),
                String.format(TIME_RANGE_CROSSED_ERR_MSG_TEMPLATE, timestamp));
        Preconditions.checkArgument(vertexId < vertexIndex.get(),
                String.format(VERTEX_NOT_FOUND_ERR_MSG_TEMPLATE, vertexId, timestamp));
        final TreeMap<Integer, IntSet> outgoingEdgesForVertex = outgoingEdgesByTimeForVertex.get(vertexId);
        if (null == outgoingEdgesForVertex) {
            return Collections.emptyIterator();
        }
        final int timeDifferential = (int) (timestamp - initTs);
        final Map.Entry<Integer, IntSet> floorEntry = outgoingEdgesForVertex.floorEntry(timeDifferential);
        if (null == floorEntry || null == floorEntry.getKey()) {
            return Collections.emptyIterator();
        }
        return getIntIterator(outgoingEdgesForVertex.firstKey(), floorEntry.getKey(), outgoingEdgesForVertex).iterator();
    }

    @Override
    public Iterator<Integer> getInEdgesAtTime(final int vertexId, final long timestamp) {
        Preconditions.checkArgument(validateTimestamp(timestamp),
                String.format(TIME_RANGE_CROSSED_ERR_MSG_TEMPLATE, timestamp));
        Preconditions.checkArgument(vertexId < vertexIndex.get(),
                String.format(VERTEX_NOT_FOUND_ERR_MSG_TEMPLATE, vertexId, timestamp));
        final TreeMap<Integer, IntSet> incomingEdgesForVertex = incomingEdgesByTimeForVertex.get(vertexId);
        if (null == incomingEdgesForVertex) {
            return null;
        }
        final int timeDifferential = (int) (timestamp - initTs);
        final Integer floorKey = incomingEdgesForVertex.floorEntry(timeDifferential).getKey();
        if (null == floorKey) {
            return null;
        }
        return getIntIterator(incomingEdgesForVertex.firstKey(), floorKey, incomingEdgesForVertex).iterator();
    }

    @Override
    public Collection<TimestampedPropertyValue> getVertexPropertiesAtTime(final int vertexId, final long timestamp) {
        Preconditions.checkArgument(validateTimestamp(timestamp),
                String.format(TIME_RANGE_CROSSED_ERR_MSG_TEMPLATE, timestamp));
        Preconditions.checkArgument(vertexId < vertexIndex.get(),
                String.format(VERTEX_NOT_FOUND_ERR_MSG_TEMPLATE, vertexId, timestamp));
        final Set<Property> allProperties = getEntityProperties(vertexProperties, vertexId);
        final Set<TimestampedPropertyValue> propertiesWithValue = new HashSet<>();
        for (final Property p : allProperties) {
            final Object value = p.getValueAtTime(timestamp);
            if (null == value) {
                continue;
            }
            propertiesWithValue.add(new TimestampedPropertyValue(p.getName(), value));
        }
        return propertiesWithValue;
    }

    @Override
    public TimestampedPropertyValue getVertexPropertyAtTime(final int vertexId, final String propertyName, final long timestamp) {
        Preconditions.checkArgument(validateTimestamp(timestamp),
                String.format(TIME_RANGE_CROSSED_ERR_MSG_TEMPLATE, timestamp));
        Preconditions.checkArgument(vertexId < vertexIndex.get(),
                String.format(VERTEX_NOT_FOUND_ERR_MSG_TEMPLATE, vertexId, timestamp));
        final Set<Property> allProperties = getEntityProperties(vertexProperties, vertexId);
        for (final Property p : allProperties) {
            if (!propertyName.equals(p.getName())) {
                continue;
            }
            final Object value = p.getValueAtTime(timestamp);
            if (null == value) {
                return null;
            }
            return new TimestampedPropertyValue(p.getName(), value);
        }
        return null;
    }

    @Override
    public Collection<TimestampedPropertyValue> getEdgePropertiesAtTime(final int edgeId, final long timestamp) {
        Preconditions.checkArgument(validateTimestamp(timestamp),
                String.format(TIME_RANGE_CROSSED_ERR_MSG_TEMPLATE, timestamp));
        Preconditions.checkArgument(edgeId < edgeIndex.get(),
                String.format(EDGE_NOT_FOUND_ERR_MSG_TEMPLATE, edgeId, timestamp));
        final Set<Property> allProperties = getEntityProperties(edgeProperties, edgeId);
        final Set<TimestampedPropertyValue> propertiesWithValue = new HashSet<>();
        for (final Property p : allProperties) {
            final Object value = p.getValueAtTime(timestamp);
            if (null == value) {
                continue;
            }
            propertiesWithValue.add(new TimestampedPropertyValue(p.getName(), value));
        }
        return propertiesWithValue;
    }

    @Override
    public TimestampedPropertyValue getEdgePropertyAtTime(final int edgeId, final String propertyName, final long timestamp) {
        Preconditions.checkArgument(validateTimestamp(timestamp),
                String.format(TIME_RANGE_CROSSED_ERR_MSG_TEMPLATE, timestamp));
        Preconditions.checkArgument(edgeId < edgeIndex.get(),
                String.format(EDGE_NOT_FOUND_ERR_MSG_TEMPLATE, edgeId, timestamp));
        final Set<Property> allProperties = getEntityProperties(edgeProperties, edgeId);
        for (final Property p : allProperties) {
            if (!propertyName.equals(p.getName())) {
                continue;
            }
            final Object value = p.getValueAtTime(timestamp);
            return new TimestampedPropertyValue(p.getName(), value);
        }
        return null;
    }

    @Override
    public Map<Integer, Collection<TimestampedPropertyValue>> getEdgePropertiesAtTime(final int srcVertexId,
                                                                                      final int destVertexId,
                                                                                      final long timestamp) {
        Preconditions.checkArgument(validateTimestamp(timestamp),
                String.format(TIME_RANGE_CROSSED_ERR_MSG_TEMPLATE, timestamp));
        Preconditions.checkArgument((srcVertexId < vertexIndex.get() && destVertexId < vertexIndex.get()),
                String.format(SRC_DEST_NOT_FOUND_ERR_MSG_TEMPLATE, srcVertexId, destVertexId, timestamp));
        final Iterator<Integer> edgesBetweenVertices = getEdgesAtTime(srcVertexId, destVertexId, timestamp);
        final Map<Integer, Collection<TimestampedPropertyValue>> edgeToProperties = new Int2ObjectOpenHashMap<>();
        if (null == edgesBetweenVertices) {
            return edgeToProperties;
        }
        for (final int edgeId : Sets.newHashSet(edgesBetweenVertices)) {
            edgeToProperties.put(edgeId, getEdgePropertiesAtTime(edgeId, timestamp));
        }
        return edgeToProperties;
    }

    @Override
    public Map<Integer, TimestampedPropertyValue> getEdgePropertyAtTime(final int srcVertexId,
                                                                        final int destVertexId,
                                                                        final String propertyName,
                                                                        final long timestamp) {
        Preconditions.checkArgument(validateTimestamp(timestamp),
                String.format(TIME_RANGE_CROSSED_ERR_MSG_TEMPLATE, timestamp));
        Preconditions.checkArgument((srcVertexId < vertexIndex.get() && destVertexId < vertexIndex.get()),
                String.format(SRC_DEST_NOT_FOUND_ERR_MSG_TEMPLATE, srcVertexId, destVertexId, timestamp));
        final Iterator<Integer> edgesBetweenVertices = getEdgesAtTime(srcVertexId, destVertexId, timestamp);
        final Map<Integer, TimestampedPropertyValue> edgeToProperties = new Int2ObjectOpenHashMap<>();
        if (null == edgesBetweenVertices) {
            return edgeToProperties;
        }
        for (final int edgeId : Sets.newHashSet(edgesBetweenVertices)) {
            edgeToProperties.put(edgeId, getEdgePropertyAtTime(edgeId, propertyName, timestamp));
        }
        return edgeToProperties;
    }

    @Override
    public void addVertexProperty(final int vertexId,
                                  final String propertyName,
                                  final Object value,
                                  long timestamp) throws PropertyNotFoundException {
        Preconditions.checkArgument(validateTimestamp(timestamp),
                String.format(TIME_RANGE_CROSSED_ERR_MSG_TEMPLATE, timestamp));
        Preconditions.checkArgument(vertexId < vertexIndex.get(),
                String.format(VERTEX_NOT_FOUND_ERR_MSG_TEMPLATE, vertexId, timestamp));
        final Set<Property> allProperties = getEntityProperties(vertexProperties, vertexId);
        final Property p = getPropertyFromSet(propertyName, allProperties);
        if (null == p) {
            throw new PropertyNotFoundException(String.format("Property %s not found for the vertex: %d", propertyName, vertexId));
        } else {
            p.setValueAtTime(timestamp, value);
            propertyStore.put(p.getId(), p);
        }
    }

    @Override
    public void addEdgeProperty(final int edgeId,
                                final String propertyName,
                                final Object value,
                                long timestamp) throws PropertyNotFoundException {
        Preconditions.checkArgument(validateTimestamp(timestamp),
                String.format(TIME_RANGE_CROSSED_ERR_MSG_TEMPLATE, timestamp));
        Preconditions.checkArgument(edgeId < edgeIndex.get(),
                String.format(EDGE_NOT_FOUND_ERR_MSG_TEMPLATE, edgeId, timestamp));
        final Set<Property> allProperties = getEntityProperties(edgeProperties, edgeId);
        final Property p = getPropertyFromSet(propertyName, allProperties);
        if (null == p) {
            throw new PropertyNotFoundException(String.format("Property %s not found for the edge: %d", propertyName, edgeId));
        } else {
            p.setValueAtTime(timestamp, value);
            propertyStore.put(p.getId(), p);
        }
    }

    @Override
    public Iterator<Integer> getAllEdgesAtTime(long timestamp) {
        Preconditions.checkArgument(validateTimestamp(timestamp),
                String.format(TIME_RANGE_CROSSED_ERR_MSG_TEMPLATE, timestamp));
        final int timeDifferential = (int) (timestamp - initTs);

        final Set<Integer> allEdgesAtTime = getIntIterator(timeDifferential, edgesByTime);
        if (allEdgesAtTime.isEmpty()) {
            return Collections.emptyIterator();
        }
        return allEdgesAtTime.iterator();
    }

    /*
    * Algorithm
    * Graph init time : t0, purging time of graph: tp
    * Get all vertices till time floor(tp). => v
    * Get all the outgoing / incoming edges for those vertices. => e
    * From Property store, remove all the data points for each property of the e, v from init time till floor(tp).*/
    @Override
    public void purgeAtTime(final long timestamp) {
        Preconditions.checkArgument(validateTimestamp(timestamp),
                String.format(TIME_RANGE_CROSSED_ERR_MSG_TEMPLATE, timestamp));
        purgeEdgeProperties(timestamp);
        purgeVertexProperties(timestamp);

    }

    private void purgeVertexProperties(long timestamp) {
        Iterator<Integer> verticesIterator = getVerticesAtTime(timestamp);
        if (null == verticesIterator) {
            return;
        }
        for (final int vertexId : Lists.newArrayList(verticesIterator)) {
            IntOpenHashSet propertyIdsPerVertex = vertexProperties.get(vertexId);
            if (null == propertyIdsPerVertex) {
                continue;
            }
            propertyStore.purgePropertiesTillTime(propertyIdsPerVertex
                    , timestamp);
        }
    }

    private void purgeEdgeProperties(final long timestamp) {
        Iterator<Integer> edgesIterator = getAllEdgesAtTime(timestamp);
        if (null == edgesIterator) {
            return;
        }
        for (final int edgeId : Lists.newArrayList(edgesIterator)) {
            IntOpenHashSet propertyIdsPerEdge = edgeProperties.get(edgeId);
            if (null == propertyIdsPerEdge) {
                continue;
            }
            propertyStore.purgePropertiesTillTime(propertyIdsPerEdge, timestamp);
        }
    }

    private void trimInternalMapsForVertexEdgeDirectionMapping(Int2ObjectOpenHashMap mapping) {
        Int2ObjectMap.FastEntrySet<TreeMap<Integer, IntSet>> entries = ((Int2ObjectOpenHashMap<TreeMap<Integer, IntSet>>) mapping).int2ObjectEntrySet();
        for (Int2ObjectMap.Entry<TreeMap<Integer, IntSet>> entry : entries) {
            TreeMap<Integer, IntSet> tm = entry.getValue();
            for (Map.Entry<Integer, IntSet> intSet : tm.entrySet()) {
                ((IntOpenHashSet)(intSet.getValue())).trim();
            }
        }
    }

    private void trimInternalMapsForVertexToEdgeMapping(Long2ObjectOpenHashMap mapping) {
        Long2ObjectMap.FastEntrySet<IntSet> entries = mapping.long2ObjectEntrySet();
        for (Long2ObjectMap.Entry<IntSet> entry : entries) {
            IntSet is = entry.getValue();
            ((IntOpenHashSet)(is)).trim();
        }
    }

    private void trimInternalMapsForProperties(Int2ObjectOpenHashMap mapping) {
        Int2ObjectMap.FastEntrySet<IntSet> entries = mapping.int2ObjectEntrySet();
        for (Int2ObjectMap.Entry<IntSet> entry : entries) {
            IntSet is = entry.getValue();
            ((IntOpenHashSet)(is)).trim();
        }
    }

    private Property getPropertyFromSet(final String propertyName, final Set<Property> allProperties) {
        if (null == allProperties) {
            return null;
        }
        for (final Property p : allProperties) {
            if (propertyName.equals(p.getName())) {
                return p;
            }
        }
        return null;
    }

    private Set<Integer> getIntIterator(final int firstKey,
                                        final int floorKey,
                                        final TreeMap<Integer, IntSet> map) {
        final Set<Integer> entityCollection = new HashSet<>();
        final Map<Integer, IntSet> subMap = map.subMap(firstKey, true, floorKey, true);
        if (null == subMap) {
            return entityCollection;
        }
        for (final Map.Entry<Integer, IntSet> entry : subMap.entrySet()) {
            entityCollection.addAll(entry.getValue());
        }
        return entityCollection;
    }

    private Set<Integer> getIntIterator(final int floorKey,
                                        final Int2ObjectAVLTreeMap<IntSet> map) {
        final Set<Integer> entityCollection = new HashSet<>();
        Int2ObjectSortedMap<IntSet> headMap =  map.headMap(floorKey);
        ObjectSortedSet<Int2ObjectMap.Entry<IntSet>> headMapEntries = headMap.int2ObjectEntrySet();
        for (Int2ObjectMap.Entry<IntSet> e : headMapEntries) {
            entityCollection.addAll(e.getValue());
        }
        IntSet floorKeyEntry = map.get(floorKey);
        if (floorKeyEntry != null) {
            entityCollection.addAll(floorKeyEntry);
        }
        return entityCollection;
    }

    private Set<Property> getEntityProperties(Map<Integer, IntOpenHashSet> entityProperties, final int entityId) {
        IntOpenHashSet propertyIds = entityProperties.getOrDefault(entityId, new IntOpenHashSet());
        if (propertyIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Property> properties =
                propertyIds.stream().map(propertyStore::get).collect(Collectors.toSet());

        return properties;
    }
}
