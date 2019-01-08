/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.fam.actions;

import static com.adobe.acs.commons.fam.actions.Actions.nameThread;

import aQute.bnd.annotation.ProviderType;
import com.adobe.acs.commons.functions.CheckedBiConsumer;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.functions.RoundRobin;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.jcr.Session;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Replication utility functions.
 */
@ProviderType
public class ReplicationActions {

    private static final String PREFIX_ACTIVATE = "activate-";
    private static final String PREFIX_DEACTIVATE = "deactivate-";

    private ReplicationActions() {
        // Utility class cannot be instantiated directly.
    }

    /**
     * Activate all nodes using default replicators
     *
     * @return
     */
    public static final CheckedBiConsumer<ResourceResolver, String> activateAll(Replicator replicator) {
        return (ResourceResolver r, String path) -> {
            nameThread(PREFIX_ACTIVATE + path);
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
            nameThread(PREFIX_ACTIVATE + path);
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
            nameThread(PREFIX_ACTIVATE + path);
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
            nameThread(PREFIX_DEACTIVATE + path);
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
            nameThread(PREFIX_DEACTIVATE + path);
            replicator.replicate(r.adaptTo(Session.class), ReplicationActionType.DEACTIVATE, path, options);
        };
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
}
