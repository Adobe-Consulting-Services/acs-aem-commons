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
package com.adobe.acs.commons.fam;

import aQute.bnd.annotation.ProviderType;
import java.util.List;
import javax.jcr.RepositoryException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import com.adobe.acs.commons.functions.CheckedBiConsumer;
import com.adobe.acs.commons.functions.CheckedBiFunction;
import com.adobe.acs.commons.functions.CheckedConsumer;

/**
 *
 */
@ProviderType
@SuppressWarnings("squid:S00112")
public interface ActionManager {
    /**
     * Schedule an activity to occur for every node found by a given query.
     * Optionally, programmatic filters can be used to ignore query results that
     * are not of interest to the activity.  These filters can usually take on
     * more complex logic perform faster than having the query engine do the same.
     * @param queryStatement Query string
     * @param language Query language to use
     * @param callback Callback action to perform for every query result
     * @param filters Optional filters return true if action should be taken
     * @return Count of items found in query
     * @throws RepositoryException
     * @throws PersistenceException
     * @throws Exception
     */
    int withQueryResults(final String queryStatement, final String language, final CheckedBiConsumer<ResourceResolver, String> callback, final CheckedBiFunction<ResourceResolver, String, Boolean>... filters) throws RepositoryException, PersistenceException, Exception;

    /**
     * Perform action at some later time using a provided pooled resolver
     * @param action Action to perform
     */
    void deferredWithResolver(final CheckedConsumer<ResourceResolver> action);

    /**
     * Perform action right now using a provided pooled resolver
     * @param action Action to perform
     * @throws java.lang.Exception
     */
    void withResolver(CheckedConsumer<ResourceResolver> action) throws Exception;
   
    /**
     * Cancel all work scheduled using this action manager.
     * @param useForce If true, forces active work to be interrupted.
     */
    void cancel(boolean useForce);
        
    /**
     * Register a handler to be fired when the work has completed with no errors.
     * @param successTask 
     */
    void onSuccess(CheckedConsumer<ResourceResolver> successTask);

    /**
     * Register a handler to be fired when the work has completed and there was at least one error.
     * @param failureTask 
     */
    void onFailure(CheckedBiConsumer<List<Failure>, ResourceResolver> failureTask);
    
    /**
     * Register a handler to be fired when the work is completed, successfully or not.  
     * Note: These handlers are called after the success/fail handlers.
     * @param finishHandler 
     */
    void onFinish(Runnable finishHandler);

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

    /**
     * @return The number of items of work added for processing.
     */
    int getAddedCount();

    /**
     * @return The number of items of work that were successfully processed.
     */
    int getSuccessCount();

    /**
     * @return The number of items of work that were unsuccessfully processed.
     */
    int getErrorCount();

    /**
     * @return The number of items of work that were processed.

     */
    int getCompletedCount();

    /**
     * @return The number of items of work that have been added but have not yet been processed.
     */
    int getRemainingCount();
}