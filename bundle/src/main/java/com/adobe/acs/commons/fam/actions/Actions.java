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
package com.adobe.acs.commons.fam.actions;

import org.osgi.annotation.versioning.ProviderType;
import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowModel;
import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowRunner;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import com.adobe.acs.commons.functions.CheckedBiConsumer;
import com.adobe.acs.commons.functions.CheckedConsumer;

/**
 * Various deferred actions to be used with the ActionManager
 */
@ProviderType
@SuppressWarnings({"squid:S1181", "squid:S1193"})
public final class Actions {
    private Actions() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(Actions.class);

    private static ThreadLocal<ActionManager> currentActionManager = new ThreadLocal<>();

    /**
     * Obtain the current action manager -- this is necessary for additional tracking such as current item
     * @return current action manager
     */
    public static ActionManager getCurrentActionManager() {
        return currentActionManager.get();
    }

    public static void setCurrentActionManager(ActionManager a) {
        if (a == null) {
            currentActionManager.remove();
        } else {
            currentActionManager.set(a);
        }
    }
    
    public static void setCurrentItem(String item) {
        ActionManager manager = getCurrentActionManager();
        if (manager != null) {
            manager.setCurrentItem(item);
        } else {
            LOG.error("Could not identify current action manager.", new IllegalStateException());
        }
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
    public static final CheckedBiConsumer<ResourceResolver, String> retryAll(final int retries, final long pausePerRetry, final CheckedBiConsumer<ResourceResolver, String> action) {
        return (ResourceResolver r, String s) -> {
            int remaining = retries;
            while (remaining > 0) {
                try {
                    action.accept(r, s);
                    return;
                } catch (InterruptedException e) {
                    r.revert();
                    r.refresh();
                    LOG.info("Timeout reached, aborting work", e);
                    throw e;
                } catch (Throwable e) {
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

    //-- Single work consumers (for use for single invocation using deferredWithResolver)
    /**
     * Retry a single action
     *
     * @param retries Number of retries to attempt
     * @param pausePerRetry Milliseconds to wait between attempts
     * @param action Action to attempt
     * @return New retry wrapper around provided action
     */
    @SuppressWarnings("squid:S3776")
    public static final CheckedConsumer<ResourceResolver> retry(final int retries, final long pausePerRetry, final CheckedConsumer<ResourceResolver> action) {
        return (ResourceResolver r) -> {
            int remaining = retries;
            while (remaining > 0) {
                try {
                    action.accept(r);
                    return;
                } catch (InterruptedException e) {
                    r.revert();
                    r.refresh();
                    LOG.info("Timeout reached, aborting work", e);
                    throw e;
                } catch (Error e) {
                    LOG.info("Critical runtime exception " + e.getMessage(), e);
                    throw e;                    
                } catch (Throwable e) {
                    LOG.info("Error commit, retry count is {}. Switch to DEBUG logging to get the full stacktrace",
                            remaining);
                    LOG.debug("Error commit, retry count is " + remaining, e);
                    r.revert();
                    r.refresh();
                    if (--remaining <= 0) {
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

    static void nameThread(String string) {
        Thread.currentThread().setName(string);
    }
}
