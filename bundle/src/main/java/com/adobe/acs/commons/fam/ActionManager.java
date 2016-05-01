/*
 * Copyright 2016 Adobe.
 *
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
 */
package com.adobe.acs.commons.fam;

import aQute.bnd.annotation.ProviderType;
import com.adobe.acs.commons.functions.BiConsumer;
import com.adobe.acs.commons.functions.BiFunction;
import com.adobe.acs.commons.functions.Consumer;
import java.util.List;
import javax.jcr.RepositoryException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;

/**
 *
 */
@ProviderType
public interface ActionManager {

    /**
     * Schedule an activity to occur for every node found by a given query.
     * Optionally, programmatic filters can be used to ignore query results that 
     * are not of interest to the activity.  These filters can usually take on
     * more complex logic perform faster than having the query engine do the same.
     * @param queryStatement Query string
     * @param language Query language to use
     * @param requiresCommit True if the ActionManager should handle commits automatically
     * @param callback Callback action to perform for every query result
     * @param filters Optional filters return true if action should be taken
     * @return Count of items found in query
     * @throws RepositoryException
     * @throws PersistenceException
     * @throws Exception 
     */
    int withQueryResults(final String queryStatement, final String language, final BiConsumer<ResourceResolver, String> callback, final BiFunction<ResourceResolver, String, Boolean>... filters) throws RepositoryException, PersistenceException, Exception;

    /**
     * Perform action at some later time using a provided pooled resolver
     * @param action Action to perform
     * @param requiresCommit If true, resource resolver should require a commit
     */
    void deferredWithResolver(final Consumer<ResourceResolver> action);

    /**
     * Perform action right now using a provided pooled resolver
     * @param action Action to perform
     * @param requiresCommit If true, resource resolver should require a commit
     */
    void withResolver(Consumer<ResourceResolver> action) throws Exception;
    
    /**
     * After scheduling actions withQueryResults or deferredWithResolver, schedule
     * a cleanup task to close all remaining resource resolvers.
     * NOTE: This is mandatory!
     */
    void addCleanupTask();

    /**
     * Have all actions completed?
     * @return True if all scheduled work has been completed
     */
    boolean isComplete();

    /**
     * Forcefully terminate open resolvers, should only be performed by the factory
     * If using withQueryResults or deferredWithResolvers, use addCleanupTask instead.
     */
    void closeAllResolvers();

    /**
     * List all failed actions 
     * @return List of failures
     */
    List<Failure> getFailureList();

    /**
     * List all failed actions for mbean reporting
     * @return List of composite data for failures
     * @throws OpenDataException 
     */
    List<CompositeData> getFailures() throws OpenDataException;

    /**
     * Provide statistics row for mbean reporting
     * @return
     * @throws OpenDataException 
     */
    CompositeData getStatistics() throws OpenDataException;

    /**
     * Note the name or path of the item currently being processed
     * This is particularly useful for error reporting
     * @param item Item name or path being processed currently
     */
    void setCurrentItem(String item);
    
    /**
     * @return The name set on this action manager at the time of its creation
     */
    String getName();
}
