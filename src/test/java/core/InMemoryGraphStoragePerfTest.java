package core;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

import model.Property;
import model.TemporalProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

class InMemoryGraphStoragePerfTest {

    private static final Set<String> VERTEX_PROPETIES = new HashSet<>();
    private static final Set<String> EDGE_PROPERTIES = new HashSet<>();
    private static final Map<String, Class> EDGE_PROPERTIES_WITH_TYPE = new HashMap<>();

    // test parameters are driven using environment variables.
    private static final String ENV_PROPERTY_RESOLUTION_MINS = "resolutionMins";
    private static final Integer DEFAULT_RESOULTION_MINS =  5;

    // number of vertices to be created
    private static final Integer[] NUM_VERTICES = new Integer[] {73000};
    private static final Integer[] NUM_EDGES =  new Integer[] {20000000};

    // the percentage of vertices that talk to each other.
    private static final Integer EDGE_PCT = 75;

    static {
        VERTEX_PROPETIES.add("Name");
        VERTEX_PROPETIES.add("IPAddress");
        EDGE_PROPERTIES.add("PacketCount");
        EDGE_PROPERTIES.add("Bandwidth");
        EDGE_PROPERTIES.add("Volume");
        EDGE_PROPERTIES.add("SessionCount");

        // initialize the edge properties with type hashmap
       /*
       EDGE_PROPERTIES_WITH_TYPE.put("PacketCount", Long.class);

       EDGE_PROPERTIES_WITH_TYPE.put("Bandwidth", Long.class);
       EDGE_PROPERTIES_WITH_TYPE.put("Volume", Long.class);
       EDGE_PROPERTIES_WITH_TYPE.put("SessionCount", Long.class);

        */
       EDGE_PROPERTIES_WITH_TYPE.put("SessionCount", Integer.class);



    }

    private Random random;
    private Integer resolution;
    private TemporalGraph underTest;
    private long initTs;

    @BeforeEach
    public void init() {
        this.random = new Random();
        String resFromProperty = System.getProperty(ENV_PROPERTY_RESOLUTION_MINS);
        this.resolution = resFromProperty != null ? Integer.parseInt(resFromProperty) : DEFAULT_RESOULTION_MINS;
        this.initTs = System.currentTimeMillis();
        this.underTest = new TemporalGraph(initTs);
    }


   // @Test
    public void memoryConsumption() throws InterruptedException {
        populateGraph();
        System.out.println("Ready for heap dump process ...");
        Thread.sleep(240000);
        System.out.println("done");
    }

    private void populateGraph() {
        List<Integer> generatedVertexIds = addVertices();
        long time;

        List<Integer> generatedEdgeIds = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int  i = 0; i < NUM_EDGES.length; i++) {
            int numEdges =  NUM_EDGES[i];
            time = System.currentTimeMillis();
            // pick the vertex and generate edges to every other vertex
            for (int j = 0; j < numEdges; j++) {
                int srcVertexIndex = random.nextInt(generatedVertexIds.size());
                int destVertexIndex = random.nextInt(generatedVertexIds.size());
                if (srcVertexIndex == destVertexIndex) {
                    continue;
                }
                Set<Property> properties = new HashSet<>();
                /*
                for (Map.Entry<String, Class> e : EDGE_PROPERTIES_WITH_TYPE.entrySet()) {
                    Property p = createPropertyTimeSeries(e.getValue(), e.getKey(), time);
                    properties.add(p);
                }*/
                final long timeBeforeEdgeAdd =  System.currentTimeMillis();
                int edgeId = underTest.addEdge(srcVertexIndex, destVertexIndex, properties, time);
                final long timeAfterEdgeAdd = System.currentTimeMillis();
                generatedEdgeIds.add(edgeId);
                // System.out.println("TotalEdges : " + generatedEdgeIds.size() + "; Single edge add time : " + (timeAfterEdgeAdd - timeBeforeEdgeAdd) + "; Total Elapsed time : " + (timeAfterEdgeAdd - startTime));
            }
        }
        long completionTime = System.currentTimeMillis();

        System.out.println(String.format("Num Vertices : %d, Num Edges : %d, Time elapsed (ms) : %d",
                                         generatedVertexIds.size(),
                                         generatedEdgeIds.size(), (completionTime - startTime)));
    }



    private List<Integer> addVertices() {
        List<Integer> generatedVertexIds = new ArrayList<>();
        long time = System.currentTimeMillis();
        for (int i = 0; i < NUM_VERTICES.length; i++) {
            final int numVertex = NUM_VERTICES[i];
            for (int j = 1; j <= numVertex; j++) {

                Property p = createProperty("Name", time);
                p.setValueAtTime(time, String.format("VirtualMachine_%d", j));

                Property p1 = createProperty("IPAddress", time);
                p1.setValueAtTime(time, String.format("IpAddress_%d", j));
                int vertexId = underTest.addVertex(Collections.singleton(p), time);
                generatedVertexIds.add(vertexId);
            }
        }
        long completionTime = System.currentTimeMillis();
        System.out.printf("Num vertices : %d, Time elapsed (ms): %d\n", generatedVertexIds.size(),
                          (completionTime - time));
        return generatedVertexIds;
    }

    @Test
    public void floorValue() {
        long[] values = new long[11];
        for (int  i = 0; i <= 100; i = i + 10) {
            values[i/10] = i;
        }

        long floorEntry = Integer.MIN_VALUE;
        long baseline = 40L;
        int left = 0;
        int right = values.length;

        while (left < (right - 1)) {
            int mid = (left + (right)) / 2;
            if (values[mid] == baseline) {
                break;
            }
            if (values[mid] > baseline) {
                right  = mid;
            } else {
                floorEntry = Math.max(floorEntry, values[mid]);
                left = mid;
            }
        }
        System.out.println("Done");
    }

    @Test
    public void  currentTimeLessThanMaxInt() {
        ZonedDateTime today  = ZonedDateTime.parse("2022-02-28T00:00:00+05:30");
        ZonedDateTime history = ZonedDateTime.parse("2022-02-21T00:00:00+05:30");
        long diff = (today.toInstant().toEpochMilli() - history.toInstant().toEpochMilli());
        long intRep = (long) Long.valueOf(diff).intValue();
        System.out.println("Precision loss : " + (diff != intRep));
        System.out.println("Assumption = current time is less than Int MAX VALUE : " + (diff < System.currentTimeMillis()));
    }

    private Property createPropertyTimeSeries(final Class valueType,
                                              final String name,
                                              final long startTime) {
        Property p = createProperty(name, startTime);
        if (valueType ==  Long.class) {
            LongPropertyTimeSeriesGenerator generator = new LongPropertyTimeSeriesGenerator();
            Map<Long, Long> valueTimeSeries = generator.getValueTimeSeries(this.resolution, startTime);
            for (Map.Entry<Long, Long> value : valueTimeSeries.entrySet()) {
                p.setValueAtTime(value.getKey(), value.getValue());
            }
            return p;
        }
        if (valueType ==  Integer.class) {
            IntegerPropertyTimeSeriesGenerator generator = new IntegerPropertyTimeSeriesGenerator();
            Map<Long, Integer> valueTimeSeries = generator.getValueTimeSeries(this.resolution, startTime);
            for (Map.Entry<Long, Integer> value : valueTimeSeries.entrySet()) {
                p.setValueAtTime(value.getKey(), value.getValue());
            }
            return p;
        }
        throw  new IllegalArgumentException(String.format("Property of type : %s is not supported",
                                                          valueType.getName()));
    }



    private Property createProperty(final String name,
                                    final long initTime) {
        return TemporalProperty.builder().name(name).time(initTime).build();
    }

    private long getPropertyId(final long entityId, final long propertyId) {
       return Objects.hash(entityId, propertyId);
    }

    private static  final class Value {
        long timestamp;
        long value;
    }
}