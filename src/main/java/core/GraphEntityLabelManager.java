package core;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import java.util.Objects;

class GraphEntityLabelManager {

    private final BidiMap<String, Long> nameToIdMap = new DualHashBidiMap<>();

    Long addLabel(final String label) {
        if (nameToIdMap.containsKey(label)) {
            throw new IllegalArgumentException(String.format("Label %s already exists. Labels names must be " +
                                                                     "unique in the edge and vertex space", label));
        }
        long labelId = Objects.hash(System.currentTimeMillis(), label);
        nameToIdMap.put(label, labelId);
        return labelId;
    }

    String getLabel(final long labelId) {
        return nameToIdMap.getKey(labelId);
    }

    void deleteLabel(final String label) {
        nameToIdMap.remove(label);
    }

}
