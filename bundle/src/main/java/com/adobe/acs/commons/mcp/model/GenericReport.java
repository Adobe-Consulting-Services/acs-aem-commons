/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.mcp.model;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.day.cq.commons.jcr.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.osgi.annotation.versioning.ProviderType;

import com.adobe.acs.commons.mcp.ProcessInstance;
import com.day.cq.commons.jcr.JcrUtil;

/**
 * Describes a very simple table, which is up to the process definition to
 * outline. This report type is not efficient with large number of resulting
 * rows, because it creates a JCR node for each row.
 */
@ProviderType
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class GenericReport extends AbstractReport {
    public static final String GENERIC_REPORT_RESOURCE_TYPE = ProcessInstance.RESOURCE_TYPE + "/process-generic-report";

    @Inject
    private List<String> columns;

    @Inject
    private List<ValueMap> rows;

    @Inject
    private String name = "report";

    public String getResourceType() {
        return GENERIC_REPORT_RESOURCE_TYPE;
    }
    
    @PostConstruct
    public void init() {
        this.columnsData = columns;
        this.rowsData = rows;
    }

    public void persist(ResourceResolver rr, String path) throws PersistenceException, RepositoryException {
        ModifiableValueMap jcrContent = ResourceUtil.getOrCreateResource(rr, path, getResourceType(), null, false).adaptTo(ModifiableValueMap.class);
        jcrContent.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        jcrContent.put("columns", getColumns().toArray(new String[0]));
        jcrContent.put("name", name);
        rr.commit();
        rr.refresh();
        JcrUtil.createPath(path + "/rows", JcrConstants.NT_UNSTRUCTURED, rr.adaptTo(Session.class));
        int rowCounter = 0;
        for (Map<String, Object> row : this.getRows()) {
            // First strip out null values
            Map<String, Object> properties = row.entrySet().stream().filter(e -> e.getValue() != null).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            rowCounter++;
            ResourceUtil.getOrCreateResource(rr, path + "/rows/row-" + rowCounter, properties, null, true);
        }
        rr.commit();
        rr.refresh();
    }


}
