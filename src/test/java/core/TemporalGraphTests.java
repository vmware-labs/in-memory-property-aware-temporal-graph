package core;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import exceptions.PropertyNotFoundException;
import model.TemporalProperty;
import model.TimestampedPropertyValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

public class TemporalGraphTests {
    private TemporalGraph underTest;
    private Random random;
    private static final long initTime = System.currentTimeMillis();
    private static final String property1 = "Bandwidth";
    private static final String property2 = "cpuUsage";
    private static final String property3 = "packetCount";

    @BeforeEach
    void init() {
        underTest = new TemporalGraph(initTime);
        random = new Random();
    }

    @Test
    void testAddProperty() {
        int  id = Math.abs(random.nextInt());
        long nowTs = initTime + TimeUnit.MINUTES.toMillis(5);
        // out of order property addition
        // no name present exception
        // no time present exception
        {
            assertThrows(IllegalArgumentException.class, () -> TemporalProperty.builder().id(id).time(initTime).build());
            assertThrows(IllegalArgumentException.class, () -> TemporalProperty.builder().name(property1).id(id).build());
            TemporalProperty prop1 = TemporalProperty.builder().name(property1).id(id).time(initTime).build();
            prop1.setValueAtTime(nowTs, 100L);
            prop1.setValueAtTime(nowTs + 15 * TimeUnit.MINUTES.toMillis(5), 100L);
            assertThrows(IllegalArgumentException.class, () -> prop1.setValueAtTime(nowTs + 10 * TimeUnit.MINUTES.toMillis(5), 100L));
        }
        // getting property at a time
        {
            TemporalProperty prop1 = TemporalProperty.builder().name(property1).id(id).time(initTime).build();
            prop1.setValueAtTime(nowTs, 100L);
            prop1.setValueAtTime(nowTs + 15 * TimeUnit.MINUTES.toMillis(5), 300L);
            prop1.setValueAtTime(nowTs + 30 * TimeUnit.MINUTES.toMillis(5), 200L);
            long val = (long) prop1.getValueAtTime(nowTs + 10 * TimeUnit.MINUTES.toMillis(5));
            assertEquals(100, val);
            val = (long) prop1.getValueAtTime(nowTs);
            assertEquals(100, val);
            assertNull(prop1.getValueAtTime(nowTs - 1L));
            System.out.println("val: "+val);
            val = (long) prop1.getValueAtTime(nowTs + 31 * TimeUnit.MINUTES.toMillis(5));
            assertEquals(200, val);
            val = (long) prop1.getValueAtTime(nowTs + 16 * TimeUnit.MINUTES.toMillis(5));
            assertEquals(300, val);
        }
    }

    @Test
    void testAddVertex() throws PropertyNotFoundException {
        int id = Math.abs(random.nextInt());
        long nowTs = initTime + TimeUnit.MINUTES.toMillis(5);
        //test all Exceptions
        {
            TemporalProperty prop1 = TemporalProperty.builder().name(property1).id(id).time(initTime).build();
            assertThrows(IllegalArgumentException.class, () -> underTest.addVertex(Sets.newHashSet(prop1), nowTs));
        }
        //add vertex
        {
            int idProp1 =  getRandomId();
            TemporalProperty prop1 = TemporalProperty.builder().name(property1).id(idProp1).time(nowTs).build();
            prop1.setValueAtTime(nowTs + 5, 100L);
            prop1.setValueAtTime(nowTs + 15 * TimeUnit.MINUTES.toMillis(5), 300L);
            prop1.setValueAtTime(nowTs + 30 * TimeUnit.MINUTES.toMillis(5), 200L);

            int idProp2 = getRandomId();
            TemporalProperty prop2 = TemporalProperty.builder().name(property2).id(idProp2).time(nowTs).build();
            prop2.setValueAtTime(nowTs + 10, 600L);
            prop2.setValueAtTime(nowTs + 20 * TimeUnit.MINUTES.toMillis(5), 700L);
            prop2.setValueAtTime(nowTs + 40 * TimeUnit.MINUTES.toMillis(5), 900L);
            int vertexId = underTest.addVertex(Sets.newHashSet(prop1, prop2), nowTs);
            assertTrue(vertexId >= 0);
            Iterator vertices =  underTest.getVerticesAtTime(nowTs);
            assertNotNull(vertices);
            assertTrue(vertices.hasNext());
            assertEquals(2, Iterators.size(vertices));
            vertices = underTest.getVerticesAtTime(nowTs - 10L);
            assertNotNull(vertices);
            assertEquals(0, Iterators.size(vertices));

            int idProp3 = getRandomId();
            TemporalProperty prop3 =
                    TemporalProperty.builder().name(property1).id(idProp3).time(nowTs + TimeUnit.MINUTES.toMillis(5)).build();
            prop3.setValueAtTime(nowTs + 10 * TimeUnit.MINUTES.toMillis(5), 400L);
            prop3.setValueAtTime(nowTs + 15 * TimeUnit.MINUTES.toMillis(5), 600L);
            prop3.setValueAtTime(nowTs + 30 * TimeUnit.MINUTES.toMillis(5), 200L);

            int idProp4 = getRandomId();
            TemporalProperty prop4 =
                    TemporalProperty.builder().name(property2).id(idProp4).time(nowTs + TimeUnit.MINUTES.toMillis(10)).build();
            prop4.setValueAtTime(nowTs + 4 * TimeUnit.MINUTES.toMillis(5), 400L);
            prop4.setValueAtTime(nowTs + 8 * TimeUnit.MINUTES.toMillis(5), 600L);
            prop4.setValueAtTime(nowTs + 12 * TimeUnit.MINUTES.toMillis(5), 200L);

            int vertexId1 = underTest.addVertex(Sets.newHashSet(prop3, prop4), nowTs + 5);
            assertTrue(vertexId1 >= 0);
            vertices =  underTest.getVerticesAtTime(nowTs);
            assertNotNull(vertices);
            assertEquals(2, Iterators.size(vertices));
            vertices =  underTest.getVerticesAtTime(nowTs + 6);
            assertNotNull(vertices);
            assertEquals(3, Iterators.size(vertices));
            vertices = underTest.getVerticesAtTime(nowTs - 5);
            assertNotNull(vertices);
            assertEquals(0, Iterators.size(vertices));
            Set<TimestampedPropertyValue> properties = (Set) underTest.getVertexPropertiesAtTime(vertexId, nowTs + 10);
            checkPropertyValues(properties, 2, 100L, 600L);
            properties = (Set) underTest.getVertexPropertiesAtTime(vertexId, nowTs + 1);
            assertNotNull(properties);
            assertEquals(0, properties.size());
            properties = (Set) underTest.getVertexPropertiesAtTime(vertexId, nowTs + 50 * TimeUnit.MINUTES.toMillis(5));
            assertNotNull(properties);
            assertEquals(2, properties.size());
            TimestampedPropertyValue p = underTest.getVertexPropertyAtTime(vertexId1, property1, nowTs + 11 * TimeUnit.MINUTES.toMillis(5));
            assertNotNull(p);
            assertEquals(400L, p.getValue());
            p = underTest.getVertexPropertyAtTime(vertexId1, property1, nowTs + 9 * TimeUnit.MINUTES.toMillis(5));
            assertNull(p);
            p = underTest.getVertexPropertyAtTime(vertexId1, property1+"1", nowTs + 9 * TimeUnit.MINUTES.toMillis(5));
            assertNull(p);
            assertThrows(PropertyNotFoundException.class, () -> underTest.addVertexProperty(vertexId1, property1+"1", 100L, nowTs));
            assertThrows(IllegalArgumentException.class, () -> underTest.addVertexProperty(vertexId1, property1, 100L, nowTs + 9 * TimeUnit.MINUTES.toMillis(5)));
            underTest.addVertexProperty(vertexId1, property1, 1000L, nowTs + 31 * TimeUnit.MINUTES.toMillis(5));
            p = underTest.getVertexPropertyAtTime(vertexId1, property1, nowTs + 32 * TimeUnit.MINUTES.toMillis(5));
            assertNotNull(p);
            assertEquals(1000L, p.getValue());
        }
    }

    @Test
    void testAddEdge() throws PropertyNotFoundException {
        long nowTs = initTime + TimeUnit.MINUTES.toMillis(5);

        int idProp1 = getRandomId();
        TemporalProperty prop1 = TemporalProperty.builder().name(property1).id(idProp1).time(nowTs).build();
        prop1.setValueAtTime(nowTs + 5, 100L);
        prop1.setValueAtTime(nowTs + 15 * TimeUnit.MINUTES.toMillis(5), 300L);
        prop1.setValueAtTime(nowTs + 30 * TimeUnit.MINUTES.toMillis(5), 200L);

        int idProp2 = getRandomId();
        TemporalProperty prop2 = TemporalProperty.builder().name(property2).id(idProp2).time(nowTs).build();
        prop2.setValueAtTime(nowTs + 10, 600L);
        prop2.setValueAtTime(nowTs + 20 * TimeUnit.MINUTES.toMillis(5), 700L);
        prop2.setValueAtTime(nowTs + 40 * TimeUnit.MINUTES.toMillis(5), 900L);

        int idProp3 = getRandomId();
        TemporalProperty prop3 =
                TemporalProperty.builder().name(property1).id(idProp3).time(nowTs + TimeUnit.MINUTES.toMillis(5)).build();
        prop3.setValueAtTime(nowTs + 10 * TimeUnit.MINUTES.toMillis(5), 400L);
        prop3.setValueAtTime(nowTs + 15 * TimeUnit.MINUTES.toMillis(5), 600L);
        prop3.setValueAtTime(nowTs + 30 * TimeUnit.MINUTES.toMillis(5), 200L);

        int idProp4 = getRandomId();
        TemporalProperty prop4 =
                TemporalProperty.builder().name(property2).id(idProp4).time(nowTs + TimeUnit.MINUTES.toMillis(10)).build();
        prop4.setValueAtTime(nowTs + 4 * TimeUnit.MINUTES.toMillis(5), 400L);
        prop4.setValueAtTime(nowTs + 8 * TimeUnit.MINUTES.toMillis(5), 600L);
        prop4.setValueAtTime(nowTs + 12 * TimeUnit.MINUTES.toMillis(5), 200L);

        int idPropEdge = getRandomId();
        TemporalProperty propEdge =
                TemporalProperty.builder().name(property2).id(idPropEdge).time(nowTs + TimeUnit.MINUTES.toMillis(10)).build();
        propEdge.setValueAtTime(nowTs + 3 * TimeUnit.MINUTES.toMillis(5), 400L);
        propEdge.setValueAtTime(nowTs + 6 * TimeUnit.MINUTES.toMillis(5), 600L);
        propEdge.setValueAtTime(nowTs + 11 * TimeUnit.MINUTES.toMillis(5), 200L);

        int idPropEdge1 = getRandomId();
        TemporalProperty propEdge1 =
                TemporalProperty.builder().name(property2).id(idPropEdge1).time(nowTs + 2 * TimeUnit.MINUTES.toMillis(5)).build();
        propEdge1.setValueAtTime(nowTs + 4 * TimeUnit.MINUTES.toMillis(5), 200L);
        propEdge1.setValueAtTime(nowTs + 6 * TimeUnit.MINUTES.toMillis(5), 400L);
        propEdge1.setValueAtTime(nowTs + 15 * TimeUnit.MINUTES.toMillis(5), 800L);
        // test exception / edge addition / getting an edge
        {
            int vertexId1 = underTest.addVertex(Sets.newHashSet(prop1, prop2), nowTs);
            int vertexId2 = underTest.addVertex(Sets.newHashSet(prop3, prop4), nowTs + 5);
            // null properties
            assertDoesNotThrow(() -> underTest.addEdge(vertexId1, vertexId2, null, nowTs));
            // Edge Addition time > property addition time
            assertThrows(IllegalArgumentException.class, () -> underTest.addEdge(vertexId1, vertexId2, Sets.newHashSet(propEdge), nowTs + TimeUnit.MINUTES.toMillis(11)));
            int edgeId = underTest.addEdge(vertexId1, vertexId2, Sets.newHashSet(propEdge), nowTs);
            assertTrue(edgeId >= 0);
            Iterator<Integer> edges = underTest.getEdgesAtTime(vertexId1, vertexId2, nowTs - 1);
            assertNotNull(edges);
            assertEquals(0, Iterators.size(edges));
            edges = underTest.getEdgesAtTime(vertexId2, vertexId1, nowTs);
            assertNotNull(edges);
            assertEquals(0, Iterators.size(edges));
            edges = underTest.getEdgesAtTime(vertexId1, vertexId2, nowTs);
            assertNotNull(edges);
            List<Integer> edgeList = Lists.newArrayList(edges);
            assertEquals(2, edgeList.size());
            assertTrue(new HashSet<Integer>(edgeList).contains(edgeId));
            int edgeId1 = underTest.addEdge(vertexId1, vertexId2, Sets.newHashSet(propEdge1), nowTs + 2);
            assertTrue(edgeId >= 0);
            edges = underTest.getEdgesAtTime(vertexId1, vertexId2, nowTs + 3);
            assertNotNull(edges);
            edgeList = Lists.newArrayList(edges);
            assertEquals(3, edgeList.size());
            Set<Integer> expectedEdges = new HashSet<>(Arrays.asList(edgeId, edgeId1));
            Set<Integer> actualEdgeSet = new HashSet<>(edgeList);
            expectedEdges.stream().forEach(e -> assertTrue(actualEdgeSet.contains(e)));

            assertEquals(2, Iterators.size(underTest.getInEdgesAtTime(vertexId2, nowTs)));
            assertEquals(3, Iterators.size(underTest.getInEdgesAtTime(vertexId2, nowTs + 2)));
            assertEquals(3, Iterators.size(underTest.getOutEdgesAtTime(vertexId1, nowTs + 4)));
            assertEquals(2, Iterators.size(underTest.getOutEdgesAtTime(vertexId1, nowTs)));
            assertEquals(0, Iterators.size(underTest.getOutEdgesAtTime(vertexId1, nowTs - 1)));

            // test edge properties
            Map<Integer, Collection<TimestampedPropertyValue>> properties = underTest.getEdgePropertiesAtTime(vertexId1, vertexId2, nowTs + 3);
            assertEquals(3, properties.size());
            checkPropertyValues(Sets.newHashSet(properties.get(edgeId)), 0, 0, 0);
            checkPropertyValues(Sets.newHashSet(properties.get(edgeId1)), 0, 0, 0);

            properties = underTest.getEdgePropertiesAtTime(vertexId1, vertexId2, nowTs + 3 * TimeUnit.MINUTES.toMillis(5));
            assertEquals(4, properties.size());
            checkPropertyValues(Sets.newHashSet(properties.get(edgeId)), 1, 0, 400L);
            checkPropertyValues(Sets.newHashSet(properties.get(edgeId1)), 0, 0, 0);

            properties = underTest.getEdgePropertiesAtTime(vertexId1, vertexId2, nowTs + 5 * TimeUnit.MINUTES.toMillis(5));
            assertEquals(4, properties.size());
            checkPropertyValues(Sets.newHashSet(properties.get(edgeId)), 1, 0, 400L);
            checkPropertyValues(Sets.newHashSet(properties.get(edgeId1)), 1, 0, 200L);

            properties = underTest.getEdgePropertiesAtTime(vertexId1, vertexId2, nowTs - 1L);
            assertEquals(0, properties.size());

            Map<Integer, TimestampedPropertyValue> propertyMap =
                    underTest.getEdgePropertyAtTime(vertexId1, vertexId2, property2, nowTs + 3 * TimeUnit.MINUTES.toMillis(5));
            assertEquals(4, propertyMap.size());
            checkPropertyValues(Sets.newHashSet(propertyMap.get(edgeId)), 1, 0, 400L);
            assertNull(propertyMap.get(edgeId1).getValue());

            propertyMap = underTest.getEdgePropertyAtTime(vertexId1, vertexId2, property2, nowTs + 5 * TimeUnit.MINUTES.toMillis(5));
            assertEquals(4, propertyMap.size());
            checkPropertyValues(Sets.newHashSet(propertyMap.get(edgeId)), 1, 0, 400L);
            checkPropertyValues(Sets.newHashSet(propertyMap.get(edgeId1)), 1, 0, 200L);

            assertThrows(IllegalArgumentException.class, () -> underTest.addEdgeProperty(edgeId, property2, 700L, nowTs + 8 * TimeUnit.MINUTES.toMillis(5)));
            underTest.addEdgeProperty(edgeId, property2, 700L, nowTs + 15 * TimeUnit.MINUTES.toMillis(5));
            TimestampedPropertyValue value = underTest.getEdgePropertyAtTime(edgeId, property2, nowTs + 16 * TimeUnit.MINUTES.toMillis(5));
            assertNotNull(value);
            assertEquals(700L, value.getValue());

            edges = underTest.getAllEdgesAtTime(nowTs + 15 * TimeUnit.MINUTES.toMillis(5));
            assertNotNull(edges);
            edgeList = Lists.newArrayList(edges);
            assertEquals(4, edgeList.size());

            underTest.purgeAtTime(nowTs + 10 * TimeUnit.MINUTES.toMillis(5));
            Collection<TimestampedPropertyValue> props = underTest.getVertexPropertiesAtTime(vertexId1, nowTs + 5 * TimeUnit.MINUTES.toMillis(5));
            assertTrue(props.isEmpty());
            props = underTest.getVertexPropertiesAtTime(vertexId1, nowTs + 21 * TimeUnit.MINUTES.toMillis(5));
            checkPropertyValues(Sets.newHashSet(props), 2, 300L, 700L);

            props = underTest.getVertexPropertiesAtTime(vertexId2, nowTs + 5 * TimeUnit.MINUTES.toMillis(5));
            assertTrue(props.isEmpty());
            props = underTest.getVertexPropertiesAtTime(vertexId2, nowTs + 16 * TimeUnit.MINUTES.toMillis(5));
            checkPropertyValues(Sets.newHashSet(props), 2, 600L, 200L);

            props = underTest.getEdgePropertiesAtTime(edgeId, nowTs + 6 * TimeUnit.MINUTES.toMillis(5));
            assertTrue(props.isEmpty());
            props = underTest.getEdgePropertiesAtTime(edgeId, nowTs + 12 * TimeUnit.MINUTES.toMillis(5));
            checkPropertyValues(Sets.newHashSet(props), 1, 0L, 200L);


            props = underTest.getEdgePropertiesAtTime(edgeId1, nowTs + 6 * TimeUnit.MINUTES.toMillis(5));
            assertTrue(props.isEmpty());
            props = underTest.getEdgePropertiesAtTime(edgeId1, nowTs + 16 * TimeUnit.MINUTES.toMillis(5));
            checkPropertyValues(Sets.newHashSet(props), 1, 0L, 800L);
        }
    }

    private void checkPropertyValues(Set<TimestampedPropertyValue> properties, int size, long p1Val, long p2Val) {
        assertNotNull(properties);
        assertEquals(size, properties.size());
        for (TimestampedPropertyValue p : properties) {
            if (p.getName().equals(property1)) {
                assertEquals(p1Val, p.getValue());
            }
            if (p.getName().equals(property2)) {
                assertEquals(p2Val, p.getValue());
            }
        }
    }

    private int getRandomId() {
        return Math.abs(random.nextInt());
    }

}
