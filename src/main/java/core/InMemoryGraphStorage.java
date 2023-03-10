package core;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

import com.google.common.annotations.VisibleForTesting;
import core.propertystore.PropertyStore;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import model.Edge;
import model.Property;
import model.PropertyAwareEntity;
import model.Vertex;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("rawtypes")
@EqualsAndHashCode
@AllArgsConstructor
class InMemoryGraphStorage {
        private static final int maxGraphStorageDuration = Integer.MAX_VALUE;
        private final long initTs;
        // atomic running counter for an index
        private final AtomicInteger vertexInsertIndex = new AtomicInteger(0);

        // time based canonical entity maps.

        // private final Map<Long, Vertex> vertices = new Long2ObjectOpenHashMap<>();
        private final ObjectArrayList<Vertex> vertices = new ObjectArrayList<>(500000);
        private final Long2IntOpenHashMap vertexIdToInsertionPoint = new Long2IntOpenHashMap(500000);
        private final Map<Long, int[]> edges = new Long2ObjectOpenHashMap<>(4000000);
        //private final Map<Long, Property> properties = new HashMap<>();
        private final PropertyStore properties = new PropertyStore();
        private final NavigableMap<Long, LongSet> verticesAtTime = new TreeMap<>();
        private final NavigableMap<Long, LongSet> edgesAtTime = new TreeMap<>();
        // private final NavigableMap<Long, LongSet> propertiesAtTime = new TreeMap<>();

        // lookup maps -> entity based view by time
        private final Map<Long, TreeMap<Long, LongSet>> outgoingEdgesByTimeForVertex = new Long2ObjectOpenHashMap<>();
        private final Map<Long, TreeMap<Long, LongSet>> incomingEdgesByTimeForVertex = new Long2ObjectOpenHashMap<>();
        private final Map<Long, TreeMap<Long, LongSet>> propertiesByTimeForVertex = new Long2ObjectOpenHashMap<>();
        private final Map<Long, TreeMap<Long, LongSet>> propertiesByTimeForEdge = new Long2ObjectOpenHashMap<>();
        private final Map<Long, TreeMap<Long, IdPair>> verticesByTimeForEdge = new Long2ObjectOpenHashMap<>();

        private final Map<Long, IntOpenHashSet> vertexProperties = new Long2ObjectOpenHashMap<>(400000);
        private final Map<Long, IntOpenHashSet> edgeProperties = new Long2ObjectOpenHashMap<>(4000000);

        // lookup maps -> time based view by entity
        // TODO: check if these are really useful.
        private final NavigableMap<Long, HashMap<Long, Set<Long>>> vertexOutgoingEdgesAtTime = new TreeMap<>();
        private final NavigableMap<Long, HashMap<Long, Set<Long>>> vertexIncomingEdgesAtTime = new TreeMap<>();
        private final NavigableMap<Long, HashMap<Long, Set<Long>>> vertexPropertiesAtTime = new TreeMap<>();
        private final NavigableMap<Long, HashMap<Long, Set<Long>>> edgePropertiesAtTime = new TreeMap<>();
        private final NavigableMap<Long, HashMap<Long, Set<Long>>> edgeVerticesAtTime = new TreeMap<>();

        public long getInitTs() {
                return initTs;
        }

        // Todo: Upasana call this method while adding Edge, Vertex, Property.
        // This needs to be done while changing "long time" to "int time"
        public boolean validateTimestamp(final long ts) {
                if (ts < initTs) {
                        return false;
                }
                if (ts - initTs >= maxGraphStorageDuration) {
                        return false;
                }
                return true;
        }

        void addVertex(final Vertex v, final long timestamp) {
                /*Preconditions.checkArgument((v instanceof TemporalVertex),
                                            String.format("Vertex supplied is not of type %s ",
                                                          TemporalVertex.class.getName()));
                TemporalVertex tv = (TemporalVertex)v;


                // make a light-weight copy of the incoming vertex by storing only the
                // basic properties
                TemporalVertex.TemporalVertexBuilder b = TemporalVertex.builder();
                b.id(tv.getId());
                b.label(tv.getLabel());
                b.time(tv.getTime());
                //vertices.put(tv.getId(), b.build());
                if (!vertexIdToInsertionPoint.containsKey(tv.getId())) {
                        vertices.add(b.build());
                        vertexIdToInsertionPoint.put(tv.getId(), vertexInsertIndex.getAndIncrement());
                } else {
                        int insertIndex =  vertexIdToInsertionPoint.get(tv.getId());
                        vertices.set(insertIndex, b.build());
                }

                LongSet vertexIds = new LongOpenHashSet();

                Map.Entry<Long, LongSet> prevVerticesEntry = verticesAtTime.floorEntry(timestamp);
                if (prevVerticesEntry != null) {
                        vertexIds.addAll(prevVerticesEntry.getValue());
                }
                vertexIds.add(tv.getId());
                verticesAtTime.put(timestamp, vertexIds);

                storeGraphEntityProps(tv, propertiesByTimeForVertex, vertexProperties);*/

        }

        void addEdge(final Edge e, long timestamp) {
                /*Preconditions.checkArgument((e instanceof TemporalEdge),
                                            String.format("Edge supplied is not of type %s ",
                                                          TemporalEdge.class.getName()));
                TemporalEdge te = (TemporalEdge) e;

                // store lightweight edge representations
                int sourceVertexIndex = Integer.MIN_VALUE;
                int destVertexIndex = Integer.MIN_VALUE;

                for (int i = 0; i < vertices.size(); i++) {
                        if (vertices.get(i).getId() == te.getSrcVertex().getId()) {
                                sourceVertexIndex = i;
                        }
                        if (vertices.get(i).getId() == te.getDestVertex().getId()) {
                                destVertexIndex = i;
                        }
                }
                if (sourceVertexIndex == Integer.MIN_VALUE || destVertexIndex == Integer.MIN_VALUE) {
                        throw new IllegalStateException(String.format("Edge Id : %d has missing source or " +
                                                                              "destination vertix information",
                                                                      te.getId()));
                }
                edges.put(e.getId(), new int[] {sourceVertexIndex, destVertexIndex});

                Map.Entry<Long, LongSet> prevEdgesEntry =  edgesAtTime.floorEntry(timestamp);
                LongSet edgeIds = new LongOpenHashSet();

                if (prevEdgesEntry != null) {
                        edgeIds.addAll(prevEdgesEntry.getValue());
                }
                edgeIds.add(e.getId());
                edgesAtTime.put(timestamp, edgeIds);

                storeGraphEntityProps(te, propertiesByTimeForEdge, edgeProperties);
                storeVertexEdgeRelations(te, timestamp);*/
        }

        Vertex getVertexAtTime(final long vertexId, final long timestamp) {
                /*
                if (!vertices.containsKey(vertexId)) {
                        return null;
                }

                 */
                /*if (!vertexIdToInsertionPoint.containsKey(vertexId)) {
                        return null;
                }

                Map.Entry<Long, LongSet> vertexIdsAtTime = verticesAtTime.floorEntry(timestamp);
                if (vertexIdsAtTime ==  null) {
                        return null;
                }
                if (!vertexIdsAtTime.getValue().contains(vertexId)) {
                        return null;
                }
                TemporalVertex tv = (TemporalVertex) vertices.get(vertexIdToInsertionPoint.get(vertexId));

                TemporalVertex.TemporalVertexBuilder builder = tv.toBuilder();

                if (propertiesByTimeForVertex.containsKey(vertexId)) {
                        // get the properties associated with the vertex at the provided time
                        NavigableMap<Long, LongSet> vertexProps =  propertiesByTimeForVertex.get(vertexId);
                        Map.Entry<Long, LongSet> propEntry = vertexProps.floorEntry(timestamp);
                        HashSet<Property> finalProps = new HashSet<>();
                        propEntry.getValue().stream().forEach(propId -> {
                                if (properties.containsKey(propId)) {
                                        Property p = properties.get(propId);
                                        finalProps.add(p);
                                }
                        });
                        TreeMap<Long, Set<Property>> propertiesAtTime = new TreeMap<>();
                        propertiesAtTime.put(vertexIdsAtTime.getKey(), finalProps);
                        // builder.properties(propertiesAtTime);

                        HashMap<String, TreeMap<Long, Property>> propertiesByTime = new HashMap<>();

                        for (Property p : finalProps) {
                                TreeMap<Long, Property> propertyByTime = new TreeMap<>();
                                propertyByTime.put(timestamp, p);
                                propertiesByTime.put(p.getName(), propertyByTime);
                        }
                        // builder.properties(propertiesByTime);
                }
                return builder.build();*/
                return null;
        }

        Edge getEdgeAtTime(final long edgeId, final long timestamp) {
                return null;
                /*
                if (!edges.containsKey(edgeId)) {
                        return null;
                } */
                /*Map.Entry<Long, LongSet> edgeIdsAtTime = edgesAtTime.floorEntry(timestamp);
                if (edgeIdsAtTime == null) {
                        return null;
                }
                if (!edgeIdsAtTime.getValue().contains(edgeId)) {
                        return null;
                }
                TemporalEdge.TemporalEdgeBuilder builder = TemporalEdge.builder();

                // get the vertices corresponding to an edge id.
                int[] vertexIds = edges.get(edgeId);
                Vertex srcVertex = vertices.get(vertexIds[0]);
                Vertex dstVertex = vertices.get(vertexIds[1]);
                builder.srcVertex(srcVertex);
                builder.destVertex(dstVertex);

                // TODO: remove DRY violation with respect to persisting entity properties
                if (propertiesByTimeForEdge.containsKey(edgeId)) {
                        // get the properties associated with the vertex at the provided time
                        NavigableMap<Long, LongSet> edgeProps =  propertiesByTimeForEdge.get(edgeId);
                        Map.Entry<Long, LongSet> propEntry = edgeProps.floorEntry(timestamp);
                        Set<Property> finalProps = new HashSet<>();
                        propEntry.getValue().stream().forEach(propId -> {
                                if (properties.containsKey(propId)) {
                                        Property p = properties.get(propId);
                                        finalProps.add(p);
                                }
                        });
                        Long2ObjectAVLTreeMap<Set<Property>> propertiesAtTime = new Long2ObjectAVLTreeMap<>();
                        propertiesAtTime.put(edgeIdsAtTime.getKey(), finalProps);
                        // builder.allPropertiesWithTime(propertiesAtTime);

                        HashMap<String, TreeMap<Long, Property>> propertiesByTime = new HashMap<>();

                        for (Property p : finalProps) {
                                TreeMap<Long, Property> propertyByTime = new TreeMap<>();
                                propertyByTime.put(timestamp, p);
                                propertiesByTime.put(p.getName(), propertyByTime);
                        }
                        // builder.allPropertiesByTime(propertiesByTime);
                }

                return builder.build();*/
        }

        Property getEdgePropertyAtTime(final long edgeId, final String propertyName, final long timestamp) {

                return getPropertyAtTime(edgeId, propertyName, timestamp, propertiesByTimeForEdge, edgeProperties);
        }

        Property getVertexPropertyAtTime(final long vertexId, final String propertyName, final long timestamp) {

                return getPropertyAtTime(vertexId, propertyName, timestamp, propertiesByTimeForVertex, vertexProperties);
        }

        @VisibleForTesting
        /*
        Map<Long, Vertex> getVertices() {
                return vertices;
        }*/

        ObjectArrayList<Vertex> getVertices() {
                return vertices;
        }

        @VisibleForTesting
        Map<Long, int[]> getEdges() {
                return edges;
        }

        @VisibleForTesting
        Map<Integer, Property> getProperties() {
                return properties.getProperties();
        }

        @VisibleForTesting
        NavigableMap<Long, LongSet> getVerticesAtTime() {
                return verticesAtTime;
        }

        @VisibleForTesting
        NavigableMap<Long, LongSet> getEdgesAtTime() {
                return edgesAtTime;
        }

        /*
        @VisibleForTesting
        NavigableMap<Long, LongSet> getPropertiesAtTime() {
                return propertiesAtTime;
        }

         */

        @VisibleForTesting
        Map<Long, TreeMap<Long, LongSet>> getOutgoingEdgesByTimeForVertex() {
                return outgoingEdgesByTimeForVertex;
        }

        @VisibleForTesting
        Map<Long, TreeMap<Long, LongSet>> getIncomingEdgesByTimeForVertex() {
                return incomingEdgesByTimeForVertex;
        }

        @VisibleForTesting
        Map<Long, TreeMap<Long, LongSet>> getPropertiesByTimeForVertex() {
                return propertiesByTimeForVertex;
        }

        @VisibleForTesting
        Map<Long, TreeMap<Long, LongSet>> getPropertiesByTimeForEdge() {
                return propertiesByTimeForEdge;
        }

        @VisibleForTesting
        Map<Long, TreeMap<Long, IdPair>> getVerticesByTimeForEdge() {
                return verticesByTimeForEdge;
        }

        @VisibleForTesting
        Map<Long, HashMap<Long, Set<Long>>> getVertexOutgoingEdgesAtTime() {
                return vertexOutgoingEdgesAtTime;
        }

        @VisibleForTesting
        Map<Long, HashMap<Long, Set<Long>>> getVertexIncomingEdgesAtTime() {
                return vertexIncomingEdgesAtTime;
        }

        @VisibleForTesting
        Map<Long, HashMap<Long, Set<Long>>> getVertexPropertiesAtTime() {
                return vertexPropertiesAtTime;
        }

        @VisibleForTesting
        Map<Long, HashMap<Long, Set<Long>>> getEdgePropertiesAtTime() {
                return edgePropertiesAtTime;
        }

        @VisibleForTesting
        Map<Long, HashMap<Long, Set<Long>>> getEdgeVerticesAtTime() {
                return edgeVerticesAtTime;
        }

        @VisibleForTesting
        Long2IntOpenHashMap getVertexIdToInsertionPoint() {
                return this.vertexIdToInsertionPoint;
        }

        @VisibleForTesting
        Map<Long, IntOpenHashSet> getEdgeProperties() {
                return edgeProperties;
        }

        @VisibleForTesting
        Map<Long, IntOpenHashSet> getVertexProperties() {
                return vertexProperties;
        }


        private void storeGraphEntityProps(PropertyAwareEntity entity,
                                           Map<Long, TreeMap<Long, LongSet>> propertiesByTimeForEntity,
                                           Int2ObjectOpenHashMap<LongOpenHashSet> entityProperties) {

                // check if the entity exists. If yes, then the current property set
                // should build upon the historical property set.
                TreeMap<Long, LongSet> entityPropsByTime = propertiesByTimeForEntity.getOrDefault(entity.getId(),
                                                                                                    new TreeMap<>());

                LongOpenHashSet propertiesForEntity = entityProperties.getOrDefault(entity.getId(),
                                                                             new LongOpenHashSet());

                TreeMap<Long, Set<Property>> allProps = new TreeMap<>(); // entity
                // .getAllPropertiesWithTime();
                for (Map.Entry<Long, Set<Property>> propertyTimeEntry : allProps.entrySet()) {
                        // record properties. also record the historical properties along with
                        // the current ones

                        Set<Property> props = propertyTimeEntry.getValue();
                        props.forEach((property) -> {
                                // if the property already exists then
                                // add the passed in property values
                                // to the values already existing.
                                if (properties.containsKey(property.getId())) {
                                        Property p = properties.get(property.getId());
                                        p.setValueAtTime(propertyTimeEntry.getKey(),
                                                         property.getValueAtTime(propertyTimeEntry.getKey()));
                                        properties.put(property.getId(), p);
                                } else {
                                        properties.put(property.getId(), property);
                                }
                                propertiesForEntity.add(property.getId());
                        });
                }
                entityProperties.put(entity.getId(), propertiesForEntity);
        }

        /*private void storeVertexEdgeRelations(final TemporalEdge te, final long timestamp) {
                Vertex srcVertex = te.getSrcVertex();
                Vertex dstVertex = te.getDestVertex();

                storeEdgeRelationInDirection(te, timestamp, srcVertex, outgoingEdgesByTimeForVertex);
                storeEdgeRelationInDirection(te, timestamp, dstVertex, incomingEdgesByTimeForVertex);
        }*/

        /*private void storeEdgeRelationInDirection(TemporalEdge te, long timestamp, Vertex vertex,
                                                  Map<Long, TreeMap<Long, LongSet>> edgesForVertexInDirection) {
                TreeMap<Long, LongSet> edgesInDirectionAtTime =
                        edgesForVertexInDirection.getOrDefault(vertex.getId(), new TreeMap<>());
                Map.Entry<Long, LongSet> prevEdgesInDirection = edgesInDirectionAtTime.floorEntry(timestamp);
                LongSet currentEdgesInDirection = new LongOpenHashSet();
                if (prevEdgesInDirection != null) {
                        currentEdgesInDirection.addAll(prevEdgesInDirection.getValue());
                }
                currentEdgesInDirection.add(te.getId());
                edgesInDirectionAtTime.put(timestamp, currentEdgesInDirection);
                edgesForVertexInDirection.put(vertex.getId(), edgesInDirectionAtTime);
        }*/

        private Property getPropertyAtTime(long entityId, String propertyName, long timestamp,
                                           final Map<Long, TreeMap<Long, LongSet>> propertiesByTimeForEntity,
                                           final Map<Long, IntOpenHashSet> entityProperties) {
                /*
                if (!propertiesByTimeForEntity.containsKey(entityId)) {
                        return null;
                }

                 */
                if (!entityProperties.containsKey(entityId)) {
                        return null;
                }
                IntOpenHashSet propertySet = entityProperties.get(entityId);

                // get the property corresponding to the specified name
                Property targetProperty = null;

                for (Integer propertyId : propertySet) {
                        if (!propertyName.equals(properties.get(propertyId).getName())) {
                             continue;
                        }
                        targetProperty = properties.get(propertyId);
                        break;
                }

                return targetProperty;
                /*
                Map.Entry<Long, LongSet> propertiesByTime = propertiesByTimeForEntity.get(entityId).floorEntry(timestamp);
                if (propertiesByTime == null) {
                        return null;
                }

                // currently there is no reverse index stored to map the property name and the property id
                // at a specific point in time. Hence we loop through all the properties found
                // at a particular point in time and filter the one with the name equal to the specified
                // name.
                // TODO; prahaladd - analyze the memory impact of maintaining a reverse index between the
                // the property name and the corresponding property in a time series based fashion.
                Set<Long> filtered = propertiesByTime.getValue().stream().filter(pid -> {
                        return (properties.containsKey(pid) && properties.get(pid).getName().equals(propertyName));
                }).collect(Collectors.toSet());

                if (filtered.size() == 0) {
                        return null;
                }
                // if there is more than one property, it is an error since
                // every edge can have only one property with a specific name
                if (filtered.size() > 1) {
                        throw new IllegalStateException(String.format("Edge Id : %d has multiple properties with" +
                                                                              " the same name : %s", entityId,
                                                                      propertyName));
                }
                return properties.get(filtered.iterator().next());

                 */
        }


        @Data
        @AllArgsConstructor
        @EqualsAndHashCode
        @Builder(toBuilder = true)
        public static class IdPair {
                private final long srcEntityId;
                private final long dstEntityId;
        }
}
