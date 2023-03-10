package model;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class TimeSpace {

    private final Map<Long, Set<Long>> verticesByTime = new TreeMap<>();
    private final Map<Long, Set<Long>> edgesByTime = new TreeMap<>();
    private final Map<Long, Set<Long>> propertiesByTime = new TreeMap<>();

    public Set<Long> getVerticesAtTime(final long timestamp) {
        return Collections.emptySet();
    }

    public Set<Long> getEdgesAtTime(final long timestamp) {
        return Collections.emptySet();
    }

    public Set<Long> getPropertiesAtTime(final long timestamp) {
        return Collections.emptySet();
    }

    public void createOrUpdateGraphEntityAtTime(final GraphEntity entity, final long timestamp) {
        if (entity instanceof  Vertex) {
            addToSet(verticesByTime, entity.getId(), timestamp);
            return;
        }
        if (entity instanceof  Edge) {
            addToSet(edgesByTime, entity.getId(), timestamp);
            return;
        }
        if (entity instanceof  Property) {
            addToSet(propertiesByTime, entity.getId(), timestamp);
            return;
        }
        throw new IllegalArgumentException(String.format("Unrecognized entity type : %s",
                                                         entity.getClass().getCanonicalName()));
    }


    private void addToSet(Map<Long, Set<Long>> map, final long entityId, final long timestamp) {
        Set<Long> entityIds = map.getOrDefault(timestamp, Collections.emptySet());
        entityIds.add(entityId);
        map.put(timestamp, entityIds);
    }
}

