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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;

public interface ContentSyncService {
    /**
     * The name of the service user used for content sync operations.
     */
    String SERVICE_NAME = "content-sync-writer";

    /**
     * The base JCR path where job results are stored.
     */
    String JOB_RESULTS_BASE_PATH = "/var/acs-commons/contentsync/jobs";

    /**
     * The authentication info map for obtaining a service resource resolver.
     */
    Map<String, Object> AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SERVICE_NAME);

    /**
     * Retrieves the list of remote catalog items for the given execution context.
     *
     * @param context the execution context
     * @return the list of remote catalog items
     * @throws Exception if retrieval fails
     */
    List<CatalogItem> getRemoteItems(ExecutionContext context) throws Exception;

    /**
     * Retrieves the list of items that need to be synchronized for the given context.
     *
     * @param context the execution context
     * @return the list of items to sync
     * @throws Exception if retrieval fails
     */
    List<CatalogItem> getItemsToSync(ExecutionContext context) throws Exception;

    /**
     * Synchronizes a single catalog item using the provided context.
     *
     * @param item    the catalog item to sync
     * @param context the execution context
     * @throws Exception if synchronization fails
     */
    void syncItem(CatalogItem item, ExecutionContext context) throws Exception;

    /**
     * Creates a RemoteInstance for the given job.
     *
     * @param job the Sling Job
     * @return the remote instance
     * @throws Exception if creation fails
     */
    RemoteInstance createRemoteInstance(Job job) throws Exception;

    /**
     * Returns a collection of node paths that should be sorted after sync.
     *
     * @param items   the collection of catalog items
     * @param context the execution context
     * @return the collection of node paths to sort
     */
    Collection<String> getNodesToSort(Collection<CatalogItem> items, ExecutionContext context);

    /**
     * Sorts the given collection of node paths.
     *
     * @param paths   the collection of node paths
     * @param context the execution context
     * @throws Exception if sorting fails
     */
    void sortNodes(Collection<String> paths, ExecutionContext context) throws Exception;

    /**
     * Deletes resources that are unknown or not present in the remote instance.
     *
     * @param context the execution context
     * @throws Exception if deletion fails
     */
    void deleteUnknownResources(ExecutionContext context) throws Exception;

    /**
     * Retrieves the update strategy for the given PID.
     *
     * @param pid the strategy PID
     * @return the update strategy, or null if not found
     */
    UpdateStrategy getStrategy(String pid);

    /**
     * Starts workflows for the given collection of sync-ed items.
     *
     * @param items   the collection of sync-ed items
     * @param context the execution context
     * @throws Exception if workflow start fails
     */
    void startWorkflows(Collection<CatalogItem> items, ExecutionContext context) throws Exception;

    /**
     * Returns the ResourceResolverFactory used for obtaining resource resolvers.
     *
     * @return the resource resolver factory
     * @throws LoginException if the factory cannot be obtained
     */
    ResourceResolverFactory getResourceResolverFactory() throws LoginException;

    void createVersion(CatalogItem item, ExecutionContext context) throws Exception;
}
