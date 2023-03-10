package model;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class TemporalPropertyTests {

    @Test
    void getValueAtTime() {
        TemporalProperty.TemporalPropertyBuilder b = TemporalProperty.builder();
        final long time = System.currentTimeMillis();
        b.id(Math.abs(new Random().nextInt()));
        b.time(time).name("bandwidth");

        Vertex v = Mockito.mock(Vertex.class);

        TemporalProperty prop = b.build();

        prop.setValueAtTime(time, 1L);
        prop.setValueAtTime(time + 1L, 2L);
        prop.setValueAtTime(time + 10L, 50L);

        assertEquals(1L, prop.getValueAtTime(time));
        assertEquals(2L, prop.getValueAtTime(time + 4L));
        assertEquals(50L, prop.getValueAtTime(time + 12L));
    }

    @Test
    void getValueAtTimeInitTimeNotSet() {
        TemporalProperty.TemporalPropertyBuilder b = TemporalProperty.builder();
        final long time = System.currentTimeMillis();
        int id = Math.abs(new Random().nextInt());
        System.out.printf("ID: %s", id);
        b.id(id).time(time).name("bandwidth");
        Vertex v = Mockito.mock(Vertex.class);
        TemporalProperty prop = b.build();

        prop.setValueAtTime(time, 1L);
        prop.setValueAtTime(time + 1L, 2L);
        prop.setValueAtTime(time + 10L, 50L);

        assertEquals(1L, prop.getValueAtTime(time));
        assertEquals(2L, prop.getValueAtTime(time + 4L));
        assertEquals(50L, prop.getValueAtTime(time + 12L));
    }

    void setValueAtTimeInitTimeNotSet() {
        TemporalProperty.TemporalPropertyBuilder b = TemporalProperty.builder();
        final long time = System.currentTimeMillis();
        b.id(new Random().nextInt());

        Vertex v = Mockito.mock(Vertex.class);

        TemporalProperty prop = b.build();

        prop.setValueAtTime(time, 1L);
        prop.setValueAtTime(time + 1L, 2L);
        prop.setValueAtTime(time + 10L, 50L);

        assertEquals(1L, prop.getValueAtTime(time));
        assertEquals(2L, prop.getValueAtTime(time + 4L));
        assertEquals(50L, prop.getValueAtTime(time + 12L));
    }

    @Test
    void setValueAtTimeInitTimeOutOfOrder() {
        TemporalProperty.TemporalPropertyBuilder b = TemporalProperty.builder();
        final long time = System.currentTimeMillis();
        b.id(new Random().nextInt()).name("Test property").time(time);

        Vertex v = Mockito.mock(Vertex.class);

        TemporalProperty prop = b.build();

        prop.setValueAtTime(time, 1L);
        prop.setValueAtTime(time + 1L, 2L);
        prop.setValueAtTime(time + 10L, 50L);

        assertEquals(1L, prop.getValueAtTime(time));
        assertEquals(2L, prop.getValueAtTime(time + 4L));
        assertEquals(50L, prop.getValueAtTime(time + 12L));

        assertThrows(IllegalArgumentException.class, () -> prop.setValueAtTime(time + 8L, 100L));
    }

    @Test
    void purgePropertyValuesTillTime() {
        TemporalProperty.TemporalPropertyBuilder b = TemporalProperty.builder();
        final long time = System.currentTimeMillis();
        b.id(new Random().nextInt()).name("Test property").time(time);

        TemporalProperty prop = b.build();

        prop.setValueAtTime(time, 1L);
        prop.setValueAtTime(time + 1L, 2L);
        prop.setValueAtTime(time + 10L, 50L);
        prop.setValueAtTime(time + 14L, 70L);
        prop.setValueAtTime(time + 18L, 90L);
        prop.setValueAtTime(time + 20L, 101L);

        prop.purgeTimeSeriesUntilTime(time - 1L);
        assertEquals(1L, prop.getValueAtTime(time));
        assertEquals(2L, prop.getValueAtTime(time + 4L));
        assertEquals(50L, prop.getValueAtTime(time + 12L));

        prop.purgeTimeSeriesUntilTime(time + 12L);
        assertEquals(null, prop.getValueAtTime(time));
        assertEquals(null, prop.getValueAtTime(time + 11L));
        assertEquals(null, prop.getValueAtTime(time + 13L));
        assertEquals(70L, prop.getValueAtTime(time + 14L));
        assertThrows(IllegalArgumentException.class, () -> prop.setValueAtTime(time + 8L, 100L));
    }
}