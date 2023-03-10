package model;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;

/**
 * An implementation of the {@link Property}.
 * Maintains the property time series within a {@link TreeMap} instance
 * in a chronological ascending order.
 */
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode
public class TemporalProperty  implements  Property {
    private final int id;

    @EqualsAndHashCode.Exclude
    private final String name;

    @EqualsAndHashCode.Exclude
    private final long time;

    @EqualsAndHashCode.Exclude
    private long latestTimestamp ;

    @Builder.Default
    private final ArrayList<TimeStampWithValue> values = new ArrayList<>();


    @Override
    public Object getValueAtTime(long timestamp) {
       if (this.time == 0L) {
           throw new IllegalStateException("Property create time is required but not set.");
       }
       return floorValue(timestamp);
    }

    @Override
    public void setValueAtTime(long timestamp, Object value) {
        if (this.time == 0L) {
            throw new IllegalStateException("Property create time is required but not set.");
        }
        if (timestamp < latestTimestamp) {
            throw new IllegalArgumentException(String.format("Incoming timestamp: %d for property %s is less " +
                                                                     "than last known timestamp: %d", timestamp,
                                                             name, latestTimestamp));
        }
        int timeDifferential = (int)(timestamp - this.time);
        TimeStampWithValue tsv = new TimeStampWithValue(timeDifferential, value);
        values.add(tsv);
        latestTimestamp = timestamp;
    }

    @Override
    public boolean purgeTimeSeriesUntilTime(final long timestamp) {
        int floorIndex = floorIndex(timestamp);
        // no values removed from the series
        if (-1 == floorIndex)
            return false;
        values.subList(0, floorIndex+1).clear();
        return true;
    }

    private int floorIndex(final long timestamp) {

        int floorEntry = Integer.MIN_VALUE;
        int baseline = (int)(timestamp - this.time);
        int left = 0;
        int right = values.size();

        // return null in case if the time is less thant the first value
        if (baseline < values.get(left).timeDifferential) {
            return -1;
        }
        while (left < (right - 1)) {
            int mid = (left + (right)) / 2;
            int currentMid = values.get(mid).timeDifferential;
            if (currentMid == baseline) {
                return mid;
            }
            if (currentMid > baseline) {
                right  = mid;
            } else {
                floorEntry = Math.max(floorEntry, currentMid);
                left = mid;
            }
        }
        return left;
    }

    private Object floorValue(final long timestamp) {
        final int floorIndex = floorIndex(timestamp);
        if (-1 == floorIndex) {
            return null;
        }
        return values.get(floorIndex).object;
    }

    public static class TemporalPropertyBuilder {
        public TemporalProperty build() {
            if (id == 0L) {
                id = Math.abs(new Random().nextInt());
            }
            if (null == name || name.isEmpty()) {
                throw new IllegalArgumentException(String.format("Name is null or empty"));
            }
            if (this.time <= 0L) {
                throw new IllegalArgumentException(String.format("Time parameter is invalid"));
            }

            return new TemporalProperty(id, name, time, latestTimestamp, (this.values$value == null) ? new ArrayList<>() :
                    this.values$value);
        }
    }

    @Data
    @AllArgsConstructor
    public static final class TimeStampWithValue {
        private final int timeDifferential;
        private final Object object;
    }

}
