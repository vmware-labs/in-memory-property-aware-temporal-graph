package core;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class LongPropertyTimeSeriesGenerator extends AbstractPropertyTimeSeriesGenerator<Long> {

    @Override
    public Map<Long, Long> getValueTimeSeries(int resolutionInMins, long startTime) {
        List<Long> timeUnits = getTimeUnits(startTime, resolutionInMins);
        Map<Long, Long> valueTimeSeries =  new TreeMap<>();
        Random r = new Random();
        for (Long timeUnit : timeUnits) {
            valueTimeSeries.put(timeUnit, r.nextLong());
        }
        return valueTimeSeries;
    }
}
