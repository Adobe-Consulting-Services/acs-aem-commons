package com.adobe.acs.commons.workflow.bulk.removal.impl;

import org.apache.commons.lang.ArrayUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

@Component(
    label = "ACS AEM Commons - Workflow Instance Remover"
)
@Service
public final class WorkflowInstanceRemoverImpl {
    private static final Logger log = LoggerFactory.getLogger(WorkflowInstanceRemoverImpl.class);

    private static final String WORKFLOW_INSTANCES_PATH = "/etc/workflow/instances";
    private static final String PN_STATUS = "status";
    private static final String NT_SLING_FOLDER = "sling:Folder";
    private static final String NT_CQ_WORKFLOW = "cq:Workflow";
    private static final int BATCH_SIZE = 1000;

    public final int removeWorkflowInstance(final ResourceResolver resourceResolver, final String... statuses) {
        final long start = System.currentTimeMillis();
        final Resource folders = resourceResolver.getResource(WORKFLOW_INSTANCES_PATH);

        int total = 0;
        int count = 0;
        Resource folderToDelete = null;

        for(final Resource folder : folders.getChildren()) {
            int remaining = 0;

            if(folderToDelete != null) {
                try {
                    folder.adaptTo(Node.class).remove();
                    // Incrementing only count to trigger batch save and not total since is not a WF
                    count++;
                } catch (RepositoryException e) {
                    log.error("Could not remove workflow folder at [ {} ] due to: {}",
                            folder.getPath(), e.getMessage());
                }
            }

            folderToDelete = null;


            if(!folder.isResourceType(NT_SLING_FOLDER)) {
                // Only process sling:Folders; skip rep:policy
                continue;
            }

            for(final Resource instance : folder.getChildren()) {

                if(!instance.isResourceType(NT_CQ_WORKFLOW)) {
                    // Only process cq:Workflow's
                    remaining++;
                    continue;
                }

                final ValueMap properties = instance.adaptTo(ValueMap.class);
                final String status = properties.get(PN_STATUS, String.class);

                if(ArrayUtils.contains(statuses, status)) {
                    // Only remove matching
                    try {
                        instance.adaptTo(Node.class).remove();
                        log.trace("Removed workflow instance at [ {} ]", instance.getPath());
                        count++;
                        total++;
                    } catch (RepositoryException e) {
                        log.error("Could not remove workflow instance at [ {} ] due to: {}",
                                instance.getPath(), e.getMessage());
                    }

                    if(count % BATCH_SIZE == 0) {
                        this.save(resourceResolver);
                        log.debug("Removed a running total of [ {} ] workflow instances", total);
                    }
                } else {
                    remaining++;
                }
            }

            if(remaining == 0) {
                // Marking this folder to be deleted as long as it is not the last folder
                // This folder will be deleted before processing the contents of the next folder

                // Leave folders for now since its hard to tell if they are in use

                //folderToDelete = folder;
            }
        }

        if(count > 0 && count % BATCH_SIZE != 0) {
            // Final batch size if needed
            this.save(resourceResolver);
        }

        log.info("Removed a total of [ {} ] workflow instances in [ {} ] ms", total, System.currentTimeMillis() - start);

        return total;
    }

    private void save(ResourceResolver resourceResolver) {
        final long start = System.currentTimeMillis();
        try {
            resourceResolver.adaptTo(Session.class).save();
            log.debug("Saving batch workflow instance removal in [ {} ] ms", System.currentTimeMillis() - start);
        } catch (RepositoryException e) {
            log.error("Could not save batch workflow instance remove: {}", e.getMessage());
        }
    }
}
