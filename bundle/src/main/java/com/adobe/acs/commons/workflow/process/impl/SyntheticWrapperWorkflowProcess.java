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

package com.adobe.acs.commons.workflow.process.impl;

import com.adobe.acs.commons.fam.ThrottledTaskRunner;
import com.adobe.acs.commons.util.ParameterUtil;
import com.adobe.acs.commons.util.WorkflowHelper;
import com.adobe.acs.commons.util.visitors.ContentVisitor;
import com.adobe.acs.commons.util.visitors.ResourceRunnable;
import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowModel;
import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowRunner;
import com.adobe.acs.commons.workflow.synthetic.impl.SyntheticWorkflowRunnerAccessor;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component(
        metatype = true,
        label = "ACS AEM Commons - Workflow Process - Synthetic Workflow Wrapper Process",
        description = "Executes an AEM Workflow model as a Synthetic Workflow using FAM"
)
@Properties({
        @Property(
                label = "Workflow Label",
                name = "process.label",
                value = "Synthetic Workflow Wrapper",
                description = "Executes an AEM Workflow model as a Synthetic Workflow (serial execution)"
        )
})
@Service
public class SyntheticWrapperWorkflowProcess implements WorkflowProcess {
    private static final Logger log = LoggerFactory.getLogger(SyntheticWrapperWorkflowProcess.class);
    private static final String ARG_TRAVERSE_TREE = "traverseTree";
    private static final String ARG_SAVE_INTERVAL = "saveInterval";
    private static final String ARG_WORKFLOW_MODEL_ID = "workflowModelId";
    private static final String ARG_THROTTLE = "throttle";

    @Reference
    private SyntheticWorkflowRunnerAccessor syntheticWorkflowRunnerAccessor;

    @Reference
    private ThrottledTaskRunner throttledTaskRunner;

    @Reference
    private WorkflowHelper workflowHelper;

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        ResourceResolver resourceResolver = null;
        final SyntheticWorkflowRunner syntheticWorkflowRunner = syntheticWorkflowRunnerAccessor.getSyntheticWorkflowRunner();

        final String payload = (String) workItem.getWorkflowData().getPayload();
        final ProcessArgs processArgs = new ProcessArgs(metaDataMap);

        try {
            resourceResolver = workflowHelper.getResourceResolver(workflowSession);
            final SyntheticWorkflowModel syntheticWorkflowModel = syntheticWorkflowRunner.getSyntheticWorkflowModel(resourceResolver, processArgs.getWorkflowModelId(), true);

            final AtomicInteger count = new AtomicInteger(0);

            // Anonymous inner class to facilitate counting of processed payloads
            final ResourceRunnable syntheticRunnable = new ResourceRunnable() {
                @Override
                public void run(final Resource resource) throws java.lang.Exception {
                    if (processArgs.isThrottle()) {
                        throttledTaskRunner.waitForLowCpuAndLowMemory();
                    }

                    syntheticWorkflowRunner.execute(resource.getResourceResolver(), resource.getPath(), syntheticWorkflowModel, false, false);

                    // Commit as needed
                    if (processArgs.getSaveInterval() > 0
                            && count.incrementAndGet() % processArgs.getSaveInterval() == 0
                            && resource.getResourceResolver().hasChanges()) {
                        resource.getResourceResolver().commit();
                    }
                }
            };

            final ContentVisitor visitor = new ContentVisitor(syntheticRunnable);
            final Resource resource = resourceResolver.getResource(payload);

            if (processArgs.isTraverseTree()) {
                visitor.accept(resource);
            } else {
                syntheticRunnable.run(resource);
            }

            if (processArgs.getSaveInterval() > 0 && resourceResolver.hasChanges()) {
                // Commit any stranglers
                resourceResolver.commit();
            }

            log.info("Synthetic Workflow Wrapper processed [ {} ] total payloads", count.get());
        } catch (Exception e) {
            throw new WorkflowException(e);
        }
    }

    /**
     * ProcessArgs parsed from the WF metadata map
     */
    private  static class ProcessArgs {
        private boolean traverseTree = false;
        private boolean throttle = false;
        private String workflowModelId;
        int saveInterval;

        public ProcessArgs(MetaDataMap map) throws WorkflowException {
            String[] lines = StringUtils.split(map.get(WorkflowHelper.PROCESS_ARGS, ""), System.lineSeparator());
            Map<String, String> data = ParameterUtil.toMap(lines, "=");

            throttle = Boolean.parseBoolean(data.get(ARG_THROTTLE));
            traverseTree = Boolean.parseBoolean(data.get(ARG_TRAVERSE_TREE));
            workflowModelId = data.get(ARG_WORKFLOW_MODEL_ID);
            try {
                saveInterval = Integer.parseInt(data.get(ARG_SAVE_INTERVAL));
            } catch (NumberFormatException e) {
                log.warn("Could not parse int from [ {} ]", data.get(ARG_SAVE_INTERVAL));
                saveInterval = 100;
            }

            if (StringUtils.isBlank(workflowModelId)) {
                throw new WorkflowException("Unable to parse the workflowModelId from the Workflow Process Args");
            }
        }

        public String getWorkflowModelId() {
            return workflowModelId;
        }

        public boolean isTraverseTree() {
            return traverseTree;
        }

        public int getSaveInterval() {
            return saveInterval;
        }

        public boolean isThrottle() {
            return throttle;
        }
    }
}