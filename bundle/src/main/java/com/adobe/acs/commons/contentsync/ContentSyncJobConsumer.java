/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
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
package com.adobe.acs.commons.contentsync;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.event.jobs.consumer.JobExecutionResult;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.adobe.acs.commons.contentsync.ContentSyncJobConsumer.JOB_TOPIC;

/**
 * Job consumer for the ACS Commons Content Sync jobs.
 * The jobs are created by {@link com.adobe.acs.commons.contentsync.servlet.ContentSyncRunServlet}.
 * <p>
 * This class listens for jobs on the {@link #JOB_TOPIC} and coordinates the synchronization
 * of content between local and remote AEM instances. It manages the full sync lifecycle,
 * including item synchronization, deletion of unknown resources, node sorting, and workflow initiation.
 * <p>
 * Progress and status are logged using the provided {@link ExecutionContext} and Sling job context.
 */
@Component(
        service = JobExecutor.class,
        property = {
                JobExecutor.PROPERTY_TOPICS + "=" + JOB_TOPIC,
        }
)
public class ContentSyncJobConsumer implements JobExecutor {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The job topic for content sync jobs.
     */
    public static final String JOB_TOPIC = "acs-commons/contentsync/job";

    /**
     * The ContentSyncService used to perform sync operations.
     */
    @Reference
    ContentSyncService syncService;

    /**
     * Processes a content sync job.
     * <p>
     * This method orchestrates the sync process: it determines which items need to be synchronized,
     * performs the sync, deletes unknown resources, sorts nodes, and starts workflows as needed.
     * Progress and errors are logged to the job context.
     *
     * @param job        the Sling job to process
     * @param jobContext the job execution context
     * @return the job execution result (success or cancelled)
     */
    @Override
    public JobExecutionResult process(Job job, JobExecutionContext jobContext) {
        long timeStarted = System.currentTimeMillis();
        try (ExecutionContext context = new ExecutionContext(job, syncService)){
            List<CatalogItem> items = syncService.getItemsToSync(context);
            jobContext.initProgress(items.size(), -1);

            int count = 1;
            long syncStarted = System.currentTimeMillis();
            for (CatalogItem item : items) {
                context.log( "[{0}] {1}",count, item.getPath());
                syncService.syncItem(item, context);

                updateProgress(count++, items.size(), syncStarted, jobContext, context);
            }
            context.log("sync-ed {0} resource(s) in {1} ms", items.size(), System.currentTimeMillis() - timeStarted);

            syncService.deleteUnknownResources(context);

            Collection<String> foldersToSort = syncService.getNodesToSort(items, context);
            syncService.sortNodes(foldersToSort, context);

            syncService.startWorkflows(items, context);
            context.log("all done in {0} ms", DurationFormatUtils.formatDurationWords(System.currentTimeMillis() - timeStarted, true, true));
            return jobContext.result().succeeded();
        } catch (Exception e) {
            log.error("content-sync job failed: {}", job.getId(), e);

            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            jobContext.log("{0}", sw.toString());
            return jobContext.result().cancelled();
        }
    }

    /**
     * Updates the job progress and logs the estimated time remaining.
     *
     * @param count       the number of items processed so far
     * @param totalSize   the total number of items to process
     * @param t0          the timestamp when processing started
     * @param jobContext  the job execution context
     * @param context     the execution context for logging
     */
    void updateProgress(int count, int totalSize, long t0, JobExecutionContext jobContext, ExecutionContext context){
        long remainingCycles = totalSize - count;
        long pace = (System.currentTimeMillis() - t0) / count;
        long estimatedTime = remainingCycles * pace;

        String pct = String.format("%.0f", count*100./totalSize);
        String eta = DurationFormatUtils.formatDurationWords(estimatedTime, true, true);

        jobContext.updateProgress(estimatedTime);
        jobContext.incrementProgressCount(1);
        context.log("{0}%, ETA: {1}", pct, eta);
    }
}
