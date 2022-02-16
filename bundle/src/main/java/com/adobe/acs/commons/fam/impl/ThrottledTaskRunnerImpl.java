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
package com.adobe.acs.commons.fam.impl;

import com.adobe.acs.commons.fam.ActionManagerConstants;
import com.adobe.acs.commons.fam.CancelHandler;
import com.adobe.acs.commons.fam.ThrottledTaskRunner;
import com.adobe.acs.commons.fam.mbean.ThrottledTaskRunnerMBean;
import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularDataSupport;
import java.lang.management.ManagementFactory;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component(metatype = true,
           label = "ACS AEM Commons - Throttled Task Runner Service")
@Service({ThrottledTaskRunner.class, ThrottledTaskRunnerStats.class})
@Properties({
    @Property(name = "jmx.objectname", value = "com.adobe.acs.commons.fam:type=Throttled Task Runner", propertyPrivate = true),
    @Property(name = "max.threads", label = "Max threads", description = "Default is 4, recommended not to exceed the number of CPU cores",value = "4"),
    @Property(name = "max.cpu", label = "Max cpu %", description = "Range is 0..1; -1 means disable this check", doubleValue = 0.75),
    @Property(name = "max.heap", label = "Max heap %", description = "Range is 0..1; -1 means disable this check", doubleValue = 0.85),
    @Property(name = "cooldown.wait.time", label = "Cooldown time", description="Time to wait for cpu/mem cooldown between checks", value = "100")
})
public class ThrottledTaskRunnerImpl extends AnnotatedStandardMBean implements ThrottledTaskRunner, ThrottledTaskRunnerStats {

    private static final Logger LOG = LoggerFactory.getLogger(ThrottledTaskRunnerImpl.class);
    private long taskTimeout;
    private int cooldownWaitTime;
    private int maxThreads;
    private double maxCpu;
    private double maxHeap;
    private volatile boolean isPaused;
    private final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    private ObjectName osBeanName;
    private ObjectName memBeanName;
    private PriorityThreadPoolExecutor workerPool;
    private BlockingQueue<Runnable> workQueue;

    public ThrottledTaskRunnerImpl() throws NotCompliantMBeanException {
        super(ThrottledTaskRunnerMBean.class);
    }

    @Override
    public void scheduleWork(Runnable work) {
        TimedRunnable r = new TimedRunnable(work, this, taskTimeout, TimeUnit.MILLISECONDS, ActionManagerConstants.DEFAULT_ACTION_PRIORITY);
        submitWork(r);
    }

    public void scheduleWork(Runnable work, CancelHandler cancelHandler) {
        TimedRunnable r = new TimedRunnable(work, this, taskTimeout, TimeUnit.MILLISECONDS, cancelHandler, ActionManagerConstants.DEFAULT_ACTION_PRIORITY);
        submitWork(r);
    }

    @Override
    public void scheduleWork(Runnable work, int priority) {
        TimedRunnable r = new TimedRunnable(work, this, taskTimeout, TimeUnit.MILLISECONDS, priority);
        submitWork(r);
    }
    
    public void scheduleWork(Runnable work, CancelHandler cancelHandler, int priority) {
        TimedRunnable r = new TimedRunnable(work, this, taskTimeout, TimeUnit.MILLISECONDS, cancelHandler, priority);
        submitWork(r);
    }

    private void submitWork(TimedRunnable r) {
        if (isPaused) {
            resumeList.add(r);
        } else {
            workerPool.submit(r);
        }
    }

    RunningStatistic waitTime = new RunningStatistic("Queue wait time");
    RunningStatistic throttleTime = new RunningStatistic("Throttle time");
    RunningStatistic processingTime = new RunningStatistic("Processing time");

    @Override
    public void logCompletion(long created, long started, long executed, long finished, boolean successful, Throwable error) {
        waitTime.log(started - created);
        throttleTime.log(executed - started);
        processingTime.log(finished - executed);
    }

    @Override
    public void clearProcessingStatistics() {
        waitTime.reset();
        throttleTime.reset();
        processingTime.reset();
    }

    @Override
    public TabularDataSupport getStatistics() {
        try {
            TabularDataSupport stats = new TabularDataSupport(RunningStatistic.getStaticsTableType());
            stats.put(waitTime.getStatistics());
            stats.put(throttleTime.getStatistics());
            stats.put(processingTime.getStatistics());
            return stats;
        } catch (OpenDataException ex) {
            LOG.error("Error generating statistics", ex);
            return null;
        }
    }

    @Override
    public boolean isRunning() {
        return workerPool != null && !workerPool.isTerminating() && !workerPool.isTerminated();
    }

    @Override
    public long getActiveCount() {
        return workerPool.getActiveCount();
    }

    @Override
    public long getTaskCount() {
        return workerPool.getTaskCount();
    }

    @Override
    public long getCompletedTaskCount() {
        return workerPool.getCompletedTaskCount();
    }

    List<Runnable> resumeList = null;

    @Override
    public void pauseExecution() {
        if (isRunning()) {
            resumeList = workerPool.shutdownNow();
            isPaused = true;
        }
    }

    @Override
    public void resumeExecution() {
        if (!isRunning()) {
            initThreadPool();
            if (isPaused && resumeList != null) {
                resumeList.forEach(workerPool::execute);
                resumeList.clear();
            }
            isPaused = false;
        }
    }

    @Override
    public void stopExecution() {
        workerPool.shutdownNow();
        isPaused = false;
        if (resumeList != null) {
            resumeList.clear();
        }
    }

    @Override
    public int getMaxThreads() {
        return maxThreads;
    }

    private final Semaphore pollingLock = new Semaphore(1);
    private long lastCheck = -1;
    private boolean wasRecentlyBusy = false;

    @SuppressWarnings("squid:S3776")
    private boolean isTooBusy() throws InterruptedException {
        if (maxCpu <= 0 && maxHeap <= 0) {
            return false;
        }

        long now = System.currentTimeMillis();
        long timeSinceLastCheck = now - lastCheck;
        if (timeSinceLastCheck < 0 || timeSinceLastCheck > cooldownWaitTime) {
            pollingLock.acquire();
            now = System.currentTimeMillis();
            timeSinceLastCheck = now - lastCheck;
            if (timeSinceLastCheck < 0 || timeSinceLastCheck > cooldownWaitTime) {
                try {
                    double cpuLevel = maxCpu > 0 ? getCpuLevel() : -1;
                    double heapUsage = maxHeap > 0 ? getMemoryUsage() : -1;

                    wasRecentlyBusy = ((maxCpu > 0 && cpuLevel >= maxCpu)
                            || (maxHeap > 0 && heapUsage >= maxHeap));
                } catch (InstanceNotFoundException ex) {
                    LOG.error("OS MBean Instance not found (should not ever happen)", ex);
                } catch (ReflectionException ex) {
                    LOG.error("OS MBean Instance reflection error (should not ever happen)", ex);
                }
                lastCheck = System.currentTimeMillis();
            }
            pollingLock.release();
        }
        return wasRecentlyBusy;
    }

    @Override
    public void waitForLowCpuAndLowMemory() throws InterruptedException {
        while (isTooBusy()) {
            Thread.sleep(cooldownWaitTime);
        }
    }

    @Override
    public final double getCpuLevel() throws InstanceNotFoundException, ReflectionException {
        // This method will block until CPU usage is low enough            
        AttributeList list = mbs.getAttributes(osBeanName, new String[]{"ProcessCpuLoad"});

        if (list.isEmpty()) {
            LOG.error("No CPU stats found for ProcessCpuLoad");
            return -1;
        }

        Attribute att = (Attribute) list.get(0);
        return (Double) att.getValue();
    }

    @Override
    public final double getMemoryUsage() {
        try {
            Object memoryusage = mbs.getAttribute(memBeanName, "HeapMemoryUsage");
            CompositeData cd = (CompositeData) memoryusage;
            long max = (Long) cd.get("max");
            long used = (Long) cd.get("used");
            return (double) used / (double) max;
        } catch (AttributeNotFoundException | InstanceNotFoundException | MBeanException | ReflectionException e) {
            LOG.error("No Memory stats found for HeapMemoryUsage", e);
            return -1;
        }
    }

    @Override
    public double getMaxCpu() {
        return maxCpu;
    }

    @Override
    public double getMaxHeap() {
        return maxHeap;
    }

    @Override
    public void setThreadPoolSize(int newSize) {
        maxThreads = newSize;
        initThreadPool();
    }

    @SuppressWarnings("squid:S2142")
    private void initThreadPool() {
        if (workQueue == null) {
            workQueue = new PriorityBlockingQueue<>();
        }

        // Terminate pool if the thread size has changed
        if (workerPool != null && workerPool.getMaximumPoolSize() != maxThreads) {
            try {
                workerPool.shutdown();
                // #2660 - Remove configurable timeout/watchdog as this can result in repository corruption.
                // Never thread termination
                // https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/ThreadPoolExecutor.html#%3Cinit%3E(int,int,long,java.util.concurrent.TimeUnit,java.util.concurrent.BlockingQueue)
                workerPool.awaitTermination(taskTimeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                LOG.error("Timeout occurred when waiting to terminate worker pool", ex);
                workerPool.shutdownNow();
            }
            workerPool = null;
        }
        if (!isRunning()) {
            // #2660 - Remove configurable timeout/watchdog as this can result in repository corruption.
            // Never thread termination
            // https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/ThreadPoolExecutor.html#%3Cinit%3E(int,int,long,java.util.concurrent.TimeUnit,java.util.concurrent.BlockingQueue)
            workerPool = new PriorityThreadPoolExecutor(maxThreads, maxThreads, taskTimeout, TimeUnit.MILLISECONDS, workQueue);
        }
    }

    protected void activate(ComponentContext componentContext) {
        Dictionary<?, ?> properties = componentContext.getProperties();
        int defaultThreadCount = Math.max(1, Runtime.getRuntime().availableProcessors()/2);

        maxCpu = PropertiesUtil.toDouble(properties.get("max.cpu"), 0.75);
        maxHeap = PropertiesUtil.toDouble(properties.get("max.heap"), 0.85);
        maxThreads = PropertiesUtil.toInteger(properties.get("max.threads"), defaultThreadCount);
        cooldownWaitTime = PropertiesUtil.toInteger(properties.get("cooldown.wait.time"), 100);

        /**
         * #2660 - Remove configurable timeout/watchdog as this can result in repository corruption.
         * Force to Long.MAX_VALUE, which disables the timeout/watchdog
         * https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/ThreadPoolExecutor.html#%3Cinit%3E(int,int,long,java.util.concurrent.TimeUnit,java.util.concurrent.BlockingQueue)
         */
        taskTimeout = Long.MAX_VALUE;

        try {
            memBeanName = ObjectName.getInstance("java.lang:type=Memory");
            osBeanName = ObjectName.getInstance("java.lang:type=OperatingSystem");
        } catch (MalformedObjectNameException | NullPointerException ex) {
            LOG.error("Error getting OS MBean (shouldn't ever happen)", ex);
        }

        initThreadPool();
    }
}
