/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.management.NotCompliantMBeanException;

import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.fam.ThrottledTaskRunner;

public class ThrottledTaskRunnerTest {

    @Rule
    public final OsgiContext osgiContext = new OsgiContext();

    private static final Logger log = LoggerFactory.getLogger(ThrottledTaskRunnerTest.class);

    @Test
    public void testExecutionOrderOverflow() throws NotCompliantMBeanException, InterruptedException {
        ThrottledTaskRunner ttr = osgiContext.registerInjectActivateService(new ThrottledTaskRunnerImpl());

        List<Long> executions = new ArrayList<>();

        for(int i=0;i<10;i++) {
            int finalI = i;
            ttr.scheduleWork(() -> {
                try {
                    // sleep to slow these down so that higher priority tasks go first
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    log.error("", e);
                }
                log.info("very low priority: {}", finalI);
                executions.add(1L);
            }, Integer.MIN_VALUE);
        }


        for(int i=0;i<10;i++) {
            int finalI = i;
            ttr.scheduleWork(() -> {
                log.info("very high priority: {}", finalI);
                executions.add(5L);
            }, Integer.MAX_VALUE);
        }

        while(ttr.getActiveCount() > 0) {
            Thread.sleep(1000);
        }

        //assert that the last 6 items in the list are all priority 1 (normal)
        //it's hard to guarantee the exact order before that, but the general expectation is the order
        //first 4 items are normal (4 threads execute)
        //next 10 are high
        //then the final 6 items
        assertEquals("wrong number of items executed", 20, executions.size());
        for (int i = 19; i > (19 - 6); i--) {
            assertEquals("wrong priority for item: " + i, 1L, executions.get(i).longValue());
        }

    }

    @Test
    public void testExecutionOrder() throws NotCompliantMBeanException, InterruptedException {
        ThrottledTaskRunner ttr = osgiContext.registerInjectActivateService(new ThrottledTaskRunnerImpl());

        List<Long> executions = new ArrayList<>();

        for(int i=0;i<10;i++) {
            int finalI = i;
            ttr.scheduleWork(() -> {
                try {
                    // sleep to slow these down so that higher priority tasks go first
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    log.error("", e);
                }
                log.info("normal priority: {}",  finalI);
                executions.add(1L);
            }, 1);
        }


        for(int i=0;i<10;i++) {
            int finalI = i;
            ttr.scheduleWork(() -> {
                log.info("high priority: {}", finalI);
                executions.add(5L);
            }, 5);
        }

        while(ttr.getActiveCount() > 0) {
            Thread.sleep(1000);
        }

        //assert that the last 6 items in the list are all priority 1 (normal)
        //it's hard to guarantee the exact order before that, but the general expectation is the order
        //first 4 items are normal (4 threads execute)
        //next 10 are high
        //then the final 6 items
        assertEquals("wrong number of items executed", 20, executions.size());
        for (int i = 19; i > (19 - 6); i--) {
            assertEquals("wrong priority for item: " + i, 1L, executions.get(i).longValue());
        }

    }

}
