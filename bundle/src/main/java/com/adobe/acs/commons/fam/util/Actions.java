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
package com.adobe.acs.commons.fam.util;

import com.adobe.acs.commons.functions.RoundRobin;
import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowModel;
import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowRunner;
import com.adobe.granite.asset.api.Asset;
import com.adobe.granite.asset.api.AssetManager;
import com.adobe.granite.asset.api.Rendition;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import com.adobe.acs.commons.functions.CheckedBiConsumer;
import com.adobe.acs.commons.functions.CheckedBiFunction;
import com.adobe.acs.commons.functions.CheckedConsumer;

/**
 * Various deferred actions to be used with the ActionManager
 */
public final class Actions {
    private Actions() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(Actions.class);

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
    public static final CheckedBiConsumer<ResourceResolver, String> retryAll(final int retries, final long pausePerRetry, final CheckedBiConsumer<ResourceResolver, String> action) {
        return (ResourceResolver r, String s) -> {
            int remaining = retries;
            while (remaining > 0) {
                try {
                    action.accept(r, s);
                    return;
                } catch (Exception e) {
                    r.revert();
                    r.refresh();
                    if (remaining-- <= 0) {
                        throw e;
                    } else {
                        Thread.sleep(pausePerRetry);
                    }
                }
            }
        };
    }

    /**
     * Run nodes through synthetic workflow
     *
     * @param model Synthetic workflow model
     */
    public static final CheckedBiConsumer<ResourceResolver, String> startSyntheticWorkflows(final SyntheticWorkflowModel model, SyntheticWorkflowRunner workflowRunner) {
        return (ResourceResolver r, String path) -> {
            r.adaptTo(Session.class).getWorkspace().getObservationManager().setUserData("changedByWorkflowProcess");
            nameThread("synWf-" + path);
            workflowRunner.execute(r,
                    path,
                    model,
                    false,
                    false);
        };
    }

    public static final CheckedBiConsumer<ResourceResolver, String> withAllRenditions(
            final CheckedBiConsumer<ResourceResolver, String> action,
            final CheckedBiFunction<ResourceResolver, String, Boolean>... filters) {
        return (ResourceResolver r, String path) -> {
            AssetManager assetManager = r.adaptTo(AssetManager.class);
            Asset asset = assetManager.getAsset(path);
            for (Iterator<? extends Rendition> renditions = asset.listRenditions(); renditions.hasNext();) {
                Rendition rendition = renditions.next();
                boolean skip = false;
                if (filters != null) {
                    for (CheckedBiFunction<ResourceResolver, String, Boolean> filter : filters) {
                        if (!filter.apply(r, rendition.getPath())) {
                            skip = true;
                            break;
                        }
                    }
                }
                if (!skip) {
                    action.accept(r, path);
                }
            }
        };
    }

    /**
     * Remove all renditions except for the original rendition for assets
     *
     */
    public static final CheckedBiConsumer<ResourceResolver, String> REMOVE_ALL_RENDITIONS =
        (ResourceResolver r, String path) -> {
            nameThread("removeRenditions-" + path);
            AssetManager assetManager = r.adaptTo(AssetManager.class);
            Asset asset = assetManager.getAsset(path);
            for (Iterator<? extends Rendition> renditions = asset.listRenditions(); renditions.hasNext();) {
                Rendition rendition = renditions.next();
                if (!rendition.getName().equalsIgnoreCase("original")) {
                    asset.removeRendition(rendition.getName());
                }
            }
        };

    /**
     * Remove all renditions with a given name
     *
     */
    public static final CheckedBiConsumer<ResourceResolver, String> removeAllRenditionsNamed(final String name) {
        return (ResourceResolver r, String path) -> {
            nameThread("removeRenditions-" + path);
            AssetManager assetManager = r.adaptTo(AssetManager.class);
            Asset asset = assetManager.getAsset(path);
            for (Iterator<? extends Rendition> renditions = asset.listRenditions(); renditions.hasNext();) {
                Rendition rendition = renditions.next();
                if (rendition.getName().equalsIgnoreCase(name)) {
                    asset.removeRendition(rendition.getName());
                }
            }
        };
    }

    /**
     * Activate all nodes using default replicators
     *
     * @return
     */
    public static final CheckedBiConsumer<ResourceResolver, String> activateAll(Replicator replicator) {
        return (ResourceResolver r, String path) -> {
            nameThread("activate-" + path);
            replicator.replicate(r.adaptTo(Session.class), ReplicationActionType.ACTIVATE, path);
        };
    }

    /**
     * Activate all nodes using provided options NOTE: If using large batch
     * publishing it is highly recommended to set synchronous to true on the
     * replication options
     *
     * @param options
     * @return
     */
    public static final CheckedBiConsumer<ResourceResolver, String> activateAllWithOptions(Replicator replicator, final ReplicationOptions options) {
        return (ResourceResolver r, String path) -> {
            nameThread("activate-" + path);
            replicator.replicate(r.adaptTo(Session.class), ReplicationActionType.ACTIVATE, path, options);
        };
    }

    /**
     * Activate all nodes using provided options NOTE: If using large batch
     * publishing it is highly recommended to set synchronous to true on the
     * replication options
     *
     * @param options
     * @return
     */
    public static final CheckedBiConsumer<ResourceResolver, String> activateAllWithRoundRobin(final Replicator replicator, final ReplicationOptions... options) {
        final List<ReplicationOptions> allTheOptions = Arrays.asList(options);
        final Iterator<ReplicationOptions> roundRobin = new RoundRobin(allTheOptions).iterator();
        return (ResourceResolver r, String path) -> {
            nameThread("activate-" + path);
            replicator.replicate(r.adaptTo(Session.class), ReplicationActionType.ACTIVATE, path, roundRobin.next());
        };
    }

    /**
     * Deactivate all nodes using default replicators
     *
     * @return
     */
    public static final CheckedBiConsumer<ResourceResolver, String> deactivateAll(final Replicator replicator) {
        return (ResourceResolver r, String path) -> {
            nameThread("deactivate-" + path);
            replicator.replicate(r.adaptTo(Session.class), ReplicationActionType.DEACTIVATE, path);
        };
    }

    /**
     * Deactivate all nodes using provided options
     *
     * @param options
     * @return
     */
    public static final CheckedBiConsumer<ResourceResolver, String> deactivateAllWithOptions(final Replicator replicator, final ReplicationOptions options) {
        return (ResourceResolver r, String path) -> {
            nameThread("deactivate-" + path);
            replicator.replicate(r.adaptTo(Session.class), ReplicationActionType.DEACTIVATE, path, options);
        };
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
    public static final CheckedConsumer<ResourceResolver> retry(final int retries, final long pausePerRetry, final CheckedConsumer<ResourceResolver> action) {
        return (ResourceResolver r) -> {
            int remaining = retries;
            while (remaining > 0) {
                try {
                    action.accept(r);
                    return;
                } catch (Exception e) {
                    r.revert();
                    r.refresh();
                    LOG.info("Error commit, retry count is " + remaining, e);
                    if (remaining-- <= 0) {
                        throw e;
                    } else {
                        Thread.sleep(pausePerRetry);
                    }
                }
            }
        };
    }

    /**
     * Run a synthetic workflow on a single node
     *
     * @param model
     * @param path
     * @return
     */
    public static final CheckedConsumer<ResourceResolver> startSyntheticWorkflow(SyntheticWorkflowModel model, String path, SyntheticWorkflowRunner workflowRunner) {
        return res -> startSyntheticWorkflows(model, workflowRunner).accept(res, path);
    }

    /**
     * Remove all non-original renditions from an asset.
     *
     * @param path
     * @return
     */
    public static final CheckedConsumer<ResourceResolver> removeRenditions(String path) {
        return res -> REMOVE_ALL_RENDITIONS.accept(res, path);
    }

    /**
     * Remove all renditions with a given name
     *
     * @param path
     * @param name
     * @return
     */
    public static final CheckedConsumer<ResourceResolver> removeRenditionsNamed(String path, String name) {
        return res -> removeAllRenditionsNamed(name).accept(res, path);
    }

    /**
     * Activate a single node.
     *
     * @param path
     * @return
     */
    public static final CheckedConsumer<ResourceResolver> activate(final Replicator replicator, String path) {
        return res -> activateAll(replicator).accept(res, path);
    }

    /**
     * Activate a single node using provided replication options.
     *
     * @param path
     * @param options
     * @return
     */
    public static final CheckedConsumer<ResourceResolver> activateWithOptions(final Replicator replicator, String path, ReplicationOptions options) {
        return res -> activateAllWithOptions(replicator, options).accept(res, path);
    }

    /**
     * Deactivate a single node.
     *
     * @param path
     * @return
     */
    public static final CheckedConsumer<ResourceResolver> deactivate(final Replicator replicator, String path) {
        return res -> deactivateAll(replicator).accept(res, path);
    }

    /**
     * Deactivate a single node using provided replication options.
     *
     * @param path
     * @param options
     * @return
     */
    public static final CheckedConsumer<ResourceResolver> deactivateWithOptions(final Replicator replicator, String path, ReplicationOptions options) {
        return res -> deactivateAllWithOptions(replicator, options).accept(res, path);
    }

    static void nameThread(String string) {
        Thread.currentThread().setName(string);
    }
}
