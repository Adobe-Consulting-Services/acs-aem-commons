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
    private static final String PN_SKIP_EMPTY_SOURCE_PROPERTY = "SKIP_EMPTY_SOURCE_PROPERTY";
    private static final String SEPARATOR = "->";
    private static final String ALTERNATE_SEPARATOR = "=>";

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
                try {
                    resourceResolver.adaptTo(Session.class).getWorkspace().getObservationManager().setUserData("acs-aem-commons.copy-properties");
                } catch (RepositoryException e) {
                    log.warn("Unable to set user-data to [ acs-aem-commons.copy-properties ]", e);
                }
            }
        } catch (RepositoryException e) {
            throw new WorkflowException(String.format("Could not find the payload for '%s'", wfPayload), e);
        }
    }

    protected void copyProperties(MetaDataMap metaDataMap, ResourceResolver resourceResolver, String payload) {
        final Resource resource = workflowHelper.getPageOrAssetResource(resourceResolver, payload);

        if (resource == null) {
            log.debug("Could not process payload [ {} ] as it could be resolved to a Page or Asset", payload);
            return;
        }

        final Boolean skipEmptyValue = metaDataMap.get(PN_SKIP_EMPTY_SOURCE_PROPERTY, Boolean.TRUE);
        final String[] propertyMaps = metaDataMap.get(PN_PROPERTY_MAP, new String[]{});

        for (String propertyMap : propertyMaps) {
            propertyMap = StringUtils.replace(propertyMap, ALTERNATE_SEPARATOR, SEPARATOR);
            Map.Entry<String, String> entry = ParameterUtil.toMapEntry(propertyMap, SEPARATOR);

            try {
                PropertyResource source = new PropertyResource(StringUtils.trim(entry.getKey()), resource.getPath(), resourceResolver);
                PropertyResource destination = new PropertyResource(StringUtils.trim(entry.getValue()), resource.getPath(), resourceResolver);

                destination.setValue(source.getValue(), skipEmptyValue);
            } catch (WorkflowException e) {
                log.error("Could not copy properties", e);
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
                        new String[]{payload, mapProperty}));
            }
        }

        public Object getValue() {
            return resource.getValueMap().get(propertyName);
        }

        public void setValue(Object value, boolean skipEmptyValue) {
            if (skipEmptyValue) {
                if (value == null) { return; }
                if ((value instanceof String) && StringUtils.isBlank((String) value)) { return; }
                if ((value instanceof Object[]) && ((Object[])value).length == 0) { return; }
            }

            final ModifiableValueMap properties = resource.adaptTo(ModifiableValueMap.class);
            if (properties != null) {
                properties.put(propertyName, value);
            } else {
                log.error("Could not set [ {} ] to [ {} ] due to ModifiableValueMap being null for resource [ {} ]",
                        propertyName, value, resource.getPath());
            }
        }
    }
}



