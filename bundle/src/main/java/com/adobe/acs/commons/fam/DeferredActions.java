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

import com.adobe.acs.commons.functions.*;
import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowModel;
import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowRunner;
import com.adobe.granite.asset.api.Asset;
import com.adobe.granite.asset.api.AssetManager;
import com.adobe.granite.asset.api.Rendition;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import java.util.Iterator;
import javax.jcr.Session;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Various deferred actions to be used with the ActionManager
 */
@Component(metatype = true, immediate = true, label = "Deferred Actions")
@Service(DeferredActions.class)
public class DeferredActions {
    @Reference
    private SyntheticWorkflowRunner workflowRunner;

    @Reference
    private Replicator replicator;

    //--- Filters (for using withQueryResults)
    /**
     * Returns opposite of its input, e.g. filterMatching(glob).andThen(not)
     */
    public Function<Boolean, Boolean> not = new Function<Boolean, Boolean>() {
        @Override
        public Boolean apply(Boolean t) {
            return !t;
        }
    };

    /**
     * Returns true of glob matches provided path
     * @param glob Regex expression
     * @return True for matches
     */
    public BiFunction<ResourceResolver, String, Boolean> filterMatching(final String glob) {
        return new BiFunction<ResourceResolver, String, Boolean>() {
            @Override
            public Boolean apply(ResourceResolver r, String path) {
                return path.matches(glob);
            }
        };
    }

    /**
     * Returns false if glob matches provided path
     * Useful for things like filterOutSubassets
     * @param glob Regex expression
     * @return False for matches
     */
    public BiFunction<ResourceResolver, String, Boolean> filterNotMatching(final String glob) {
        return filterMatching(glob).andThen(not);
    }

    /**
     * Exclude subassets from processing
     * @return true if node is not a subasset
     */
    public BiFunction<ResourceResolver, String, Boolean> filterOutSubassets() {
        return filterNotMatching(".*?/subassets/.*");
    }

    /**
     * Determine if node is a valid asset, skip any non-assets
     * It's better to filter via query if possible to avoid having to use this
     * @return True if asset
     */
    public BiFunction<ResourceResolver, String, Boolean> filterNonAssets() {
        return new BiFunction<ResourceResolver, String, Boolean>() {
            @Override
            public Boolean apply(ResourceResolver r, String path) {
                nameThread("filterNonAssets-" + path);
                Resource res = r.getResource(path);
                return (DamUtil.resolveToAsset(res) != null);
            }
        };
    }

    //-- Query Result consumers (for using withQueryResults)
    /**
     * Run nodes through synthetic workflow
     * @param model Synthetic workflow model
     */
    public BiConsumer<ResourceResolver, String> startSyntheticWorkflows(final SyntheticWorkflowModel model) {
        return new BiConsumer<ResourceResolver, String>() {
            @Override
            public void accept(ResourceResolver r, String path) throws Exception {
                r.adaptTo(Session.class).getWorkspace().getObservationManager().setUserData("changedByWorkflowProcess");
                nameThread("synWf-" + path);
                workflowRunner.execute(r,
                        path,
                        model,
                        false,
                        false);
            }
        };
    }

    /**
     * Remove all renditions except for the original rendition for assets
     */
    public BiConsumer<ResourceResolver, String> removeAllRenditions() {
        return new BiConsumer<ResourceResolver, String>() {
            @Override
            public void accept(ResourceResolver r, String path) {
                nameThread("removeRenditions-" + path);
                AssetManager assetManager = r.adaptTo(AssetManager.class);
                Asset asset = assetManager.getAsset(path);
                for (Iterator<? extends Rendition> renditions = asset.listRenditions(); renditions.hasNext();) {
                    Rendition rendition = renditions.next();
                    if (!rendition.getName().equalsIgnoreCase("original")) {
                        asset.removeRendition(rendition.getName());
                    }
                }
            }
        };
    }
    
    /**
     * Activate all nodes using default replicators
     */
    public BiConsumer<ResourceResolver, String> activateAll() {
        return new BiConsumer<ResourceResolver, String>() {
            @Override
            public void accept(ResourceResolver r, String path) throws ReplicationException {
                nameThread("activate-" + path);
                replicator.replicate(r.adaptTo(Session.class), ReplicationActionType.ACTIVATE, path);
            }
        };
    }

    /**
     * Activate all nodes using provided options
     */
    public BiConsumer<ResourceResolver, String> activateAllWithOptions(final ReplicationOptions options) {
        return new BiConsumer<ResourceResolver, String>() {
            @Override
            public void accept(ResourceResolver r, String path) throws ReplicationException {
                nameThread("activate-" + path);
                replicator.replicate(r.adaptTo(Session.class), ReplicationActionType.ACTIVATE, path, options);
            }
        };
    }

    /**
     * Deactivate all nodes using default replicators
     */
    public BiConsumer<ResourceResolver, String> deactivateAll() {
        return new BiConsumer<ResourceResolver, String>() {
            @Override
            public void accept(ResourceResolver r, String path) throws ReplicationException {
                nameThread("deactivate-" + path);
                replicator.replicate(r.adaptTo(Session.class), ReplicationActionType.DEACTIVATE, path);
            }
        };
    }

    /**
     * Deactivate all nodes using provided options
     * 
     */
    public BiConsumer<ResourceResolver, String> deactivateAllWithOptions(final ReplicationOptions options) {
        return new BiConsumer<ResourceResolver, String>() {
            @Override
            public void accept(ResourceResolver r, String path) throws ReplicationException {
                nameThread("deactivate-" + path);
                replicator.replicate(r.adaptTo(Session.class), ReplicationActionType.DEACTIVATE, path, options);
            }
        };
    }

    //-- Single work consumers (for use for single invocation using deferredWithResolver)
    public Consumer<ResourceResolver> startSyntheticWorkflow(final SyntheticWorkflowModel model, final String path) {
        return new Consumer<ResourceResolver>() {
            @Override
            public void accept(ResourceResolver r) throws Exception {
                startSyntheticWorkflows(model).accept(r, path);
            }
        };
    }
    
    public Consumer<ResourceResolver> removeRenditions(final String path) {
        return new Consumer<ResourceResolver>() {
            @Override
            public void accept(ResourceResolver r) throws Exception {
                removeAllRenditions().accept(r, path);
            }
        };
    }

    public Consumer<ResourceResolver> activate(final String path) {
        return new Consumer<ResourceResolver>() {
            @Override
            public void accept(ResourceResolver r) throws Exception {
                activateAll().accept(r, path);
            }
        };
    }

    public Consumer<ResourceResolver> activateWithOptions(final String path, final ReplicationOptions options) {
        return new Consumer<ResourceResolver>() {
            @Override
            public void accept(ResourceResolver r) throws Exception {
                activateAllWithOptions(options).accept(r, path);
            }
        };
    }

    public Consumer<ResourceResolver> deactivate(final String path) {
        return new Consumer<ResourceResolver>() {
            @Override
            public void accept(ResourceResolver r) throws Exception {
                deactivateAll().accept(r, path);
            }
        };
    }

    public Consumer<ResourceResolver> deactivateWithOptions(final String path, final ReplicationOptions options) {
        return new Consumer<ResourceResolver>() {
            @Override
            public void accept(ResourceResolver r) throws Exception {
                deactivateAllWithOptions(options).accept(r, path);
            }
        };
    }

    private void nameThread(String string) {
        Thread.currentThread().setName(string);
    }
}
