/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2018 Adobe
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
package com.adobe.acs.commons.throttling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThrottlingStateTest {

    private static final Logger LOG = LoggerFactory.getLogger(ThrottlingStateTest.class);

    private static final int CONSTANT_LOAD_SIZE = 10;

    Clock clock;

    @Before
    public void setup() {
        clock = Mockito.mock(Clock.class);
    }

    private LoadEstimator getConstantLoad() {

        LoadEstimator le = () -> CONSTANT_LOAD_SIZE;
        return le;
    }

    private ThrottlingConfiguration tc = new ThrottlingConfiguration(10, 90);

    @Test
    public void simpleThrottlingTestWithConstantLoad() {

        // send 10 requets initially at the same time
        Instant startTime = Instant.now();
        Mockito.when(clock.instant()).thenReturn(startTime);
        ThrottlingState s = new ThrottlingState(clock, getConstantLoad());
        for (int i = 0; i < CONSTANT_LOAD_SIZE; i++) {
            ThrottlingDecision decision = s.evaluateThrottling();
            assertEquals(ThrottlingDecision.State.NOTHROTTLE, decision.getState());
        }
        ThrottlingDecision decision = s.evaluateThrottling();
        assertEquals(ThrottlingDecision.State.THROTTLE, decision.getState());
        assertEquals(60 * 1000, decision.getDelay());

        // when the time iterates by 59 seconds throttling should still be active
        Instant delay1 = startTime.plusSeconds(59);
        Mockito.when(clock.instant()).thenReturn(delay1);
        ThrottlingDecision decision1 = s.evaluateThrottling();
        assertEquals(ThrottlingDecision.State.THROTTLE, decision1.getState());
        assertEquals(1 * 1000, decision1.getDelay());

        // when another 2 seconds pass, 10 new requests should run un-throttled
        Instant delay2 = delay1.plusSeconds(2);
        Mockito.when(clock.instant()).thenReturn(delay2);
        for (int i = 0; i < CONSTANT_LOAD_SIZE; i++) {
            ThrottlingDecision decision2 = s.evaluateThrottling();
            assertEquals(ThrottlingDecision.State.NOTHROTTLE, decision2.getState());
        }
        // and the 11th should be throttled again
        ThrottlingDecision decision2 = s.evaluateThrottling();
        assertEquals(ThrottlingDecision.State.THROTTLE, decision2.getState());
    }

    @Test
    public void throttlingWith1Slot() {
        // just to make sure that the case with 1 slot is covered correctly
        int loadSize = 1;
        LoadEstimator ld = () -> {
            return loadSize;
        };
        Instant startTime = Instant.now();
        Mockito.when(clock.instant()).thenReturn(startTime);
        ThrottlingState s = new ThrottlingState(clock, ld);

        ThrottlingDecision decision = s.evaluateThrottling();
        assertEquals(ThrottlingDecision.State.NOTHROTTLE, decision.getState());

        decision = s.evaluateThrottling();
        assertEquals(ThrottlingDecision.State.THROTTLE, decision.getState());

        Instant now = startTime.plusSeconds(61);
        Mockito.when(clock.instant()).thenReturn(now);
        decision = s.evaluateThrottling();
        assertEquals(ThrottlingDecision.State.NOTHROTTLE, decision.getState());

    }

    @Test
    public void throttlingWithMixedLoad() {
        int loadSize = 10;
        LoadEstimator ld = () -> {
            return loadSize;
        };
        Instant startTime = Instant.now();
        Mockito.when(clock.instant()).thenReturn(startTime);
        ThrottlingState s = new ThrottlingState(clock, ld);

        // FIXME

    }

    @Test
    public void testResize_SimpleResize() {

        Instant startTime = Instant.now();
        Mockito.when(clock.instant()).thenReturn(startTime);
        ThrottlingState s = new ThrottlingState(clock, getConstantLoad());
        for (int i = 0; i < CONSTANT_LOAD_SIZE; i++) {
            s.evaluateThrottling();
        }

        s.resize(8);

        // check that all timestamps are preserved
        assertEquals(s.timestamps.length, 8);
        for (int i = 0; i < s.timestamps.length; i++) {
            assertEquals(startTime, s.timestamps[i]);
        }
    }

    @Test
    public void testResize_DecreaseSize() {

        Instant startTime = Instant.now();
        Mockito.when(clock.instant()).thenReturn(startTime);
        ThrottlingState s = new ThrottlingState(clock, getConstantLoad());
        for (int i = 0; i < 5; i++) {
            s.evaluateThrottling();
        }
        Instant time2 = startTime.plusSeconds(2);
        Mockito.when(clock.instant()).thenReturn(time2);
        for (int i = 5; i < CONSTANT_LOAD_SIZE; i++) {
            s.evaluateThrottling();
        }
        assertEquals(0, s.currentIndex.get());

        s.resize(8);
        assertEquals(s.timestamps.length, 8);

        // check that first 5 entries are time2
        for (int i = 0; i < 5; i++) {
            assertEquals(time2, s.timestamps[i]);
        }
        // and the remaining 3 should be of startTime
        for (int i = 5; i < 8; i++) {
            assertEquals(startTime, s.timestamps[i]);
        }
        // and currentIndex should be 0
        assertEquals(0, s.currentIndex.get());
    }

    @Test
    public void testResize_DecreaseSize_2() {

        Instant startTime = Instant.now();
        Mockito.when(clock.instant()).thenReturn(startTime);
        LoadEstimator le = () -> 10;
        ThrottlingState s = new ThrottlingState(clock, le);
        for (int i = 0; i < 5; i++) {
            s.evaluateThrottling();
        }
        Instant time2 = startTime.plusSeconds(2);
        Mockito.when(clock.instant()).thenReturn(time2);
        for (int i = 5; i < CONSTANT_LOAD_SIZE; i++) {
            s.evaluateThrottling();
        }

        // now all slots are full
        Instant time3 = time2.plusSeconds(60); // should cause the first entries to get purged
        Mockito.when(clock.instant()).thenReturn(time3);
        assertEquals(ThrottlingDecision.State.NOTHROTTLE, s.evaluateThrottling().getState());
        assertEquals(ThrottlingDecision.State.NOTHROTTLE, s.evaluateThrottling().getState());

        assertEquals(2, s.currentIndex.get());

        s.loadEstimator = () -> 8; // reduce the number of slots to 8

        assertEquals(ThrottlingDecision.State.THROTTLE, s.evaluateThrottling().getState());

        // resize has happened
        assertEquals(s.timestamps.length, 8);

        // check that first 2 entries are time3
        for (int i = 0; i < 2; i++) {
            assertEquals(time3, s.timestamps[i]);
        }
        // and the next 5 should be of time2
        for (int i = 2; i < 7; i++) {
            assertEquals(time2, s.timestamps[i]);
        }
        // slot 8 should be empty
        assertEquals(Instant.EPOCH, s.timestamps[7]);

        // and currentIndex should be 0
        assertEquals(0, s.currentIndex.get());

    }

    @Test
    public void testResize_IncreaseSize() {

        Instant startTime = Instant.now();
        Mockito.when(clock.instant()).thenReturn(startTime);
        ThrottlingState s = new ThrottlingState(clock, getConstantLoad());
        for (int i = 0; i < 5; i++) {
            s.evaluateThrottling();
        }
        Instant time2 = startTime.plusSeconds(2);
        Mockito.when(clock.instant()).thenReturn(time2);
        for (int i = 5; i < CONSTANT_LOAD_SIZE; i++) {
            s.evaluateThrottling();
        }
        assertEquals(0, s.currentIndex.get());

        s.resize(12);
        assertEquals(s.timestamps.length, 12);

        // check that first 5 entries are startTime
        for (int i = 0; i < 5; i++) {
            assertEquals(startTime, s.timestamps[i]);
        }
        // the next 5 should be of startTime
        for (int i = 5; i < 10; i++) {
            assertEquals(time2, s.timestamps[i]);
        }
        // 11 and 12 are still default
        assertEquals(Instant.EPOCH, s.timestamps[10]);
        assertEquals(Instant.EPOCH, s.timestamps[11]);

        // and currentIndex should point to freshly added entries
        assertEquals(10, s.currentIndex.get());
    }

}
