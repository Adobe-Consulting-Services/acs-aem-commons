/*
 * Copyright 2016 Adobe.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adobe.acs.commons.fam.impl;

import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularType;

/**
 * Collect a numeric series and produce a rolling report on the trend
 */
public class RunningStatistic {

    static private int rollingAverageWidth = 20;
    private double total;
    private long counter;
    private double rollingCounter;
    private long min;
    private long max;
    private LinkedList<Long> rollingSeries;
    private String name;

    public RunningStatistic(String name) {
        this.name = name;
        reset();
    }

    public void log(long l) {
        total += l;
        counter++;
        rollingCounter += l;
        rollingSeries.add(l);
        rollingCounter -= rollingSeries.remove(0);
        min = Math.min(min, l);
        max = Math.max(max, l);
    }

    public void reset() {
        rollingSeries = new LinkedList<Long>();
        for (int i = 0; i < rollingAverageWidth; i++) {
            rollingSeries.add(0L);
        }
        counter = 0;
        rollingCounter = 0;
        total = 0;
        min = Long.MAX_VALUE;
        max = Long.MIN_VALUE;
    }

    public long getMin() {
        return min;
    }

    public long getMax() {
        return max;
    }

    public double getMean() {
        return total / counter;
    }

    public double getRollingMean() {
        return rollingCounter / rollingAverageWidth;
    }

    public CompositeData getStatistics() throws OpenDataException {
        return new CompositeDataSupport(compositeType,itemNames, new Object[] {name, min, max, getMean(), getRollingMean()});
    }

    public static TabularType getStaticsTableType() {
        return tabularType;
    }

    private static String[] itemNames;
    private static CompositeType compositeType;
    private static TabularType tabularType;

    static {
        try {
            itemNames = new String[]{"attribute","min", "max", "mean", "rolling mean"};
            compositeType = new CompositeType(
                    "Statics Row",
                    "Single row of statistics",
                    itemNames,
                    new String[]{"Name", "Minimum value", "Maximum value", "Overall average", "Average of last " + rollingAverageWidth},
                    new OpenType[]{SimpleType.STRING, SimpleType.LONG, SimpleType.LONG, SimpleType.DOUBLE, SimpleType.DOUBLE});
            tabularType = new TabularType("Statistics", "Collected statistics", compositeType, new String[] {"attribute"});
        } catch (OpenDataException ex) {
            Logger.getLogger(RunningStatistic.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
