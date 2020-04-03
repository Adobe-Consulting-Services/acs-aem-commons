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
package com.adobe.acs.commons.mcp.impl.processes;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.actions.ActionBatch;
import com.adobe.acs.commons.fam.actions.Actions;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.CheckboxComponent;
import com.adobe.acs.commons.mcp.form.PathfieldComponent;
import com.adobe.acs.commons.mcp.form.RadioComponent.EnumerationSelector;
import com.adobe.acs.commons.mcp.util.StringUtil;
import com.adobe.acs.commons.util.visitors.TreeFilteringResourceVisitor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.Queue;

/**
 * Stops all running sling jobs and empties the queue entirely.
 */
public class DeepPrune extends ProcessDefinition implements Serializable {
    private static final long serialVersionUID = 7526472295622776160L;

    private final transient JobManager jobManager;

    @SuppressWarnings("squid:S00115")
    enum FolderRule {all(s->true),numeric(StringUtils::isNumeric),hexadecimal(StringUtil::isHex),none(s->false);
        Function<String, Boolean> matcher;
        FolderRule(Function<String, Boolean> m) {
            matcher = m;
        }    
    }
    
    @FormField(name="Starting folder", 
        description="Starting point for event removal",
        hint="/var/eventing",
        component=PathfieldComponent.FolderSelectComponent.class,
        options={"base=/", "default=/var/eventing"})
    public String startingFolder;
    @FormField(name="Minimum purge level",
        description="Folder depth relative to start where purge will happen",
        options={"default=3"})
    public int minPurgeDepth = 3;
    @FormField(name="Passes",
        description="Number of passes to attempt removal",
        hint="1,2,3",
        options={"default=3"})
    public int numPasses = 3;
    @FormField(name="Ignore",
        description="Ignore nodes which have these names (comma-delimited)",
        hint="rep:policy,jobs,offloading",
        options={"default=rep:policy,jobs,offloading"})
    public String ignore;
    private List<String> ignoreList;
    @FormField(name="Batch size",
        description="Max number of operations to commit at a time",
        hint="10",
        options={"default=10"})
    public int batchSize = 10;
    @FormField(name="Retries",
        description="Max number of retries per commit",
        hint="3",
        options={"default=3"})
    public int retryCount = 3;
    @FormField(name="Retry delay",
        description="Delay between retries (in milliseconds)",
        hint="25,50,100,...",
        options={"default=25"})
    public int retryWait = 25;
    @FormField(
            name = "Delete Folders",
            description = "Define which folders to delete, if any.",
            component = EnumerationSelector.class,
            options={"default=all","vertical"}
    )
    private FolderRule folderRule = FolderRule.all;

    @FormField(
            name = "Stop job queues",
            description = "If checked, stop job queues before and resume them after the purge process",
            component = CheckboxComponent.class,
            options = {"checked"}
    )
    private boolean stopJobs = true;

    public static final String JOB_TYPE = "slingevent:Job";
    private final transient List<String> suspendedQueues = new ArrayList<>();

    public DeepPrune(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @Override
    public void init() {
        ignoreList = Arrays.asList(ignore.split(","));
    }

    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException {
        if (stopJobs) {
            instance.defineCriticalAction("Stop job queues", rr, this::stopJobQueues);
        }
        if (numPasses > 0) {
            instance.defineAction("1st pass", rr, this::purgeJobs);
        }
        if (numPasses > 1) {
            instance.defineAction("2nd pass", rr, this::purgeJobs);
        }
        if (numPasses > 2) {
            instance.defineAction("3rd pass", rr, this::purgeJobs);
        }
        if (stopJobs) {
            instance.defineCriticalAction("Resume job queues", rr, this::resumeJobQueues);
        }
        instance.getInfo().setDescription(startingFolder);
    }

    private void stopJobQueues(ActionManager manager) {
        for (Queue q : jobManager.getQueues()) {
            if (!q.isSuspended() || q.getStatistics().getNumberOfQueuedJobs() > 0) {
                suspendedQueues.add(q.getName());
                manager.deferredWithResolver(rr -> q.suspend());
            }
        }
    }

    private boolean shouldIgnore(Resource res) {
        return res == null || ignoreList.contains(res.getName());
    }
    
    private void purgeJobs(ActionManager manager) {
        ActionBatch batch = new ActionBatch(manager, batchSize);
        batch.setRetryCount(retryCount);
        batch.setRetryWait(retryWait);
        TreeFilteringResourceVisitor visitor = new TreeFilteringResourceVisitor();
        visitor.setDepthFirstMode();
        visitor.setTraversalFilter(res->visitor.isFolder(res) && !shouldIgnore(res));
        AtomicInteger lastLevel = new AtomicInteger(0);
        visitor.setResourceVisitor((res, level) -> {
            if (level >= minPurgeDepth && !shouldIgnore(res) && folderRule.matcher.apply(res.getName())) {
                if (lastLevel.getAndSet(level) != level) {
                    batch.commitBatch();
                }
                String path = res.getPath();
                batch.add(rr -> deleteResource(rr, path));
            }
        });
        visitor.setLeafVisitor((res, level) -> {
            if (!shouldIgnore(res)) {
                if (lastLevel.getAndSet(level) != level) {
                    batch.commitBatch();
                }
                String path = res.getPath();
                batch.add(rr -> deleteResource(rr, path));
            }
        });
        manager.deferredWithResolver(rr -> {
            Resource res = rr.getResource(startingFolder);
            if (res != null) {
                visitor.accept(res);
            }
            batch.commitBatch();
        });
    }

    private void deleteResource(ResourceResolver rr, String path) throws PersistenceException {
        Actions.setCurrentItem(path);
        Resource r = rr.getResource(path);
        if (r != null) {
            rr.delete(r);
        }
    }

    private void resumeJobQueues(ActionManager manager) {
        for (Queue q : jobManager.getQueues()) {
            if (suspendedQueues.contains(q.getName())) {
                manager.deferredWithResolver(rr -> q.resume());
            }
        }
    }

    @Override
    public void storeReport(ProcessInstance instance, ResourceResolver rr) {
        // no-op
    }
}