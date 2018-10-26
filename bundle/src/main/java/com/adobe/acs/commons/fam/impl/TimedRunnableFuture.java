
package com.adobe.acs.commons.fam.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * A runnable future for {@link TimedRunnable} which implements comparable for the purpsoe of priority execution.
 */
public class TimedRunnableFuture extends FutureTask implements Comparable {

    private TimedRunnable timedRunnable;

    /**
     * {@inheritDoc}
     */
    public TimedRunnableFuture(Callable callable) {
        super(callable);
    }

    /**
     * {@inheritDoc}
     */
    public TimedRunnableFuture(Runnable runnable, Object result) {
        super(runnable, result);
        if(runnable instanceof TimedRunnable) {
            timedRunnable = (TimedRunnable) runnable;
        }
    }

    @Override
    public int compareTo(Object o) {
        if(o instanceof TimedRunnableFuture) {
            TimedRunnableFuture other = (TimedRunnableFuture) o;
            TimedRunnable otherTimedRunnable = other.timedRunnable;
            if(otherTimedRunnable!=null && timedRunnable!=null) {
                return timedRunnable.compareTo(otherTimedRunnable);
            } else {
                return 0;
            }
        }
        return 0;
    }
}
