package transformers;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MetricsDecoderEncoderHandler {

    public Result<Number> encode(List<Number> data) {
        if (data == null) {
            return null;
        }
        if (data.size() == 0) {
            return null;
        }

        Number baseValue = data.get(0);
        if (!(baseValue instanceof Long) && !(baseValue instanceof Integer)) {
            throw new IllegalArgumentException(String.format("Delta encoding not supported for type: %s", baseValue.getClass()));
        }

        byte[] encoded;
        if (baseValue instanceof Long) {
            encoded = encodeVarLongSeries(data);
            return new Result(METRIC_TYPE.LONG, encoded);
        } else {
            encoded = encodeIntVarSeries(data);
            return new Result(METRIC_TYPE.INTEGER, encoded);
        }
    }

    private byte[] encodeIntVarSeries(List<Number> data) {
        return new VarIntArrayEncoderDecoder().encode(data.stream().mapToInt(Number::intValue).toArray());
    }

    private byte[] encodeVarLongSeries(List<Number> data) {
        return new VarLongArrayEncoderDecoder().encode(data.stream().mapToLong(Number::longValue).toArray());
    }

    public List<Number> decode(Result<Number> encoded) {
        List<Number> retVal = new ArrayList<>();
        if (encoded.metric_type == METRIC_TYPE.LONG) {
            decodeVarLongSeries(encoded.getEncodedData(), retVal);
        } else if (encoded.metric_type == METRIC_TYPE.INTEGER) {
            decodeVarIntSeries(encoded.getEncodedData(), retVal);
        }
        return retVal;
    }

    private void decodeVarLongSeries(byte[] encodedData, List<Number> retVal) {
        retVal.addAll(Arrays.stream(new VarLongArrayEncoderDecoder().decode(encodedData)).boxed().collect(Collectors.toList()));
    }

    private void decodeVarIntSeries(byte[] encodedData, List<Number> retVal) {
        retVal.addAll(Arrays.stream(new VarIntArrayEncoderDecoder().decode(encodedData)).boxed().collect(Collectors.toList()));
    }

    enum METRIC_TYPE {
        INTEGER,
        LONG,
        OTHER
    }

    public static class Result<T extends Number> {
        @Getter
        private byte[] encodedData;
        @Getter
        private METRIC_TYPE metric_type = METRIC_TYPE.OTHER;

        private Result(METRIC_TYPE type, byte[] encodedData) {
            this.metric_type = type;
            this.encodedData = encodedData;
        }
    }
}
