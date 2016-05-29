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

import com.adobe.acs.commons.workflow.WorkflowPackageManager;
import com.adobe.granite.asset.api.AssetManager;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.DamUtil;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.List;

@Component
@Properties({
        @Property(
                label = "Workflow Label",
                name = "process.label",
                value = "DAM Asset Metadata Move",
                description = "Moves assets to DAM folders based on metadata property/values"
        )
})
@Service
public class DamAssetMetadataMoveWorkflowProcess implements WorkflowProcess {
    private static final Logger log = LoggerFactory.getLogger(DamAssetMetadataMoveWorkflowProcess.class);

    private static final String PN_FOLDER_CONFIG = "acsAssetMetadataMoveConfig";
    private static final String ARGS_CONFIG_DELAY = "config:delay";
    private static int DEFAULT_DELAY = 5; // seconds

    @Reference
    private WorkflowPackageManager workflowPackageManager;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public final void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap args) throws WorkflowException {
        boolean dirty = false;
        ResourceResolver resourceResolver = null;


        try {
            resourceResolver = workflowSession.adaptTo(ResourceResolver.class);
            AssetManager assetManager = resourceResolver.adaptTo(AssetManager.class);

            delay(resourceResolver, args);

            final List<String> payloads = workflowPackageManager.getPaths(resourceResolver, (String) workItem.getWorkflowData().getPayload());
            for (final String payload : payloads) {
                final Asset asset = DamUtil.resolveToAsset(resourceResolver.getResource(payload));

                if (asset == null) {
                    log.debug("Payload path [ {} ] does not resolve to an asset", payload);
                    return;
                }

                final FolderConfig folderConfig = new FolderConfig(asset);
                final String destFolderPath = folderConfig.getDestination(asset);
                final String destAssetPath = destFolderPath + "/" + asset.getName();

                log.debug("Requesting move of [ {} ] to [ {} ] ", asset.getPath(), destFolderPath);
                if (StringUtils.startsWith(destFolderPath, "/content/dam")) {
                    Resource destResource = getOrCreateAssetFolder(resourceResolver, destFolderPath);

                    if (destResource == null) {
                        log.warn("Could not find a destination resource at [ {} ]. This is very odd, Check permissions.", destFolderPath);
                    } else if (StringUtils.equals(asset.getPath(), destAssetPath)) {
                        log.debug("Requesting NOOP of moving an asset to the folder its already in.");
                    } else {
                        log.info("Moving asset [ {} ] to [ {} ]", asset.getPath(), destAssetPath);
                        assetManager.moveAsset(asset.getPath(), destAssetPath);
                        dirty = true;
                    }
                }
            } // end for

            if (dirty) {
                resourceResolver.commit();

                // Terminating the Workflow as it is unknown what may come after this step, at which point the payload will not exist so the WF will fail
                log.debug("Success! Now self-terminated workflow. It is unknown what may come after this step, at which point the payload will not exist so the WF will fail");
                workflowSession.terminateWorkflow(workItem.getWorkflow());
            } else {
                log.debug("No dam asset movement was requested");
            }
        } catch (RepositoryException e) {
            log.error("Could not process DAM Status Move Workflow Process step", e);
        } catch (PersistenceException e) {
            log.error("Could not process DAM Status Move Workflow Process step", e);
        }
    }

    /**
     * Get or create an Asset folder.
     *
     * @param resourceResolver the resourceResolver obj that must be able to read and write
     * @param path             the absolute path of the Asset folder to get or create
     * @return the resource representing the Asset folder at destPath
     * @throws RepositoryException
     */
    private Resource getOrCreateAssetFolder(ResourceResolver resourceResolver, String path) throws RepositoryException {
        Resource assetFolderResource = resourceResolver.getResource(path);

        if (assetFolderResource == null) {
            Node node = JcrUtils.getOrCreateByPath(path,
                    JcrResourceConstants.NT_SLING_ORDERED_FOLDER, resourceResolver.adaptTo(Session.class));
            assetFolderResource = resourceResolver.getResource(node.getPath());
        }

        return assetFolderResource;
    }

    /**
     * Looks up the Asset Folder tree until a jcr:content node is found with a acsAssetMetadataMoveConfig property.
     *
     * @param folder the Asset folder to begin with
     * @return the String[] of acsAssetMetadataMoveConfig mappings
     */
    private String[] findClosestFolderConfig(Resource folder) {
        Resource jcrContent = folder.getChild(JcrConstants.JCR_CONTENT);

        if (jcrContent != null) {
            ValueMap properties = jcrContent.getValueMap();

            if (properties.keySet().contains(PN_FOLDER_CONFIG)) {
                return properties.get(PN_FOLDER_CONFIG, new String[]{});
            }
        }

        if (folder.getParent() != null) {
            return findClosestFolderConfig(folder.getParent());
        } else {
            return null;
        }
    }

    /**
     * Looks up the Asset node structure until a sling:Folder or sling:OrderedFolder is found.
     *
     * @param resource the Asset resource
     * @return the closest  sling:Folder or sling:OrderedFolder resource
     */
    private Resource findClosestFolder(Resource resource) {
        String[] assetFolderTypes = new String[]{JcrResourceConstants.NT_SLING_FOLDER, JcrResourceConstants.NT_SLING_ORDERED_FOLDER};

        String primaryType = resource.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE, String.class);

        if (ArrayUtils.contains(assetFolderTypes, primaryType)) {
            log.debug("Found asset's first parent folder at [ {} ]", resource.getPath());
            return resource;
        } else {
            if (resource.getParent() != null) {
                return findClosestFolder(resource.getParent());
            } else {
                return null;
            }
        }
    }

    /**
     * Inject configured delay.
     *
     * @param args Workflow metadata map w PROCESS_ARGS key
     */
    private void delay(ResourceResolver resourceResolver, MetaDataMap args) throws WorkflowException {
        int delay = DEFAULT_DELAY;
        String processArgs = StringUtils.stripToEmpty(args.get("PROCESS_ARGS", ""));

        if (StringUtils.startsWith(processArgs, ARGS_CONFIG_DELAY)) {
            String delayStr = StringUtils.stripToNull(StringUtils.removeStart(processArgs, ARGS_CONFIG_DELAY));
            if (delayStr != null) {
                try {
                    delay = Integer.parseInt(delayStr);
                } catch (Exception e) {
                    log.warn("Unable to parse delay arg; defaulting to [ {} ] seconds", delay, e);
                }
            }
        }

        if (delay > 0) {
            try {
                // Sleep N seconds before moving to avoid funny reload behavior in Review Task UI
                log.info("Sleeping for a configured [ {} ] seconds", delay);
                Thread.sleep(1000 * delay);
                resourceResolver.refresh();
            } catch (InterruptedException e) {
                throw new WorkflowException(e);
            }
        }
    }

    /**
     * Represents the set of DAM Asset Metadata Move folder configurations.
     * <p>
     * dam:status == approved -> /content/dam/approved
     * dam:status == rejected -> /content/dam/rejected
     */
    final class FolderConfig {
        private List<ProcessArgsEntry> argEntries = new ArrayList<ProcessArgsEntry>();

        public FolderConfig(Asset asset) {
            if (asset != null) {

                Resource folder = findClosestFolder(asset.adaptTo(Resource.class));
                if (folder != null) {
                    final String[] lines = findClosestFolderConfig(folder);

                    if (lines != null) {
                        for (String line : lines) {
                            String property = StringUtils.stripToNull(StringUtils.substringBefore(line, "=="));
                            String value = StringUtils.stripToNull(StringUtils.substringBetween(line, "==", "->"));
                            String destination = StringUtils.stripToNull(StringUtils.substringAfterLast(line, "->"));

                            if (property != null && destination != null) {
                                argEntries.add(new ProcessArgsEntry(property, value, destination));
                            }
                        }
                    }
                } else {
                    log.warn("Could not locate a parent folder for Asset [ {} ]", asset.getPath());
                }
            }
        }

        /**
         * For the given asset, find the first matching destination.
         *
         * @param asset the Asset to move
         * @return the absolute path of the destination, or null of none can be found
         */
        public String getDestination(Asset asset) {
            for (ProcessArgsEntry argEntry : argEntries) {
                String assetValue = asset.getMetadataValue(argEntry.getProperty());

                if (assetValue == null && argEntry.getValue() == null) {
                    return argEntry.getDestination();
                } else if (StringUtils.equals(argEntry.getValue(), assetValue)) {
                    return argEntry.getDestination();
                }
            }

            return null;
        }

        /**
         * Represents the <property> == <value> -> <destination> configuration.
         */
        public class ProcessArgsEntry {
            private String property;
            private String value;
            private String destination;

            public ProcessArgsEntry(String property, String value, String destination) {
                this.property = property;
                this.value = value;
                this.destination = destination;
            }

            public String getProperty() {
                return property;
            }

            public String getValue() {
                return value;
            }

            public String getDestination() {
                return destination;
            }
        }
    }
}
