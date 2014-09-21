package com.adobe.acs.commons.workflow.bulk.removal.impl;

import com.adobe.acs.commons.workflow.bulk.removal.WorkflowInstanceFolderComparator;
import com.adobe.acs.commons.workflow.bulk.removal.WorkflowInstanceRemover;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
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
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.regex.Pattern;

@Component(
    label = "ACS AEM Commons - Workflow Instance Remover"
)
@Service
public final class WorkflowInstanceRemoverImpl implements WorkflowInstanceRemover {
    private static final Logger log = LoggerFactory.getLogger(WorkflowInstanceRemoverImpl.class);

    private static final String WORKFLOW_INSTANCES_PATH = "/etc/workflow/instances";
    private static final String PN_STATUS = "status";
    private static final String PN_START_TIME = "startTime";
    private static final String PN_PAYLOAD_PATH = "data/payload/path";

    private static final String NT_SLING_FOLDER = "sling:Folder";
    private static final String NT_CQ_WORKFLOW = "cq:Workflow";
    private static final int BATCH_SIZE = 1000;

    /**
     * {@inheritDoc}
     */
    public final int removeWorkflowInstances(final ResourceResolver resourceResolver,
                                             final Collection<String> models,
                                             final Collection<String> statuses,
                                             final Collection<Pattern> payloads,
                                             final Calendar olderThan) {
        final long start = System.currentTimeMillis();
        final Resource folders = resourceResolver.getResource(WORKFLOW_INSTANCES_PATH);
        final Collection<Resource> sortedFolders = this.getSortedAndFilteredFolders(folders);

        int total = 0;
        int count = 0;
        for(final Resource folder : sortedFolders) {
            int remaining = 0;

            for(final Resource instance : folder.getChildren()) {

                if (!instance.isResourceType(NT_CQ_WORKFLOW)) {
                    // Only process cq:Workflow's
                    remaining++;
                    continue;
                }

                final ValueMap properties = instance.adaptTo(ValueMap.class);
                final String status = properties.get(PN_STATUS, String.class);
                final String model = StringUtils.removeStart(StringUtils.removeEnd(properties.get("modelId", String.class),
                        "/jcr:content/model"), "/etc/workflow/models/");
                final Calendar startTime = properties.get(PN_START_TIME, Calendar.class);
                final String payload = properties.get(PN_PAYLOAD_PATH, String.class);

                if (CollectionUtils.isNotEmpty(statuses) && !statuses.contains(status)) {
                    log.trace("Workflow instance [ {} ] has non-matching status of [ {} ]", instance.getPath(), status);
                    remaining++;
                    continue;
                } else if (CollectionUtils.isNotEmpty(models) && !models.contains(model)) {
                    log.trace("Workflow instance [ {} ] has non-matching model of [ {} ]", instance.getPath(), model);
                    remaining++;
                    continue;
                } else if (olderThan != null && startTime != null && startTime.before(olderThan)) {
                    log.trace("Workflow instance [ {} ] has non-matching start time of [ {} ]", instance.getPath(),
                            startTime);
                    remaining++;
                    continue;
                } else {

                    if (CollectionUtils.isNotEmpty(payloads)) {
                        // Only evaluate payload patterns if they are provided

                        boolean match = false;

                        if (StringUtils.isNotEmpty(payload)) {
                            for (final Pattern pattern : payloads) {
                                if (payload.matches(pattern.pattern())) {
                                    // payload matches a pattern
                                    match = true;
                                    break;
                                }
                            }

                            if (!match) {
                                // Not a match; skip to next workflow instance
                                log.trace("Workflow instance [ {} ] has non-matching payload path [ {} ]",
                                        instance.getPath(), payload);
                                remaining++;
                                continue;
                            }
                        }
                    }

                    // Only remove matching

                    try {
                        instance.adaptTo(Node.class).remove();
                        log.trace("Removed workflow instance at [ {} ]", instance.getPath());
                        count++;
                        total++;
                    } catch (RepositoryException e) {
                        log.error("Could not remove workflow instance at [ {} ]",
                                instance.getPath(), e);
                    }

                    if(count % BATCH_SIZE == 0) {
                        this.save(resourceResolver);
                        log.debug("Removed a running total of [ {} ] workflow instances", total);
                    }
                }
            }

            if(remaining == 0) {
                try {
                    folder.adaptTo(Node.class).remove();
                    // Incrementing only count to trigger batch save and not total since is not a WF
                    count++;
                } catch (RepositoryException e) {
                    log.error("Could not remove workflow folder at [ {} ]", folder.getPath(), e);
                }
            }
        }

        if(count > 0 && count % BATCH_SIZE != 0) {
            // Final batch size if needed
            this.save(resourceResolver);
        }

        log.info("Removed a total of [ {} ] workflow instances in [ {} ] ms", total, System.currentTimeMillis() - start);

        return total;
    }

    private Collection<Resource> getSortedAndFilteredFolders(Resource folderResource) {
        final Collection<Resource> sortedCollection = new TreeSet(new WorkflowInstanceFolderComparator());
        final Iterator<Resource> folders = folderResource.listChildren();

        while(folders.hasNext()) {
            final Resource folder = folders.next();

            if(!folder.isResourceType(NT_SLING_FOLDER)) {
                // Only process sling:Folders; eg. skip rep:Policy
                continue;
            } else {
                sortedCollection.add(folder);
            }
        }

        return sortedCollection;
    }

    private void save(ResourceResolver resourceResolver) {
        final long start = System.currentTimeMillis();
        try {
            resourceResolver.adaptTo(Session.class).save();
            log.debug("Saving batch workflow instance removal in [ {} ] ms", System.currentTimeMillis() - start);
        } catch (RepositoryException e) {
            log.error("Could not save batch workflow instance remove: {}", e);
        }
    }
}
