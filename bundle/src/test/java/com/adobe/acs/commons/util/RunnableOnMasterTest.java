/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
