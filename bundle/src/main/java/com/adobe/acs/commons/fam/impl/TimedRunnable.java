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

import com.adobe.acs.commons.fam.CancelHandler;
import com.adobe.acs.commons.fam.ThrottledTaskRunner;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runnable task that has a time limit
 */
public class TimedRunnable implements Runnable {

    long created = System.currentTimeMillis();
    long started = -1;
    long executed = -1;
    long finished = -1;
    Runnable work;
    ThrottledTaskRunner runner;
    int timeout;
    TimeUnit timeoutUnit;
    Optional<CancelHandler> cancelHandler = Optional.empty();
    private static final Logger LOG = LoggerFactory.getLogger(TimedRunnable.class);

    public TimedRunnable(Runnable work, ThrottledTaskRunner runner, int timeout, TimeUnit timeoutUnit) {
        this.work = work;
        this.runner = runner;
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
        LOG.debug("Task created");
    }

    public TimedRunnable(Runnable work, ThrottledTaskRunner runner, int timeout, TimeUnit timeoutUnit, CancelHandler cancelHandler) {
        this(work, runner, timeout, timeoutUnit);
        this.cancelHandler = Optional.of(cancelHandler);
    }

    /**
     * Run the underlying runnable but only give it a fixed duration before
     * throwing an interruption
     */
    @Override
    @SuppressWarnings("squid:S1181")
    public void run() {
        if (cancelHandler.isPresent() && cancelHandler.get().isCancelled()) {
            return;
        }
        final Thread thisThread = Thread.currentThread();
        final Semaphore timerSemaphore = new Semaphore(0);

        Thread watchDog = new Thread(watchThread(thisThread, timerSemaphore));

        boolean successful = false;
        Throwable error = null;
        try {
            started = System.currentTimeMillis();
            runner.waitForLowCpuAndLowMemory();
            executed = System.currentTimeMillis();
            if (timeout > 0) {
                watchDog.start();
            }
            cancelHandler.ifPresent(h->h.trackActiveWork(thisThread));
            work.run();
            finished = System.currentTimeMillis();
            cancelHandler.ifPresent(h->h.untrackActiveWork(thisThread));
            timerSemaphore.release();
            successful = true;
        } catch (Throwable ex) {
            finished = System.currentTimeMillis();
            cancelHandler.ifPresent(h->h.untrackActiveWork(thisThread));
            LOG.error("Task encountered an uncaught exception", ex);
        }
        runner.logCompletion(created, started, executed, finished, successful, error);
    }

    private Runnable watchThread(Thread workThread, Semaphore timerSemaphore) {
        return () -> {
            boolean finished1 = false;
            try {
                finished1 = timerSemaphore.tryAcquire(timeout, timeoutUnit);
            } catch (InterruptedException ex) {
                LOG.error("Watchdog thread interrupted", ex);
            }
            if (!finished1) {
                LOG.error("Watchdog reached interval timeout, worker thread will now be interrupted!");
                workThread.interrupt();
            }
        };
    }
}