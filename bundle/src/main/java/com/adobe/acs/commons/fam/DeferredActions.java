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
 */
@Component
@Service(DeferredActions.class)
@ProviderType
public final class DeferredActions {

    public static final String ORIGINAL_RENDITION = IDeferredActions.ORIGINAL_RENDITION;

    @Reference
    private IDeferredActions delegate;

    //--- Filters (for using withQueryResults)
    /**
     * Returns opposite of its input, e.g. filterMatching(glob).andThen(not)
     */
    public Function<Boolean, Boolean> not = Function.adapt(delegate.not);

    /**
     * Returns true of glob matches provided path
     *
     * @param glob Regex expression
     * @return True for matches
     */
    public BiFunction<ResourceResolver, String, Boolean> filterMatching(final String glob) {
        return BiFunction.adapt(delegate.filterMatching(glob));
    }

    /**
     * Returns false if glob matches provided path Useful for things like
     * filterOutSubassets
     *
     * @param glob Regex expression
     * @return False for matches
     */
    public BiFunction<ResourceResolver, String, Boolean> filterNotMatching(final String glob) {
        return BiFunction.adapt(delegate.filterNotMatching(glob));
    }

    /**
     * Exclude subassets from processing
     *
     * @return true if node is not a subasset
     */
    public BiFunction<ResourceResolver, String, Boolean> filterOutSubassets() {
        return BiFunction.adapt(delegate.filterOutSubassets());
    }

    /**
     * Determine if node is a valid asset, skip any non-assets It's better to
     * filter via query if possible to avoid having to use this
     *
     * @return True if asset
     */
    public BiFunction<ResourceResolver, String, Boolean> filterNonAssets() {
        return BiFunction.adapt(delegate.filterNonAssets());
    }

    /**
     * This filter identifies assets where the original rendition is newer than
     * any of the other renditions. This is an especially useful function for
     * updating assets with missing or outdated thumbnails.
     *
     * @return True if asset has no thumbnails or outdated thumbnails
     */
    public BiFunction<ResourceResolver, String, Boolean> filterAssetsWithOutdatedRenditions() {
        return BiFunction.adapt(delegate.filterAssetsWithOutdatedRenditions());
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
        return BiConsumer.adapt(delegate.retryAll(retries, pausePerRetry, action));
    }

    /**
     * Run nodes through synthetic workflow
     *
     * @param model Synthetic workflow model
     */
    public BiConsumer<ResourceResolver, String> startSyntheticWorkflows(final SyntheticWorkflowModel model) {
        return BiConsumer.adapt(delegate.startSyntheticWorkflows(model));
    }

    public BiConsumer<ResourceResolver, String> withAllRenditions(
            final BiConsumer<ResourceResolver, String> action,
            final BiFunction<ResourceResolver, String, Boolean>... filters) {
        return BiConsumer.adapt(delegate.withAllRenditions(action, filters));
    }

    /**
     * Remove all renditions except for the original rendition for assets
     *
     * @return
     */
    public BiConsumer<ResourceResolver, String> removeAllRenditions() {
        return BiConsumer.adapt(delegate.removeAllRenditions());
    }

    /**
     * Remove all renditions with a given name
     *
     * @param name
     * @return
     */
    public BiConsumer<ResourceResolver, String> removeAllRenditionsNamed(final String name) {
        return BiConsumer.adapt(delegate.removeAllRenditionsNamed(name));
    }

    /**
     * Activate all nodes using default replicators
     *
     * @return
     */
    public BiConsumer<ResourceResolver, String> activateAll() {
        return BiConsumer.adapt(delegate.activateAll());
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
        return BiConsumer.adapt(delegate.activateAllWithOptions(options));
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
        return BiConsumer.adapt(delegate.activateAllWithRoundRobin(options));
    }

    /**
     * Deactivate all nodes using default replicators
     *
     * @return
     */
    public BiConsumer<ResourceResolver, String> deactivateAll() {
        return BiConsumer.adapt(delegate.deactivateAll());
    }

    /**
     * Deactivate all nodes using provided options
     *
     * @param options
     * @return
     */
    public BiConsumer<ResourceResolver, String> deactivateAllWithOptions(final ReplicationOptions options) {
        return BiConsumer.adapt(delegate.deactivateAllWithOptions(options));
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
        return Consumer.adapt(delegate.retry(retries, pausePerRetry, action));
    }

    /**
     * Run a synthetic workflow on a single node
     *
     * @param model
     * @param path
     * @return
     */
    final public Consumer<ResourceResolver> startSyntheticWorkflow(SyntheticWorkflowModel model, String path) {
        return Consumer.adapt(delegate.startSyntheticWorkflow(model, path));
    }

    /**
     * Remove all non-original renditions from an asset.
     *
     * @param path
     * @return
     */
    final public Consumer<ResourceResolver> removeRenditions(String path) {
        return Consumer.adapt(delegate.removeRenditions(path));
    }

    /**
     * Remove all renditions with a given name
     *
     * @param path
     * @param name
     * @return
     */
    final public Consumer<ResourceResolver> removeRenditionsNamed(String path, String name) {
        return Consumer.adapt(delegate.removeRenditionsNamed(path, name));
    }

    /**
     * Activate a single node.
     *
     * @param path
     * @return
     */
    final public Consumer<ResourceResolver> activate(String path) {
        return Consumer.adapt(delegate.activate(path));
    }

    /**
     * Activate a single node using provided replication options.
     *
     * @param path
     * @param options
     * @return
     */
    final public Consumer<ResourceResolver> activateWithOptions(String path, ReplicationOptions options) {
        return Consumer.adapt(delegate.activateWithOptions(path, options));
    }

    /**
     * Deactivate a single node.
     *
     * @param path
     * @return
     */
    final public Consumer<ResourceResolver> deactivate(String path) {
        return Consumer.adapt(delegate.deactivate(path));
    }

    /**
     * Deactivate a single node using provided replication options.
     *
     * @param path
     * @param options
     * @return
     */
    final public Consumer<ResourceResolver> deactivateWithOptions(String path, ReplicationOptions options) {
        return Consumer.adapt(delegate.deactivateWithOptions(path, options));
    }

    // these two references are no longer needed, but baseline reports their removal as requiring a major version bump
    protected void bindWorkflowRunner(SyntheticWorkflowRunner workflowRunner) {}
    protected void unbindWorkflowRunner(SyntheticWorkflowRunner workflowRunner) {}
    protected void bindReplicator(Replicator replicator) {}
    protected void unbindReplicator(Replicator replicator) {}
}
