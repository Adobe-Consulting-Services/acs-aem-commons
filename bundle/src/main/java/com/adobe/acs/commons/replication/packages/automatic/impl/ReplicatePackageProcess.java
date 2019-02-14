/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 - Adobe
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
package com.adobe.acs.commons.replication.packages.automatic.impl;

import com.adobe.acs.commons.util.WorkflowHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.Replicator;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;

/**
 * Workflow process for kicking off an AutomaticPackageReplicatorJob
 */
@Component
@Property(label = "Workflow Label", name = "process.label", value = "Build and Replicate Package", description = "Builds and Replicates a Package of Content, set the path of the package to replicate as the argument.")
@Service
public class ReplicatePackageProcess implements WorkflowProcess {

    private static final Logger log = LoggerFactory.getLogger(ReplicatePackageProcess.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Replicator replicator;

    @Reference
    private EventAdmin eventAdmin;

    /*
     * (non-Javadoc)
     *
     * @see
     * com.day.cq.workflow.exec.WorkflowProcess#execute(com.day.cq.workflow.exec
     * .WorkItem, com.day.cq.workflow.WorkflowSession,
     * com.day.cq.workflow.metadata.MetaDataMap)
     */
    @Override
    public void execute(WorkItem item, WorkflowSession session, MetaDataMap args) throws WorkflowException {
        log.trace("execute");
        String packagePath = args.get(WorkflowHelper.PROCESS_ARGS, String.class);
        if (StringUtils.isNotEmpty(packagePath)) {
            log.debug("Executing Automatic Package Replicator Job for package {}", packagePath);
            AutomaticPackageReplicatorJob aprJob = new AutomaticPackageReplicatorJob(resourceResolverFactory,
                    replicator, eventAdmin, packagePath);
            try {
                aprJob.excute();
            } catch (Exception e) {
                log.error("Exception executing Automatic Package Replicator Job for package " + packagePath, e);
                throw new WorkflowException(
                        "Exception executing Automatic Package Replicator Job for package " + packagePath, e);
            }
        } else {
            log.warn("No package path specified");
            throw new WorkflowException("No package path specified for Automatic Package Replicator Job");
        }
    }

}
