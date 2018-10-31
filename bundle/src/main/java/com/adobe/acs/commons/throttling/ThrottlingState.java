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

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This class does the decision if a requests needs to be throttled or not, and holds all
 * relevant data.
 * 
 * The basic unit is "requests per minute".
 * 
 * Internally it holds an array of "slots", and the value in the slot designates when the
 * next request can be scheduled. The size of the array is determined dynamically by the
 * LoadEstimator, the adjustment is made on every request evaluated by this class.
 * 
 * 
 * It works best if the LoadEstimator returns streamlined values which do not jump too much, otherwise
 * you might get a stop-and-go behavior.
 * 
 * 
 * 
 */
public class ThrottlingState {

    private static final Logger LOG = LoggerFactory.getLogger(ThrottlingState.class);

    /**
     * This array holds the timestamps when requests have reached the system; in
     * case the timestamp has already expired, Instant.EPOCH is stored. Access
     * to this array should be guarded by the lock.
     */
    protected Instant[] timestamps;

    /**
     * The clock to get the current timestamps from.
     */
    private Clock clock;

    protected LoadEstimator loadEstimator;

    private static final long ONE_MINUTE = 1000 * 60;

    protected AtomicInteger currentIndex = new AtomicInteger();

    protected ThrottlingState(Clock clock, LoadEstimator le) {
        int queueLen = le.getMaxRequestPerMinute();
        this.clock = clock;
        this.loadEstimator = le;
        timestamps = new Instant[queueLen];
        for (int i = 0; i < queueLen; i++) {
            timestamps[i] = Instant.EPOCH;
        }
        currentIndex.set(0);
    }

    /**
     * @return
     */
    protected synchronized ThrottlingDecision evaluateThrottling() {

        ThrottlingDecision result = null;
        purgeExpiredEntries();
        resize(loadEstimator.getMaxRequestPerMinute());

        int idx = currentIndex.get();
        if (isSlotEmpty(idx)) {
            // 1 minute already passed, reuse that slot

            timestamps[idx] = clock.instant();
            currentIndex.updateAndGet(index -> (index == this.timestamps.length - 1) ? 0 : index+1);
            result = new ThrottlingDecision(ThrottlingDecision.State.NOTHROTTLE);

        } else {
            // time has not yet passed, we need some throttling

            long diff = timestamps[idx].toEpochMilli() + ONE_MINUTE - clock.instant().toEpochMilli();
            result = new ThrottlingDecision(ThrottlingDecision.State.THROTTLE).withDelay(diff)
                    .withMessage("throttling required (at least " + diff + " ms)");

        }
        return result;

    }

    /**
     * cleanup the timestamps array and replace all expired entries with
     * Instant.EPOCH;
     *
     * @return the number of emptied slots
     */
    private int purgeExpiredEntries() {
        int result = 0;

        // the array is expected to be small; therefor we can iterate through all
        // entries without a huge performance impact (although it would be possible
        // to reduce the number of array accesses by a more efficient and complex
        // algorithm
        for (int i = 0; i < timestamps.length; i++) {
            long now = clock.instant().toEpochMilli();
            if (now - timestamps[i].toEpochMilli() > ONE_MINUTE) {
                timestamps[i] = Instant.EPOCH;
                result++;
            }
        }
        return result;
    }

    /**
     * resize the queue
     *
     * @param newSize new size of queue
     * @return the number of free entries in the array
     */
    protected int resize(int newSize) {

        if (newSize != timestamps.length) {

            LOG.debug("Resizing throttling queue from {} to {}", timestamps.length, newSize);
            Instant[] newQueue = new Instant[newSize];
            int result = 0;

            if (timestamps.length - newSize > 0) { // queue got smaller
                result = reduceSize(newQueue);

            } else { // queue got larger, just resize the timestamps array and ignore the purge till
                // next cycle
                result = increaseSize(newQueue);
            }

            timestamps = newQueue;

            return result;

        } else {
            // Do not resize
            LOG.debug("No resizing required");

        }

        return 0;
    }

    private int increaseSize(Instant[] newQueue) {
        // copy all data and fill the rest with default value
        for (int i = 0; i < timestamps.length; i++) {
            newQueue[i] = timestamps[i];
        }
        for (int i = timestamps.length; i < newQueue.length; i++) {
            newQueue[i] = Instant.EPOCH;
        }
        // we just got some free slots, so let's put currentIndex there
        currentIndex.set(timestamps.length);
        return newQueue.length - timestamps.length; // we have some free slots now        
    }

    private int reduceSize(Instant[] newQueue) {
        int newIndex = 0;
        // step 1: first copy all entries with a smaller index than currentIndex
        for (int i = currentIndex.get() - 1; i >= 0 && newIndex < newQueue.length; i--, newIndex++) {
            newQueue[newIndex] = timestamps[i];
        }
        // step 2: and then copy from the highest index down to currentIndex
        for (int i = timestamps.length - 1; i > currentIndex.get() && newIndex < newQueue.length; i--, newIndex++) {
            newQueue[newIndex] = timestamps[i];
        }
        // step 3: reset currentIndex, as we start again from 0
        currentIndex.set(0);

        return 0; // no free slot
    }

    private boolean isSlotEmpty(int index) {
        return (timestamps[index] == Instant.EPOCH);
    }

}
