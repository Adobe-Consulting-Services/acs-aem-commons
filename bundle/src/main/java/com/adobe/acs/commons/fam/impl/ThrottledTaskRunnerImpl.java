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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
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
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component(service = {ThrottledTaskRunner.class, ThrottledTaskRunnerStats.class},
       immediate = true,
       property = {
       "jmx.objectname" + "=" + "com.adobe.acs.commons.fam:type=Throttled Task Runner"
       })
@Designate(ocd= ThrottledTaskRunnerImpl.Config.class)
public class ThrottledTaskRunnerImpl extends AnnotatedStandardMBean implements ThrottledTaskRunner, ThrottledTaskRunnerStats {

    @ObjectClassDefinition(name = "ACS AEM Commons - Throttled Task Runner Service",
            description = "WARNING: Setting a low 'Watchdog time' value that results in the interrupting of writing threads can lead to repository corruption. Ensure that this value is high enough to allow even outlier writing processes to complete.")
    public @interface Config {

        double DEFAULT_MAX_CPU = 0.75;
        double DEFAULT_MAX_HEAP = 0.85;
        int DEFAULT_MAX_THREADS = 4;
        int DEFAULT_COOLDOWN_TIME = 100;
        int DEFAULT_TASK_TIMEOUT = 3600000;

        @AttributeDefinition(name = "Max threads", description = "Default is 4, recommended not to exceed the number of CPU cores", defaultValue = "" + DEFAULT_MAX_THREADS)
        int max_threads() default DEFAULT_MAX_THREADS;

        @AttributeDefinition(name = "Max cpu %", description = "Range is 0..1; -1 means disable this check", defaultValue = ""+DEFAULT_MAX_CPU)
        double max_cpu() default DEFAULT_MAX_CPU;

        @AttributeDefinition(name = "Max heap %", description = "Range is 0..1; -1 means disable this check", defaultValue = "" + DEFAULT_MAX_HEAP)
        double max_heap() default DEFAULT_MAX_HEAP;

        @AttributeDefinition(name = "Cooldown time", description = "Time to wait for cpu/mem cooldown between checks", defaultValue = "" + DEFAULT_COOLDOWN_TIME)
        int cooldown_wait_time() default DEFAULT_MAX_THREADS;

        @AttributeDefinition(name = "Watchdog time", description = "Maximum time allowed (in ms) per action before it is interrupted forcefully. Defaults to 1 hour.", defaultValue = "" + DEFAULT_TASK_TIMEOUT)
        int task_timeout() default DEFAULT_TASK_TIMEOUT;
    }

    private static final Logger LOG = LoggerFactory.getLogger(ThrottledTaskRunnerImpl.class);
    private int taskTimeout;
    private int cooldownWaitTime;
    private int maxThreads;
    private double maxCpu;
    private double maxHeap;
    private boolean isPaused;
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
        workerPool.submit(r);
    }

    public void scheduleWork(Runnable work, CancelHandler cancelHandler) {
        TimedRunnable r = new TimedRunnable(work, this, taskTimeout, TimeUnit.MILLISECONDS, cancelHandler, ActionManagerConstants.DEFAULT_ACTION_PRIORITY);
        workerPool.submit(r);
    }


    @Override
    public void scheduleWork(Runnable work, int priority) {
        TimedRunnable r = new TimedRunnable(work, this, taskTimeout, TimeUnit.MILLISECONDS, priority);
        workerPool.submit(r);
    }

    public void scheduleWork(Runnable work, CancelHandler cancelHandler, int priority) {
        TimedRunnable r = new TimedRunnable(work, this, taskTimeout, TimeUnit.MILLISECONDS, cancelHandler, priority);
        workerPool.submit(r);
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
                workerPool.awaitTermination(taskTimeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                LOG.error("Timeout occurred when waiting to terminate worker pool", ex);
            }
            workerPool = null;
        }
        if (!isRunning()) {
            workerPool = new PriorityThreadPoolExecutor(maxThreads, maxThreads, taskTimeout, TimeUnit.MILLISECONDS, workQueue);
        }
    }

    @Activate
    protected void activate(Config config) {
        maxCpu = config.max_cpu();
        maxHeap = config.max_heap();
        maxThreads = config.max_threads();
        cooldownWaitTime = config.cooldown_wait_time();
        taskTimeout = config.task_timeout();

        try {
            memBeanName = ObjectName.getInstance("java.lang:type=Memory");
            osBeanName = ObjectName.getInstance("java.lang:type=OperatingSystem");
        } catch (MalformedObjectNameException | NullPointerException ex) {
            LOG.error("Error getting OS MBean (shouldn't ever happen)", ex);
        }

        initThreadPool();
    }
}
