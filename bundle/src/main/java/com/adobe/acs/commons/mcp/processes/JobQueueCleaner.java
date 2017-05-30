/*
 * Copyright 2017 Adobe.
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
package com.adobe.acs.commons.mcp.processes;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.actions.Actions;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.PathfieldComponent;
import com.adobe.acs.commons.util.visitors.TreeFilteringResourceVisitor;
import java.util.ArrayList;
import java.util.List;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.Queue;

/**
 * Stops all running sling jobs and empties the queue entirely.
 */
@Component
@Service(ProcessDefinition.class)
public class JobQueueCleaner implements ProcessDefinition {
    @Reference
    transient private JobManager jobManager;

    @FormField(name="Starting folder", 
        description="Starting point for event removal",
        hint="/var/eventing",
        component=PathfieldComponent.FolderSelectComponent.class,
        options={"base=/var/eventing", "default=/var/eventing"})
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

    public static final String JOB_TYPE = "slingevent:Job";
    public static final String POLICY_NODE_NAME = "rep:policy";
    transient private final List<String> suspendedQueues = new ArrayList<>();

    public JobQueueCleaner() {
    }

    @Override
    public String getName() {
        return "Job Queue Cleaner";
    }

    @Override
    public void init() {
        // Nothing to do here
    }

    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException {
        instance.defineCriticalAction("Stop job queues", rr, this::stopJobQueues);
        if (numPasses > 0) {
            instance.defineAction("1st pass", rr, this::purgeJobs);
        }
        if (numPasses > 1) {
            instance.defineAction("2nd pass", rr, this::purgeJobs);
        }
        if (numPasses > 2) {
            instance.defineAction("3rd pass", rr, this::purgeJobs);
        }
        instance.defineCriticalAction("Resume job queues", rr, this::resumeJobQueues);
    }

    private void stopJobQueues(ActionManager manager) {
        for (Queue q : jobManager.getQueues()) {
            if (!q.isSuspended() || q.getStatistics().getNumberOfQueuedJobs() > 0) {
                suspendedQueues.add(q.getName());
                manager.deferredWithResolver(rr -> q.suspend());
            }
        }
    }

    private void purgeJobs(ActionManager manager) {
        TreeFilteringResourceVisitor visitor = new TreeFilteringResourceVisitor();
        visitor.setDepthFirstMode();
        visitor.setResourceVisitor((res, level) -> {
            if (level >= minPurgeDepth) {
                String path = res.getPath();
                manager.deferredWithResolver(Actions.retry(10, 100, rr -> deleteResource(rr, path)));
            }
        });
        visitor.setLeafVisitor((res, level) -> {
            if (!res.getName().equals(POLICY_NODE_NAME)) {
                String path = res.getPath();
                manager.deferredWithResolver(Actions.retry(10, 100, rr -> deleteResource(rr, path)));
            }
        });
        manager.deferredWithResolver(rr -> {
            Resource res = rr.getResource(startingFolder);
            if (res != null) {
                visitor.accept(res);
            }
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
    }
}
