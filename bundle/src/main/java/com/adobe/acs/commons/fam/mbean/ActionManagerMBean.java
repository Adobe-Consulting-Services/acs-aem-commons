package com.adobe.acs.commons.fam.mbean;

import com.adobe.granite.jmx.annotation.Description;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularDataSupport;

/**
 * Provide details about running tasks
 */
@Description("Task Manager")
public interface ActionManagerMBean {
    @Description("Tasks")
    public TabularDataSupport getStatistics() throws OpenDataException;
    
    @Description("Purge completed tasks")
    public void purgeCompletedTasks();
    
    @Description("Failures")
    public TabularDataSupport getFailures() throws OpenDataException;    
}
