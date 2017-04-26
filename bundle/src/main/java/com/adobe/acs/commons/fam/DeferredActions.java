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
import com.adobe.acs.commons.fam.util.Actions;
import com.adobe.acs.commons.fam.util.Filters;
import com.adobe.acs.commons.functions.*;
import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowModel;
import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowRunner;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Various deferred actions to be used with the ActionManager
 * @deprecated Use the Checked function definitions and the fam.util classes now.  This class is only provided for compatibility.
 */
@Component
@Service(DeferredActions.class)
@ProviderType
public final class DeferredActions {

    public static final String ORIGINAL_RENDITION = Filters.ORIGINAL_RENDITION;

    @Reference
    Replicator replicator;
    
    @Reference
    SyntheticWorkflowRunner workflowRunner;
    
    //--- Filters (for using withQueryResults)
    /**
     * Returns opposite of its input, e.g. filterMatching(glob).andThen(not)
     */
    public Function<Boolean, Boolean> not = Function.adapt(Filters.NOT);

    /**
     * Returns true of glob matches provided path
     *
     * @param glob Regex expression
     * @return True for matches
     */
    public BiFunction<ResourceResolver, String, Boolean> filterMatching(final String glob) {
        return BiFunction.adapt(Filters.filterMatching(glob));
    }

    /**
     * Returns false if glob matches provided path Useful for things like
     * filterOutSubassets
     *
     * @param glob Regex expression
     * @return False for matches
     */
    public BiFunction<ResourceResolver, String, Boolean> filterNotMatching(final String glob) {
        return BiFunction.adapt(Filters.filterNotMatching(glob));
    }

    /**
     * Exclude subassets from processing
     *
     * @return true if node is not a subasset
     */
    public BiFunction<ResourceResolver, String, Boolean> filterOutSubassets() {
        return BiFunction.adapt(Filters.FILTER_OUT_SUBASSETS);
    }

    /**
     * Determine if node is a valid asset, skip any non-assets It's better to
     * filter via query if possible to avoid having to use this
     *
     * @return True if asset
     */
    public BiFunction<ResourceResolver, String, Boolean> filterNonAssets() {
        return BiFunction.adapt(Filters.FILTER_NON_ASSETS);
    }

    /**
     * This filter identifies assets where the original rendition is newer than
     * any of the other renditions. This is an especially useful function for
     * updating assets with missing or outdated thumbnails.
     *
     * @return True if asset has no thumbnails or outdated thumbnails
     */
    public BiFunction<ResourceResolver, String, Boolean> filterAssetsWithOutdatedRenditions() {
        return BiFunction.adapt(Filters.FILTER_ASSETS_WITH_OUTDATED_RENDITIONS);
    }

    //-- Query Result consumers (for using withQueryResults)
    /**
     * Retry provided action a given number of times before giving up and
     * throwing an error. Before each retry attempt, the resource resolver is
     * reverted so when using this it is a good idea to commit from your action
     * directly.
     *
     * @param retries Number of retries to attempt
     * @param pausePerRetry Milliseconds to wait between attempts
     * @param action Action to attempt
     * @return New retry wrapper around provided action
     */
    public BiConsumer<ResourceResolver, String> retryAll(final int retries, final long pausePerRetry, final BiConsumer<ResourceResolver, String> action) {
        return BiConsumer.adapt(Actions.retryAll(retries, pausePerRetry, action));
    }

    /**
     * Run nodes through synthetic workflow
     *
     * @param model Synthetic workflow model
     */
    public BiConsumer<ResourceResolver, String> startSyntheticWorkflows(final SyntheticWorkflowModel model) {
        return BiConsumer.adapt(Actions.startSyntheticWorkflows(model, workflowRunner));
    }

    public BiConsumer<ResourceResolver, String> withAllRenditions(
            final BiConsumer<ResourceResolver, String> action,
            final BiFunction<ResourceResolver, String, Boolean>... filters) {
        return BiConsumer.adapt(Actions.withAllRenditions(action, filters));
    }

    /**
     * Remove all renditions except for the original rendition for assets
     *
     * @return
     */
    public BiConsumer<ResourceResolver, String> removeAllRenditions() {
        return BiConsumer.adapt(Actions.REMOVE_ALL_RENDITIONS);
    }

    /**
     * Remove all renditions with a given name
     *
     * @param name
     * @return
     */
    public BiConsumer<ResourceResolver, String> removeAllRenditionsNamed(final String name) {
        return BiConsumer.adapt(Actions.removeAllRenditionsNamed(name));
    }

    /**
     * Activate all nodes using default replicators
     *
     * @return
     */
    public BiConsumer<ResourceResolver, String> activateAll() {
        return BiConsumer.adapt(Actions.activateAll(replicator));
    }

    /**
     * Activate all nodes using provided options NOTE: If using large batch
     * publishing it is highly recommended to set synchronous to true on the
     * replication options
     *
     * @param options
     * @return
     */
    public BiConsumer<ResourceResolver, String> activateAllWithOptions(final ReplicationOptions options) {
        return BiConsumer.adapt(Actions.activateAllWithOptions(replicator, options));
    }

    /**
     * Activate all nodes using provided options NOTE: If using large batch
     * publishing it is highly recommended to set synchronous to true on the
     * replication options
     *
     * @param options
     * @return
     */
    public BiConsumer<ResourceResolver, String> activateAllWithRoundRobin(final ReplicationOptions... options) {
        return BiConsumer.adapt(Actions.activateAllWithRoundRobin(replicator, options));
    }

    /**
     * Deactivate all nodes using default replicators
     *
     * @return
     */
    public BiConsumer<ResourceResolver, String> deactivateAll() {
        return BiConsumer.adapt(Actions.deactivateAll(replicator));
    }

    /**
     * Deactivate all nodes using provided options
     *
     * @param options
     * @return
     */
    public BiConsumer<ResourceResolver, String> deactivateAllWithOptions(final ReplicationOptions options) {
        return BiConsumer.adapt(Actions.deactivateAllWithOptions(replicator, options));
    }

    //-- Single work consumers (for use for single invocation using deferredWithResolver)
    /**
     * Retry a single action
     *
     * @param retries Number of retries to attempt
     * @param pausePerRetry Milliseconds to wait between attempts
     * @param action Action to attempt
     * @return New retry wrapper around provided action
     */
    public Consumer<ResourceResolver> retry(final int retries, final long pausePerRetry, final Consumer<ResourceResolver> action) {
        return Consumer.adapt(Actions.retry(retries, pausePerRetry, action));
    }

    /**
     * Run a synthetic workflow on a single node
     *
     * @param model
     * @param path
     * @return
     */
    final public Consumer<ResourceResolver> startSyntheticWorkflow(SyntheticWorkflowModel model, String path) {
        return Consumer.adapt(Actions.startSyntheticWorkflow(model, path, workflowRunner));
    }

    /**
     * Remove all non-original renditions from an asset.
     *
     * @param path
     * @return
     */
    final public Consumer<ResourceResolver> removeRenditions(String path) {
        return Consumer.adapt(Actions.removeRenditions(path));
    }

    /**
     * Remove all renditions with a given name
     *
     * @param path
     * @param name
     * @return
     */
    final public Consumer<ResourceResolver> removeRenditionsNamed(String path, String name) {
        return Consumer.adapt(Actions.removeRenditionsNamed(path, name));
    }

    /**
     * Activate a single node.
     *
     * @param path
     * @return
     */
    final public Consumer<ResourceResolver> activate(String path) {
        return Consumer.adapt(Actions.activate(replicator, path));
    }

    /**
     * Activate a single node using provided replication options.
     *
     * @param path
     * @param options
     * @return
     */
    final public Consumer<ResourceResolver> activateWithOptions(String path, ReplicationOptions options) {
        return Consumer.adapt(Actions.activateWithOptions(replicator, path, options));
    }

    /**
     * Deactivate a single node.
     *
     * @param path
     * @return
     */
    final public Consumer<ResourceResolver> deactivate(String path) {
        return Consumer.adapt(Actions.deactivate(replicator, path));
    }

    /**
     * Deactivate a single node using provided replication options.
     *
     * @param path
     * @param options
     * @return
     */
    final public Consumer<ResourceResolver> deactivateWithOptions(String path, ReplicationOptions options) {
        return Consumer.adapt(Actions.deactivateWithOptions(replicator, path, options));
    }
}
