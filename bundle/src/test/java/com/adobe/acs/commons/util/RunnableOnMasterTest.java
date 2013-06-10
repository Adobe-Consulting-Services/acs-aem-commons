package com.adobe.acs.commons.util;

import org.apache.commons.lang.mutable.MutableInt;
import org.junit.Test;
import static org.junit.Assert.*;

public class RunnableOnMasterTest {

    @Test
    public void test_that_without_bind_called_not_run() {
        Harness harness = new Harness();
        harness.run();
        assertEquals(0, harness.counter.intValue());
    }

    @Test
    public void test_that_run_called_after_bind_as_master() {
        Harness harness = new Harness();
        harness.bindRepository(null, null, true);
        harness.run();
        assertEquals(1, harness.counter.intValue());
    }

    @Test
    public void test_that_bind_as_slave_not_run() {
        Harness harness = new Harness();
        harness.bindRepository(null, null, false);
        harness.run();
        assertEquals(0, harness.counter.intValue());
    }

    @Test
    public void test_that_run_not_called_after_unbding() {
        Harness harness = new Harness();
        harness.bindRepository(null, null, true);
        harness.run();
        assertEquals(1, harness.counter.intValue());
        harness.unbindRepository();
        harness.run();
        assertEquals(1, harness.counter.intValue());
    }

    class Harness extends RunnableOnMaster {
        private MutableInt counter = new MutableInt();
        
        @Override
        protected void runOnMaster() {
            counter.increment();
        }
    }

}
