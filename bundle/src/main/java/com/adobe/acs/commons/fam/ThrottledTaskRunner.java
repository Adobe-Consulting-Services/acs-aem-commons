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
package com.adobe.acs.commons.fam;

import aQute.bnd.annotation.ProviderType;
import com.adobe.acs.commons.fam.mbean.ThrottledTaskRunnerMBean;

/**
 * In addition to MBean operations, a ThrottledTaskRunner lets the caller schedule work and provides a throttle method.
 * The logCompletion method should also allow a runnable action provide appropriate notification of success/failure
 */
@ProviderType
public interface ThrottledTaskRunner extends ThrottledTaskRunnerMBean {

    /**
     * Waits for CPU and Memory usage to fall below an acceptable threshold.
     * NEVER call this inside a critical section as it will result in sluggish lock contention.
     * Only call this BEFORE starting a critical section.
     * @throws InterruptedException If the thread was interrupted
     */
    void waitForLowCpuAndLowMemory() throws InterruptedException;

    /**
     * Schedule some kind of work to run in the future using the internal thread pool.
     * The work will be throttled according to the CPU/Memory settings
     * @param work
     */
    void scheduleWork(Runnable work);

    /**
     * Schedule some kind of work to run in the future using the internal thread pool.
     * The work will be throttled according to the CPU/Memory settings.  This action can be canceled at any time.
     * @param work
     * @param cancelHandler
     */
    void scheduleWork(Runnable work, CancelHandler cancelHandler);

    /**
     * Schedule some kind of work to run in the future using the internal thread pool.
     * The work will be throttled according to the CPU/Memory settings
     * @param work
     * @param priority the priority of the task
     */
    void scheduleWork(Runnable work, int priority);

    /**
     * Schedule some kind of work to run in the future using the internal thread pool.
     * The work will be throttled according to the CPU/Memory settings.  This action can be canceled at any time.
     * @param work 
     * @param cancelHandler
     * @param priority the priority of the task
     */
    void scheduleWork(Runnable work, CancelHandler cancelHandler, int priority);
    
    /**
     * Record statistics
     * @param created Task creation time (Milliseconds since epoch) -- This is when the work is added to the queue
     * @param started Start time (Milliseconds since epoch) -- This is recorded when the work is picked up by a thread
     * @param executed Execution time (Milliseconds since epoch) -- this is recorded after CPU/Memory throttle completes
     * @param finished Finish time (Milliseconds since epoch) -- This is recorded when work finishes (or throws an error)
     * @param successful If true, action concluded normally
     * @param error Exception caught, if any.
     */
    void logCompletion(long created, long started, long executed, long finished, boolean successful, Throwable error);
    
    /**
     * Get number of maximum threads supported by this thread manager
     * @return Thread pool maximum size
     */
    int getMaxThreads();
}
