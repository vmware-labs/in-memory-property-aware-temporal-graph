package core.propertystore;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import model.Property;
import model.TemporalProperty;
import transformers.MetricsDecoderEncoderHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class PropertyStore {
    private final static int DEFAULT_NUM_PROPERTIES = 0;
    private final Int2ObjectOpenHashMap<CompressedProperty> propertyStore;

    public PropertyStore() {
        this(DEFAULT_NUM_PROPERTIES);
    }

    public PropertyStore(final int totalExpectedProperties) {
        this.propertyStore = new Int2ObjectOpenHashMap<>(totalExpectedProperties);
    }

    public void put(int propertyId, Property p) {
        TemporalProperty tp  = (TemporalProperty)p;
        ArrayList<TemporalProperty.TimeStampWithValue> valuesWithTime =  tp.getValues();
        CompressedProperty.CompressedPropertyBuilder b = CompressedProperty.builder();
        b.id(propertyId);
        b.name(new String(p.getName()));
        b.time(p.getTime());
        b.latestTimestamp(((TemporalProperty) p).getLatestTimestamp());
        int[] timeDiffs = new int[valuesWithTime.size()];
        // perform delta encoding for integer and long values.
        if ((valuesWithTime.get(0).getObject() instanceof  Long) || (valuesWithTime.get(0).getObject() instanceof  Integer)) {
            List<Number> toEncode =  new ArrayList<>();
            valuesWithTime.stream().forEach(new Consumer<TemporalProperty.TimeStampWithValue>() {
                @Override
                public void accept(TemporalProperty.TimeStampWithValue timeStampWithValue) {

                    if (timeStampWithValue.getObject() instanceof Long) {
                        toEncode.add((Long)timeStampWithValue.getObject());
                    } else {
                        toEncode.add((Integer)timeStampWithValue.getObject());
                    }
                }
            });
            // the resultant object is a  result object that contains
            // sufficient information to decode the result back.
            b.valueSeries(new MetricsDecoderEncoderHandler().encode(toEncode));
        } else {
            // non-integer/long values - retain objects as from source.
            Object[] values = new Object[valuesWithTime.size()];
            for (int i = 0; i < valuesWithTime.size(); i++) {
                values[i] = valuesWithTime.get(i);
            }
            b.valueSeries(values);
        }
        for (int  i = 0; i < valuesWithTime.size(); i++) {
            timeDiffs[i] = valuesWithTime.get(i).getTimeDifferential();
        }
        b.timeDiffs(timeDiffs);
        propertyStore.put(propertyId, b.build());
    }

    public Property get(final int propertyId) {
        if (!propertyStore.containsKey(propertyId)) {
            return null;
        }
        CompressedProperty cp = (propertyStore.get(propertyId));
        TemporalProperty.TemporalPropertyBuilder b = TemporalProperty.builder();
        b.id(propertyId);
        b.name(cp.name);
        b.time(cp.time);
        b.latestTimestamp(cp.latestTimestamp);
        ArrayList<TemporalProperty.TimeStampWithValue> valuesWithTime = new ArrayList<>();
        // the data was encoded using delta encoding
        if (cp.valueSeries instanceof MetricsDecoderEncoderHandler.Result) {
            MetricsDecoderEncoderHandler.Result<Number> res = (MetricsDecoderEncoderHandler.Result)(cp.valueSeries);
            List<Number> decoded = new MetricsDecoderEncoderHandler().decode(res);

            for (int  i = 0; i < cp.timeDiffs.length; i++) {
                TemporalProperty.TimeStampWithValue tsv = new TemporalProperty.TimeStampWithValue(cp.timeDiffs[i], decoded.get(i));
                valuesWithTime.add(tsv);
            }
        } else {
            for (int  i = 0; i < cp.timeDiffs.length; i++) {
                Object[] values = (Object[])(cp.valueSeries);
                valuesWithTime.add((TemporalProperty.TimeStampWithValue)values[i]);
            }
        }
        b.values(valuesWithTime);

        return b.build();
    }

    public boolean containsKey(int propertyId) {
        return propertyStore.containsKey(propertyId);
    }

    public Map<Integer, Property> getProperties() {
        Map<Integer, Property> properties = new HashMap<>();
        for (Map.Entry<Integer, CompressedProperty> entry : propertyStore.entrySet()) {
            Property tp = get(entry.getKey());
            properties.put(entry.getKey(), tp);
        }
        return properties;
    }

    public boolean trim() {
        return this.propertyStore.trim();
    }

    public void purgePropertiesTillTime(final IntSet propertyIds, long timestamp) {
        for (int propId : propertyIds) {
            final Property p = this.get(propId);
            if (null == p) {
                continue;
            }
            if (p.purgeTimeSeriesUntilTime(timestamp)) {
                this.put(propId, p);
            }
        }
    }

    @Data
    @AllArgsConstructor
    @Builder(toBuilder = true)
    private static final class CompressedProperty {
        private final int id;
        private final String name;
        private final long time;
        private final int[] timeDiffs;
        private final Object valueSeries;
        private final long latestTimestamp;
    }
}
