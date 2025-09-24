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

import com.adobe.acs.commons.contentsync.servlet.ContentCatalogServlet;
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
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.adobe.acs.commons.contentsync.ContentCatalogJobConsumer.JOB_TOPIC;
import static com.adobe.acs.commons.contentsync.servlet.ContentCatalogServlet.JOB_RESOURCES;
import static com.adobe.acs.commons.contentsync.servlet.ContentCatalogServlet.getJobResultsPath;

/**
 * Execute a Sling job to build a content catalog.
 * The jobs are created by {@link ContentCatalogServlet}
 */
@Component(
        service = JobExecutor.class,
        property = {
                JobExecutor.PROPERTY_TOPICS + "=" + JOB_TOPIC,
        }
)
public class ContentCatalogJobConsumer implements JobExecutor {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String SERVICE_NAME = "content-sync-reader";
    public static final Map<String, Object> AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SERVICE_NAME);
    public static final String JOB_TOPIC = "acs-commons/contentsync";

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    @Reference
    ContentSyncService syncService;

    protected void bindDeltaStrategy(UpdateStrategy strategy) {
    }

    protected void unbindDeltaStrategy(UpdateStrategy strategy) {
    }

    @Override
    public JobExecutionResult process(Job job, JobExecutionContext jobContext) {
        String pid = (String)job.getProperty("strategy");
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
            result.add(JOB_RESOURCES, resources);
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
     * Save results of a completed job into a nt:file node
     *
     * The path is determined by {@link ContentCatalogServlet#getJobResultsPath(Job jobId)}
     * @return  the path of the created nt:file node
     */
    String save(JsonObject result, Job job) throws RepositoryException, LoginException, PersistenceException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try(JsonWriter out = Json.createWriter(bout)){
            out.writeObject(result);
        }
        try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)) {
            String resultsPath = getJobResultsPath(job);
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
