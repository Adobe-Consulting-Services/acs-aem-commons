/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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

import com.adobe.acs.commons.util.ParameterUtil;
import com.adobe.acs.commons.util.WorkflowHelper;
import com.adobe.acs.commons.workflow.WorkflowPackageManager;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.vault.util.PathUtil;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.List;
import java.util.Map;

@Component(
        service = WorkflowProcess.class,
        property = "process.label=Copy properties"
)
public class CopyPropertiesProcess implements WorkflowProcess {
    private static final Logger log = LoggerFactory.getLogger(CopyPropertiesProcess.class);
    private static final String PN_PROPERTY_MAP = "PROPERTY_MAP";
    private static final String SEPARATOR = "->";
    private static final String ALTERNATE_SEPARATOR = "=>";
    private static final String EVENT_DATA = "acs-aem-commons.workflow.copy-properties";

    @Reference
    private WorkflowPackageManager workflowPackageManager;

    @Reference
    private WorkflowHelper workflowHelper;

    @Override
    public final void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        String wfPayload = null;

        try (ResourceResolver resourceResolver = workflowHelper.getResourceResolver(workflowSession)) {
            wfPayload = (String) workItem.getWorkflowData().getPayload();

            final List<String> payloads = workflowPackageManager.getPaths(resourceResolver, wfPayload);

            for (final String payload : payloads) {
                copyProperties(metaDataMap, resourceResolver, payload);
            }

            if (resourceResolver.hasChanges()) {
                setJcrSessionUserData(resourceResolver);
            }
        } catch (RepositoryException e) {
            throw new WorkflowException(String.format("Could not find the payload for '%s'", wfPayload), e);
        }
    }

    protected void setJcrSessionUserData(ResourceResolver resourceResolver) {
        try {
            resourceResolver.adaptTo(Session.class).getWorkspace().getObservationManager().setUserData(EVENT_DATA);
        } catch (RepositoryException e) {
            log.warn("Unable to set user-data to [ {} ]", EVENT_DATA, e);
        }
    }

    protected void copyProperties(MetaDataMap metaDataMap, ResourceResolver resourceResolver, String payload) {
        final Resource resource = workflowHelper.getPageOrAssetResource(resourceResolver, payload);

        if (resource == null) {
            log.warn("Could not process payload [ {} ] as it could be resolved to a Page or Asset", payload);
            return;
        }

        final String[] propertyMaps = metaDataMap.get(PN_PROPERTY_MAP, new String[]{});

        for (String propertyMap : propertyMaps) {
            propertyMap = StringUtils.replace(propertyMap, ALTERNATE_SEPARATOR, SEPARATOR);
            Map.Entry<String, String> entry = ParameterUtil.toMapEntry(propertyMap, SEPARATOR);

            try {
                PropertyResource source = new PropertyResource(StringUtils.trim(entry.getKey()), resource.getPath(), resourceResolver);
                PropertyResource destination = new PropertyResource(StringUtils.trim(entry.getValue()), resource.getPath(), resourceResolver);

                /**
                 * IF the property exists on the source AND is empty on the source AND the property exists on the destination THEN set the destination property to empty.
                 * IF the property exists on the source AND is empty on the source AND the property does not exist on the destination THEN do nothing (leave the destination alone)
                 * IF the property doesn’t exist on the source, THEN remove the property from the destination
                 * ELSE, copy the value from the source to the destination.
                 */

                if (source.propertyExists() && !source.hasValue() && destination.propertyExists()) {
                    log.debug("Remove destination property during copy properties of [ {} -> {} ] because source property exists and has no value, and destination has the property", source, destination);
                    destination.setValue(null);
                } else if (!source.propertyExists() && destination.propertyExists()) {
                    log.debug("Remove destination property during copy properties of [ {} -> {} ] because source property does not exists, and destination has the property", source, destination);
                    destination.setValue(null);
                } else if (source.propertyExists() && !source.hasValue() && !destination.propertyExists()) {
                    log.debug("Do nothing. Skipping [ {} -> {} ] because source has no value, and destination is missing the property", source, destination);
                } else {
                    log.debug("Setting [ {} ] value during copy properties of [ {} -> {} ]", source.getValue(), source, destination);
                    destination.setValue(source.getValue());
                }
            } catch (WorkflowException e) {
                log.error("Could not copy properties [ {} -> {} ] for payload [ {} ]", //NOPMD - Flagged as false positive
                        new String[]{entry.getKey(), entry.getValue(), resource.getPath()} , e);
            }
        }
    }

    static class PropertyResource {
        private final String propertyName;
        private final Resource resource;

        public PropertyResource(String mapProperty, String payload, ResourceResolver resourceResolver) throws WorkflowException {
            String resourcePath = StringUtils.substringBeforeLast(mapProperty, "/");
            propertyName = StringUtils.substringAfterLast(mapProperty, "/");

            if (!StringUtils.startsWith(resourcePath, "/")) {
                resourcePath = PathUtil.makePath(payload, resourcePath);
            }

            resource = resourceResolver.getResource(resourcePath);

            if (resource == null || StringUtils.isBlank(propertyName)) {
                throw new WorkflowException(String.format("Unable to parse valid resource and property combination from [ %s + %s ]",
                       payload, mapProperty));
            }
        }

        public boolean propertyExists() {
            return resource.getValueMap().containsKey(propertyName);
        }

        public boolean hasValue() {
            final Object value = getValue();

            if (value == null) { return false; }
            if ((value instanceof String) && StringUtils.isBlank((String) value)) { return false; }
            if ((value instanceof Object[]) && ((Object[])value).length == 0) { return false; }

            return true;
        }

        public Object getValue() {
            return resource.getValueMap().get(propertyName);
        }

        public void setValue(Object value) {
            final ModifiableValueMap properties = resource.adaptTo(ModifiableValueMap.class);
            if (properties != null) {
                if (value != null) {
                    properties.put(propertyName, value);
                } else if (properties.containsKey(propertyName)){
                    properties.remove(propertyName);
                }
            }
        }

        public String toString() {
            return String.format("%s/%s (Property %s)",
                    resource.getPath(),
                    propertyName,
                    propertyExists() ? "exists" : "does not exist");
        }
    }
}



