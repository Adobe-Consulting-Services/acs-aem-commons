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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.event.jobs.consumer.JobExecutionResult;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.adobe.acs.commons.contentsync.ContentCatalogJobConsumer.JOB_TOPIC;

/**
 * Execute a Sling job to build a content catalog.
 * <p>
 * The jobs are created by {@link com.adobe.acs.commons.contentsync.servlet.ContentCatalogServlet}.
 * This consumer processes jobs on the {@link #JOB_TOPIC}, collects catalog items using the configured
 * {@link UpdateStrategy}, serializes the results to JSON, and saves them as a JCR nt:file node.
 * <p>
 * Errors during processing are logged and reported to the job context.
 */
@Component(
        service = JobExecutor.class,
        property = {
                JobExecutor.PROPERTY_TOPICS + "=" + JOB_TOPIC,
        }
)
public class ContentCatalogJobConsumer implements JobExecutor {

    /**
     * SLF4J logger for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Service user name for reading content sync data.
     */
    public static final String SERVICE_NAME = "content-sync-reader";

    /**
     * Authentication info for obtaining a service resource resolver.
     */
    public static final Map<String, Object> AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SERVICE_NAME);

    /**
     * The job topic for content catalog jobs.
     */
    public static final String JOB_TOPIC = "acs-commons/contentsync";

    /**
     * The ResourceResolverFactory used to obtain resource resolvers.
     */
    @Reference
    ResourceResolverFactory resourceResolverFactory;

    /**
     * The ContentSyncService used to retrieve update strategies and catalog items.
     */
    @Reference
    ContentSyncService syncService;

    /**
     * OSGi bind method for update strategies (not used in this implementation).
     * @param strategy the update strategy to bind
     */
    protected void bindDeltaStrategy(UpdateStrategy strategy) {}

    /**
     * OSGi unbind method for update strategies (not used in this implementation).
     * @param strategy the update strategy to unbind
     */
    protected void unbindDeltaStrategy(UpdateStrategy strategy) {}

    /**
     * Processes a content catalog job.
     * <p>
     * Retrieves the update strategy, collects catalog items, serializes them to JSON,
     * and saves the results to a JCR nt:file node. Errors are logged and reported to the job context.
     *
     * @param job        the Sling job to process
     * @param jobContext the job execution context
     * @return the job execution result (success or cancelled)
     */
    @Override
    public JobExecutionResult process(Job job, JobExecutionContext jobContext) {
        String pid = (String) job.getProperty("strategy");
        try {
            UpdateStrategy updateStrategy = syncService.getStrategy(pid);

            log.debug("processing {}, pid: {}", job.getId(), pid);
            Map<String, Object> jobProperties = job.getPropertyNames().stream().collect(Collectors.toMap(Function.identity(), job::getProperty));

            List<CatalogItem> items = updateStrategy.getItems(jobProperties); // this can take time
            JsonArrayBuilder resources = Json.createArrayBuilder();
            for (CatalogItem item : items) {
                resources.add(item.getJsonObject());
            }

            JsonObjectBuilder result = Json.createObjectBuilder();
            result.add(com.adobe.acs.commons.contentsync.servlet.ContentCatalogServlet.JOB_RESOURCES, resources);
            save(result.build(), job);
        } catch (Exception e) {
            log.error("content-sync job failed: {}", job.getId(), e);

            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            jobContext.log("{0}", sw.toString());
            return jobContext.result().cancelled();
        }
        return jobContext.result().succeeded();
    }

    /**
     * Save results of a completed job into a nt:file node.
     * <p>
     * The path is determined by {@link com.adobe.acs.commons.contentsync.servlet.ContentCatalogServlet#getJobResultsPath(Job)}.
     *
     * @param result the JSON result to save
     * @param job    the Sling job
     * @return the path of the created nt:file node
     * @throws RepositoryException   if JCR operations fail
     * @throws LoginException        if resolver login fails
     * @throws PersistenceException  if commit fails
     */
    String save(JsonObject result, Job job) throws RepositoryException, LoginException, PersistenceException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (JsonWriter out = Json.createWriter(bout)) {
            out.writeObject(result);
        }
        try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)) {
            String resultsPath = com.adobe.acs.commons.contentsync.servlet.ContentCatalogServlet.getJobResultsPath(job);
            String resultsParent = ResourceUtil.getParent(resultsPath);
            String resultsNode = ResourceUtil.getName(resultsPath);
            Node parentNode = JcrUtils.getOrCreateByPath(resultsParent, JcrConstants.NT_FOLDER, JcrConstants.NT_FOLDER, resolver.adaptTo(Session.class), false);
            Node ntFile = JcrUtils.putFile(parentNode, resultsNode, "application/json", new ByteArrayInputStream(bout.toByteArray()), Calendar.getInstance());
            log.debug(ntFile.getPath());
            resolver.commit();
            return ntFile.getPath();
        }
    }
}
