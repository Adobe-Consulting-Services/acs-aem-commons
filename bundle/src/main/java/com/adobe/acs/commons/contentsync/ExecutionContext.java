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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;

import org.apache.sling.event.jobs.Job;

import static com.adobe.acs.commons.contentsync.ContentSyncService.JOB_RESULTS_BASE_PATH;
import com.adobe.acs.commons.contentsync.io.JobLogWriter;

/**
 * ExecutionContext encapsulates the state and resources required to execute a content sync job.
 * <p>
 * It manages the job, the associated remote instance, and a log writer for job-specific logging.
 * This class also provides utility methods for logging, dry-run detection, and resource cleanup.
 * <p>
 * Usage:
 * <pre>
 * try (ExecutionContext ctx = new ExecutionContext(job, syncService)) {
 *     ctx.log("Sync started for {0}", ctx.getRemoteInstance().getHostConfiguration().getHost());
 *     // ... perform sync ...
 * }
 * </pre>
 */
public final class ExecutionContext extends HashMap<String, Object> implements AutoCloseable {
    /**
     * Key for storing remote items in the context map.
     */
    public static final String REMOTE_ITEMS = "remoteItems";

    /**
     * Key for storing the update strategy in the context map.
     */
    public static final String UPDATE_STRATEGY = "updateStrategy";

    /**
     * The Sling Job associated with this execution context.
     */
    final Job job;

    /**
     * The remote instance being synchronized.
     */
    final RemoteInstance remoteInstance;

    /**
     * The log writer for job-specific logs.
     */
    final JobLogWriter logWriter;

    /**
     * Constructs a new ExecutionContext for the given job and sync service.
     *
     * @param job         the Sling Job to execute
     * @param syncService the ContentSyncService providing dependencies
     * @throws Exception if remote instance or log writer initialization fails
     */
    public ExecutionContext(Job job, ContentSyncService syncService) throws Exception {
        this.job = job;
        this.remoteInstance =  syncService.createRemoteInstance(job);

        String logPath = ExecutionContext.getLogPath(job);
        this.logWriter = new JobLogWriter(syncService.getResourceResolverFactory(), logPath);

        log("remote host: {0}", remoteInstance.getHostConfiguration().getHost());
        log("sync root: {0}", job.getProperty("root"));
    }

    /**
     * Returns the log path for the given job.
     *
     * @param job the Sling Job
     * @return the log path as a String
     */
    public static String getLogPath(Job job){
        return String.format(JOB_RESULTS_BASE_PATH + "/%s", job.getId());
    }

    /**
     * Logs a formatted message to the job log.
     * If this is a dry run, the message is prefixed with "[dry-run]".
     *
     * @param msg  the message format string
     * @param args arguments referenced by the format specifiers in the message
     */
    public void log(String msg, Object... args)  {
        try {
            if (dryRun()) msg = "[dry-run] " + msg;
            String logEntry = MessageFormat.format(msg, args);
            logWriter.write(logEntry);
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the Sling Job associated with this context.
     *
     * @return the Job
     */
    public Job getJob() {
        return job;
    }

    /**
     * Gets the remote instance associated with this context.
     *
     * @return the RemoteInstance
     */
    public RemoteInstance getRemoteInstance() {
        return remoteInstance;
    }

    /**
     * Returns true if this execution is a dry run.
     *
     * @return true if dry run, false otherwise
     */
    public boolean dryRun() {
        return job.getProperty("dryRun") != null;
    }

    /**
     * Closes the execution context, releasing resources.
     * Closes the remote instance and the log writer.
     * Throws RuntimeException if an IOException occurs.
     */
    @Override
    public void close() {
        try {
            remoteInstance.close();
            logWriter.close();
        } catch (IOException  e){
            throw new RuntimeException(e);
        }
    }
}
