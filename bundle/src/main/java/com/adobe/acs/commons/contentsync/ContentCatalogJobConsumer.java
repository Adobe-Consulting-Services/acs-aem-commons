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
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.adobe.acs.commons.contentsync.ContentCatalogJobConsumer.JOB_TOPIC;
import static com.adobe.acs.commons.contentsync.servlet.ContentCatalogServlet.getJobResultsPath;

@Component(
        service = JobExecutor.class,
        property = {
                JobExecutor.PROPERTY_TOPICS + "=" + JOB_TOPIC,
        }
)
public class ContentCatalogJobConsumer implements JobExecutor {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final transient Map<String, UpdateStrategy> updateStrategies = Collections.synchronizedMap(new LinkedHashMap<>());

    public static final String SERVICE_NAME = "content-sync";
    public static final String JOB_TOPIC = "acs-commons/contentsync";

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    @Reference(service = UpdateStrategy.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC)
    protected void bindDeltaStrategy(UpdateStrategy strategy) {
        if (strategy != null) {
            String key = strategy.getClass().getName();
            updateStrategies.put(key, strategy);
        }
    }

    protected void unbindDeltaStrategy(UpdateStrategy strategy) {
        String key = strategy.getClass().getName();
        updateStrategies.remove(key);
    }

    @Override
    public JobExecutionResult process(Job job, JobExecutionContext context) {
        String pid = (String)job.getProperty("strategy");
        UpdateStrategy updateStrategy = getStrategy(pid);
        try {
            Map<String, Object> jobProperties = job.getPropertyNames().stream().collect(Collectors.toMap(Function.identity(), job::getProperty));

            List<CatalogItem> items = updateStrategy.getItems(jobProperties); // this can take time
            JsonArrayBuilder resources = Json.createArrayBuilder();
            for (CatalogItem item : items) {
                resources.add(item.getJsonObject());
            }

            JsonObjectBuilder result = Json.createObjectBuilder();
            result.add("resources", resources);
            save(result.build(), job);
        } catch (Exception e) {
            log.error("content-sync job failed: {}", job.getId(), e);
            return context.result().message(e.getMessage()).cancelled();
        }
        return context.result().succeeded();
    }
    /**
     * Get the strategy to build catalog.
     * If pid is null, the first available strategy is used.
     *
     * @param pid the pid of the update strategy
     * @return the update strategy
     */
    UpdateStrategy getStrategy(String pid) {
        UpdateStrategy strategy;
        if(pid == null){
            strategy = updateStrategies.values().iterator().next();
        } else {
            strategy = updateStrategies.get(pid);
            if(strategy == null){
                throw new IllegalArgumentException("Cannot find UpdateStrategy for pid " + pid + "."
                        + " Available strategies: " + updateStrategies.values()
                        .stream().map(s -> s.getClass().getName()).collect(Collectors.toList()));
            }
        }
        return strategy;
    }

    /**
     * Save results of a completed job into a nt:file node
     *
     * The path is determined by {@link ContentCatalogServlet#getJobResultsPath(String jobId)}
      */
    void save(JsonObject result, Job job) throws RepositoryException, LoginException, PersistenceException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try(JsonWriter out = Json.createWriter(bout)){
            out.writeObject(result);
        }
        Map<String, Object> AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SERVICE_NAME);
        try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)) {
            String resultsPath = getJobResultsPath(job.getId());
            String resultsParent = ResourceUtil.getParent(resultsPath);
            String resultsNode = ResourceUtil.getName(resultsPath);
            Node parentNode = JcrUtils.getOrCreateByPath(resultsParent, JcrConstants.NT_FOLDER, JcrConstants.NT_FOLDER, resolver.adaptTo(Session.class), false);
            JcrUtils.putFile(parentNode, resultsNode, "application/json", new ByteArrayInputStream(bout.toByteArray()), null);
            resolver.commit();
        }
    }
}
