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

package com.adobe.acs.commons.workflow.bulk.execution.impl;

import com.adobe.acs.commons.workflow.bulk.execution.BulkWorkflowEngine;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.NameConstants;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.AbstractResourceVisitor;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigResourceVisitor extends AbstractResourceVisitor {

    private static final String BULK_WORKFLOW_MANAGER_PAGE_FOLDER_PATH = "/etc/acs-commons/bulk-workflow-manager";
    private static final String NT_PAGE_CONTENT = "cq:PageContent";
    private static final String[] ACCEPTED_PRIMARY_TYPES = new String[]{NameConstants.NT_PAGE, NT_PAGE_CONTENT};
    private List<Resource> configurations = new ArrayList<Resource>();

    public final List<Resource> getConfigs() {
        return Collections.unmodifiableList(this.configurations);
    }

    @Override
    public final void accept(final Resource resource) {
        // Only accept the Root folder and cq:Page and cq:PageContent nodes; All other structures are uninteresting
        // to this functionality and may be very large
        if (StringUtils.startsWith(resource.getPath(), BULK_WORKFLOW_MANAGER_PAGE_FOLDER_PATH)) {
            final ValueMap properties = resource.adaptTo(ValueMap.class);
            final String primaryType = properties.get(JcrConstants.JCR_PRIMARYTYPE, String.class);

            if (BULK_WORKFLOW_MANAGER_PAGE_FOLDER_PATH.equals(resource.getPath())
                    || ArrayUtils.contains(ACCEPTED_PRIMARY_TYPES, primaryType)) {
                super.accept(resource);
            }
        }
    }

    @Override
    protected void visit(final Resource resource) {
        final ValueMap properties = resource.adaptTo(ValueMap.class);

        // Ensure jcr:primaryType = cq:PageContent and 
        // that the sling:resourceType is that of Bulk Workflow Manager Page
        if (NT_PAGE_CONTENT.equals(properties.get(JcrConstants.JCR_PRIMARYTYPE, String.class))
                && StringUtils.equals(BulkWorkflowEngine.SLING_RESOURCE_TYPE,
                    properties.get(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, String.class))) {
            this.configurations.add(resource);
        }
    }
}
