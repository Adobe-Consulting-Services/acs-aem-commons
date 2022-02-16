/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
 * %%
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
 * #L%
 */
package com.adobe.acs.commons.fam.impl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Collect a numeric series and produce a rolling report on the trend
 */
public class RunningStatistic {

    private static int rollingAverageWidth = 20;
    private final String name;
    private final AtomicLong counter = new AtomicLong();
    private final AtomicLong min = new AtomicLong();
    private final AtomicLong max = new AtomicLong();
    private double total;
    private double rollingCounter;
    private List<Long> rollingSeries;

    private static final Logger LOG = LoggerFactory.getLogger(RunningStatistic.class);

    public RunningStatistic(String name) {
        this.name = name;
        reset();
    }

    public synchronized void log(long l) {
        total += l;
        counter.incrementAndGet();
        rollingCounter += l;
        rollingSeries.add(l);
        rollingCounter -= rollingSeries.remove(0);
        min.set(Math.min(min.get(), l));
        max.set(Math.max(max.get(), l));
    }

    public synchronized void reset() {
        rollingSeries = Collections.synchronizedList(new LinkedList<>());
        for (int i = 0; i < rollingAverageWidth; i++) {
            rollingSeries.add(0L);
        }
        counter.set(0);
        rollingCounter=0;
        total=0;
        min.set(Long.MAX_VALUE);
        max.set(Long.MIN_VALUE);
    }

    public long getMin() {
        return min.get();
    }

    public long getMax() {
        return max.get();
    }

    public synchronized double getMean() {
        return total / counter.get();
    }

    public synchronized double getRollingMean() {
        return rollingCounter / rollingAverageWidth;
    }

    public CompositeData getStatistics() throws OpenDataException {
        return new CompositeDataSupport(compositeType,itemNames, new Object[] {
            name, 
            min.get(), 
            max.get(), 
            getMean(), 
            getRollingMean()
        });
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
            LOG.error("Cannot create MBean", ex);
        }
    }
}
