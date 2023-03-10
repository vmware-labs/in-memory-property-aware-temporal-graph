package core;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class AbstractPropertyTimeSeriesGenerator <T> {

     public abstract Map<Long, T> getValueTimeSeries(final int resolutionInMins, final long startTime);

     protected List<Long> getTimeUnits(final long startTime, final int resolutionMins) {
         // divide 24 hours into the specified resolution
         long resolutionMillis = TimeUnit.MILLISECONDS.convert(resolutionMins, TimeUnit.MINUTES);
         long millisInDay = TimeUnit.MILLISECONDS.convert(1L, TimeUnit.DAYS);
         int numUnits = Long.valueOf(millisInDay / resolutionMillis).intValue();
         List<Long> timeUnits = new ArrayList<>();
         timeUnits.add(startTime);
         for (int i = 1; i < numUnits; ++i) {
             timeUnits.add(startTime + (resolutionMillis * i));
         }
         return timeUnits;
     }
}
