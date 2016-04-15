package com.adobe.acs.commons.fam.mbean;

import com.adobe.granite.jmx.annotation.Description;
import javax.management.openmbean.TabularDataSupport;

/**
 * Throttled task runner definition
 */
@Description("Throttled Task Runner")
public interface ThrottledTaskRunnerMBean {
    
    @Description("Processes currently running")
    public long getActiveCount();

    @Description("Processes completed since last reset")
    public long getCompletedTaskCount();

    @Description("Processes added to queue since last reset")
    public long getTaskCount();

    @Description("Is queue active and able to take jobs?")
    public boolean isRunning();

    @Description("Stop queue and keep unfinished work for resume")
    public void pauseExecution();

    @Description("Restart queue that was previously halted or paused")
    public void resumeExecution();

    @Description("Stop queue and terminate any unfinished work")
    public void stopExecution();
    
    @Description("Job processing statistics")
    public TabularDataSupport getStatistics();
    
    @Description("Reset job processing statistics")
    public void clearProcessingStatistics();
}
