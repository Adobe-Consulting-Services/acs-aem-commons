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

package com.adobe.acs.commons.workflow.bulk.impl;

import com.adobe.acs.commons.workflow.bulk.BulkWorkflowEngine;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.NameConstants;
import org.apache.commons.lang.ArrayUtils;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.AbstractResourceVisitor;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ResumableResourceVisitor extends AbstractResourceVisitor {
    private static final Logger log = LoggerFactory.getLogger(BulkWorkflowEngineImpl.class);

    private static final String NT_PAGE_CONTENT = "cq:PageContent";
    private static final String[] ACCEPTED_PRIMARY_TYPES = new String[] { NameConstants.NT_PAGE, NT_PAGE_CONTENT };
    private List<Resource> resources = new ArrayList<Resource>();
    
    public final List<Resource> getResumableResources() {
        return this.resources;
    }
    
    @Override
    public final void accept(final Resource resource) {
        final ValueMap properties = resource.adaptTo(ValueMap.class);
        final String primaryType = properties.get(JcrConstants.JCR_PRIMARYTYPE, String.class);

        if (BulkWorkflowEngine.BULK_WORKFLOW_MANAGER_PAGE_FOLDER_PATH.equals(resource.getPath())) {
            super.accept(resource);
        } else if (ArrayUtils.contains(ACCEPTED_PRIMARY_TYPES, primaryType)) {
            super.accept(resource);
        }
    }
    
    @Override
    protected void visit(final Resource resource) {
        final ValueMap properties = resource.adaptTo(ValueMap.class);
        
        if(NT_PAGE_CONTENT.equals(properties.get(JcrConstants.JCR_PRIMARYTYPE, String.class))) {
            if(BulkWorkflowEngine.SLING_RESOURCE_TYPE.equals(properties.get(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, String.class))) {
                if(BulkWorkflowEngine.STATE_STOPPED_DEACTIVATED.equals(properties.get(BulkWorkflowEngine.KEY_STATE, String.class))) {
                    this.resources.add(resource);
                }
            }
        }
        
        return;
    }
}
