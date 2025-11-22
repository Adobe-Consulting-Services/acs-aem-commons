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
package com.adobe.acs.commons.contentsync.impl;

import com.adobe.acs.commons.adobeio.service.IntegrationService;
import com.adobe.acs.commons.contentsync.*;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.model.WorkflowModel;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.resource.*;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.jcr.contentloader.ContentImporter;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.json.JsonObject;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.adobe.acs.commons.contentsync.ConfigurationUtils.SETTINGS_PATH;
import static com.adobe.acs.commons.contentsync.ConfigurationUtils.UPDATE_STRATEGY_KEY;
import static com.adobe.acs.commons.contentsync.ConfigurationUtils.DEFAULT_STRATEGY_PID;

@Component(service = ContentSyncService.class)
public class ContentSyncServiceImpl implements ContentSyncService {
    public static final int COMPLETION_CHECK_INTERVAL = 3000;

    /**
     * Map of registered update strategies, keyed by class name.
     */
    private final transient Map<String, UpdateStrategy> updateStrategies = Collections.synchronizedMap(new LinkedHashMap<>());

    @Reference
    ContentImporter importer;

    @Reference (cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC)
    volatile IntegrationService integrationService;

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    @Reference
    HttpClientBuilderFactory clientBuilderFactory;

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

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CatalogItem> getRemoteItems(ExecutionContext context) throws Exception {
        Job job = context.getJob();
        RemoteInstance remoteInstance = context.getRemoteInstance();

        String root = (String) job.getProperty("root");
        boolean recursive = job.getProperty("recursive") != null;
        String catalogServlet = (String) job.getProperty("catalogServlet");
        String strategyPid = job.getProperty(UPDATE_STRATEGY_KEY, DEFAULT_STRATEGY_PID);

        long t0 = System.currentTimeMillis();
        ContentCatalog contentCatalog = new ContentCatalog(remoteInstance, catalogServlet);
        context.log("building catalog from {0}", contentCatalog.getFetchURI(root, strategyPid, recursive));

        String jobId = contentCatalog.startCatalogJob(root, strategyPid, recursive);
        context.log("Remote jobId: {0}", jobId);
        for (; ; ) {
            context.log("{0}", "collecting resources on the remote instance...");
            Thread.sleep(COMPLETION_CHECK_INTERVAL);

            if (contentCatalog.isComplete(jobId)) {
                break;
            }
        }

        List<CatalogItem> items = contentCatalog.getResults();
        context.log("catalog url: {0}", contentCatalog.getStatusCatalogJobURI(jobId));
        context.log("{0} resource(s) fetched in {1} ms", items.size(), (System.currentTimeMillis() - t0));
        return items;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CatalogItem> getItemsToSync(ExecutionContext context) throws Exception, GeneralSecurityException, URISyntaxException, InterruptedException {
        boolean incremental = context.getJob().getProperty("incremental") != null;
        String strategyPid = context.getJob().getProperty(UPDATE_STRATEGY_KEY, DEFAULT_STRATEGY_PID);

        // call the remote AEM instance, block until complete
        List<CatalogItem> remoteItems = getRemoteItems(context);
        context.put(ExecutionContext.REMOTE_ITEMS, remoteItems);
        List<CatalogItem> lst = new ArrayList<>();

        // compare the list of items fetched from remote with the local and figure out which ones need sync-ing
        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)) {
            UpdateStrategy updateStrategy = getStrategy(strategyPid);
            context.put(ExecutionContext.UPDATE_STRATEGY, updateStrategy);
            for (CatalogItem item : remoteItems) {
                if (item.getCustomExporter() != null) {
                    context.log("{0} has a custom json exporter ({1}}) and cannot be imported", item.getPath(), item.getCustomExporter());
                    continue;
                }

                Resource resource = resourceResolver.getResource(item.getPath());
                if (resource == null || !incremental || updateStrategy.isModified(item, resource)) {
                    item.setMessage(updateStrategy.getMessage(item, resource));
                    lst.add(item);
                }
            }
        }
        context.log("{0} resource(s) to sync", lst.size());
        return lst;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void syncItem(CatalogItem item, ExecutionContext context) throws Exception {
        context.log("{0}", item.getMessage());
        if (context.dryRun()) {
            return;
        }

        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)) {
            Session session = resourceResolver.adaptTo(Session.class);
            ContentReader contentReader = new ContentReader(session);
            RemoteInstance remoteInstance = context.getRemoteInstance();
            ContentSync contentSync = new ContentSync(remoteInstance, resourceResolver, importer);
            try {
                String reqPath = item.getContentUri();
                JsonObject json = remoteInstance.getJson(reqPath);

                List<String> binaryProperties = contentReader.collectBinaryProperties(json);
                JsonObject sanitizedJson = contentReader.sanitize(json);

                String observationData = context.getJob().getProperty(ConfigurationUtils.EVENT_USER_DATA_KEY, String.class);
                if(observationData != null){
                    session.getWorkspace().getObservationManager().setUserData(observationData);
                }

                context.log("\timporting data");
                contentSync.importData(item, sanitizedJson);
                if (!binaryProperties.isEmpty()) {
                    context.log("\tcopying {0} binary property(es)", binaryProperties.size());

                    boolean contentResource = item.hasContentResource();
                    String basePath = item.getPath() + (contentResource ? "/jcr:content" : "");
                    List<String> propertyPaths = binaryProperties.stream().map(p -> basePath + p).collect(Collectors.toList());
                    contentSync.copyBinaries(propertyPaths);
                }

                resourceResolver.commit();
                item.setUpdated(true);
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                context.log("{0}", sw.toString());
                resourceResolver.revert();
            }
        }
    }

    public void createVersion(CatalogItem item, ExecutionContext context) throws Exception {
        boolean createVersion = context.getJob().getProperty("createVersion") != null;
        if (!createVersion || context.dryRun()) {
            return;
        }
        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)) {
            Resource targetResource = resourceResolver.getResource(item.getPath());
            ContentSync contentSync = new ContentSync(context.getRemoteInstance(), resourceResolver, importer);
            if (targetResource != null) {
                String revisionId = contentSync.createVersion(targetResource);
                if (revisionId != null) {
                    context.log("{0}", "\tcreated revision: " + revisionId);
                }
            }
        }
    }

    /**
     * Get the strategy to build catalog.
     * If pid is null, the first available strategy is used.
     *
     * @param pid the pid of the update strategy
     * @return the update strategy
     * @throws IllegalArgumentException if no strategy is found for the given pid
     */
    public UpdateStrategy getStrategy(String pid) {
        UpdateStrategy strategy;
        if (pid == null) {
            strategy = updateStrategies.values().iterator().next();
        } else {
            strategy = updateStrategies.get(pid);
            if (strategy == null) {
                throw new IllegalArgumentException("Cannot find UpdateStrategy for pid " + pid + "."
                        + " Available strategies: " + updateStrategies.values()
                        .stream().map(s -> s.getClass().getName()).collect(Collectors.toList()));
            }
        }
        return strategy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RemoteInstance createRemoteInstance(Job job) throws Exception {
        String cfgPath = (String) job.getProperty("source");

        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)) {
            GeneralSettingsModel generalSettings = resourceResolver.resolve(SETTINGS_PATH).adaptTo(GeneralSettingsModel.class);
            SyncHostConfiguration hostConfig = resourceResolver.getResource(cfgPath).adaptTo(SyncHostConfiguration.class);
            return new RemoteInstance(clientBuilderFactory, hostConfig, generalSettings, integrationService);
        }
    }

    /**
     * For each path ensure that the order of child nodes is the same as on the remote instance.
     *
     * @param paths   the collection of node paths
     * @param context the execution context
     * @throws Exception if sorting fails
     */
    @Override
    public void sortNodes(Collection<String> paths, ExecutionContext context) throws Exception {
        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)) {
            RemoteInstance remoteInstance = context.getRemoteInstance();
            ContentSync contentSync = new ContentSync(remoteInstance, resourceResolver, importer);
            for (String parentPath : paths) {
                Resource res = resourceResolver.getResource(parentPath);
                context.log("sorting child nodes of {0}", parentPath);

                if (!context.dryRun() && res != null) {
                    Node targetNode = res.adaptTo(Node.class);
                    contentSync.sort(targetNode);
                }
            }
            resourceResolver.commit();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            context.log("{0}", sw.toString());
        }
    }

    /**
     * Returns the set of node paths to sort for the given catalog items and context.
     *
     * @param items   the collection of catalog items
     * @param context the execution context
     * @return the set of node paths to sort
     */
    @Override
    public Set<String> getNodesToSort(Collection<CatalogItem> items, ExecutionContext context) {
        String root = (String) context.getJob().getProperty("root");

        Set<String> sortedNodes = new LinkedHashSet<>();
        for (CatalogItem item : items) {
            String parentPath = ResourceUtil.getParent(item.getPath());
            if (parentPath.startsWith(root)) {
                sortedNodes.add(parentPath);
            }
        }
        return sortedNodes;
    }

    /**
     * Deletes resources that exist in the destination but not in the source.
     *
     * @param context the execution context
     * @throws Exception if deletion fails
     */
    @Override
    public void deleteUnknownResources(ExecutionContext context) throws Exception {
        if (context.getJob().getProperty("delete") == null) {
            return;
        }

        Job job = context.getJob();
        UpdateStrategy updateStrategy = (UpdateStrategy) context.get(ExecutionContext.UPDATE_STRATEGY);
        List<CatalogItem> remoteItems = (List<CatalogItem>) context.get(ExecutionContext.REMOTE_ITEMS);
        Collection<String> remotePaths = remoteItems.stream().map(c -> c.getPath()).collect(Collectors.toList());

        Map<String, Object> jobProperties = job.getPropertyNames().stream().collect(Collectors.toMap(Function.identity(), job::getProperty));
        Collection<String> localPaths = updateStrategy
                .getItems(jobProperties).stream().map(c -> c.getPath())
                .collect(Collectors.toList());

        localPaths.removeAll(remotePaths);

        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)) {
            for (String path : localPaths) {
                Resource res = resourceResolver.getResource(path);
                if (res != null) {
                    context.log("deleting {0}", path);
                    if (!context.dryRun()) {
                        resourceResolver.delete(res);
                    }
                }
            }
            resourceResolver.commit();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            context.log("{0}", sw.toString());
        }
    }

    /**
     * Starts workflows for the given collection of catalog items.
     *
     * @param items   the collection of catalog items
     * @param context the execution context
     * @throws Exception if workflow start fails
     */
    @Override
    public void startWorkflows(Collection<CatalogItem> items, ExecutionContext context) throws Exception {
        String workflowModel = (String) context.getJob().getProperty("workflowModel");
        if (workflowModel == null || workflowModel.isEmpty()) {
            return;
        }
        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)) {
            List<String> paths = items.stream()
                    .filter(itm -> itm.isUpdated())
                    .map(item -> item.getPath())
                    .filter(path -> resourceResolver.getResource(path) != null)
                    .collect(Collectors.toList());
            WorkflowSession workflowSession = resourceResolver.adaptTo(WorkflowSession.class);
            WorkflowModel model = workflowSession.getModel(workflowModel);
            if (model == null) {
                context.log("cannot find workflow model {0}", workflowModel);
            } else {
                context.log("starting {0} workflow for {1} resources", workflowModel, paths.size());
                for (String path : paths) {
                    WorkflowData data = workflowSession.newWorkflowData("JCR_PATH", path);
                    if (!context.dryRun()) {
                        workflowSession.startWorkflow(model, data);
                    }
                }

            }
        }
    }

    /**
     * Returns the {@link ResourceResolverFactory} used for obtaining resource resolvers.
     *
     * @return the resource resolver factory
     * @throws LoginException if the factory cannot be obtained
     */
    @Override
    public ResourceResolverFactory getResourceResolverFactory() throws LoginException {
        return resourceResolverFactory;
    }

}
