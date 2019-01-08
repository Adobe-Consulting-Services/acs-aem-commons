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
package com.adobe.acs.commons.fam.mbean;

import aQute.bnd.annotation.ProviderType;
import com.adobe.granite.jmx.annotation.Description;
import com.adobe.granite.jmx.annotation.Name;
import javax.management.openmbean.TabularDataSupport;

/**
 * Throttled task runner definition
 */
@Description("Throttled Task Runner")
@ProviderType
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
    
    @Description("Change thread pool size (preserves running queue)")
    public void setThreadPoolSize(@Name("New size") @Description("4 is the suggested default.") int size);
    
}
