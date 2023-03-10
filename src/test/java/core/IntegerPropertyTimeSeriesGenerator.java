package core;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class IntegerPropertyTimeSeriesGenerator extends AbstractPropertyTimeSeriesGenerator<Integer> {

    @Override
    public Map<Long, Integer> getValueTimeSeries(int resolutionInMins, long startTime) {
        List<Long> timeUnits = getTimeUnits(startTime, resolutionInMins);
        Map<Long, Integer> valueTimeSeries =  new TreeMap<>();
        Random r = new Random();
        for (Long timeUnit : timeUnits) {
            valueTimeSeries.put(timeUnit, Math.abs(r.nextInt()));
        }
        return valueTimeSeries;
    }
}
