package com.adobe.acs.commons.fam.impl;

import com.adobe.acs.commons.fam.ThrottledTaskRunner;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.NotCompliantMBeanException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ThrottledTaskRunnerTest {

    @Rule
    public final OsgiContext osgiContext = new OsgiContext();

    private static final Logger log = LoggerFactory.getLogger(ThrottledTaskRunnerTest.class);

    @Test
    public void testExecutionOrder() throws NotCompliantMBeanException, InterruptedException {
        ThrottledTaskRunner ttr = osgiContext.registerInjectActivateService(new ThrottledTaskRunnerImpl());

        List<Long> executions = new ArrayList<>();

        for(int i=0;i<10;i++) {
            int finalI = i;
            ttr.scheduleWork(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log.info("normal priority: {}" + finalI);
                executions.add(1L);
            }, 1);
        }


        for(int i=0;i<10;i++) {
            int finalI = i;
            ttr.scheduleWork(() -> {
                log.info("high priority: {}" + finalI);
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
        assertEquals("", 20, executions.size());
        assertEquals("", 1L, executions.get(19).longValue());
        assertEquals("", 1L, executions.get(18).longValue());
        assertEquals("", 1L, executions.get(17).longValue());
        assertEquals("", 1L, executions.get(16).longValue());
        assertEquals("", 1L, executions.get(15).longValue());
        assertEquals("", 1L, executions.get(14).longValue());

    }

}
