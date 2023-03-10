package transformers;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Implements a delta encoding scheme where-in {@link List} containing {@link Number} instances of actual type {@link Integer}
 * or {@link Long} are reduced to {@link Integer} deltas.
 * <br>
 * <b>
 *     Delta encoding does not result in significant savings when the underlying values are floating point types. The current
 *     implementation of the encoder decoder does not support floating point data types.
 * </b>
 */
public class DeltaEncoderDecoder implements  EncoderDecoder<List<Number>, DeltaEncoderDecoder.Result<Number>> {

    private static final BiFunction<Integer, Integer, Integer> intReducer = (aInt, aInt2) -> aInt2 - aInt;
    private static final BiFunction<Long, Long, Integer> longReducer = (aLong, aLong2) -> Long.valueOf(aLong2 - aLong).intValue();

    private static final HashMap<Class, BiFunction<?,?,?>> classReducerMap = new HashMap<>();

    static {
        classReducerMap.put(Long.class, longReducer);
        classReducerMap.put(Integer.class, intReducer);
    }

    @Override
    public Result<Number> encode(List<Number> data) {

        // get the underlying data type for the input data
        // it is assumed that the ArrayList contains data items
        // of homogenous types
        if (data == null) {
            return null;
        }
        if (data.size() == 0) {
            return null;
        }

        Number baseValue = data.get(0);
        if (!(baseValue instanceof  Long) && !(baseValue instanceof Integer) ) {
            throw new IllegalArgumentException(String.format("Delta encoding not supported for type: %s", baseValue.getClass()));
        }

        long[] encoded = encodeLongSeries(data);
        return new Result<>(baseValue, encoded);
    }

    @Override
    public List<Number> decode(Result<Number> encoded) {
        Number baseValue = encoded.getBaseValue();
        long[] encodedData = encoded.getEncodedData();
        List<Number> retVal = new ArrayList<>();
        retVal.add(baseValue);
        for (int  i = 1; i < encodedData.length; i++) {
            if (baseValue instanceof  Long) {
                long prevValue = retVal.get(i - 1).longValue();
                retVal.add(prevValue + encodedData[i]);
            } else {
                int prevValue = retVal.get(i - 1).intValue();
                retVal.add(prevValue + Long.valueOf(encodedData[i]).intValue());
            }

        }
        return retVal;
    }

    private long[] encodeLongSeries(List<Number> data) {
        long[] encoded = new long[data.size()];
        long prevValue = 0L;
        for (int i = 0; i < data.size(); i++) {
            long current = data.get(i).longValue();
            encoded[i] = Long.valueOf(((Number) (data.get(i))).longValue() - prevValue);
            prevValue = current;
        }
        return encoded;
    }

    private int[] encodeIntSeries(List<Number> data) {
        int[] encoded = new int[data.size()];
        int prevValue = 0;
        for (int i = 0; i < data.size(); i++) {
            int current = data.get(i).intValue();
            encoded[i] = Long.valueOf(((Number)(data.get(i))).longValue() - prevValue).intValue();
            prevValue = current;
        }
        return encoded;
    }

    public static class Result<T extends  Number> {
        @Getter
        private final T baseValue;
        @Getter
        private final long[] encodedData;

        private Result(T baseValue, long[] encodedData) {
            this.baseValue = baseValue;
            this.encodedData = Arrays.copyOf(encodedData, encodedData.length);
        }
    }
}
