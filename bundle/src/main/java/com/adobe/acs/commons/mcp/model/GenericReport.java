/*
 * Copyright 2017 Adobe.
 *
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
 */
package com.adobe.acs.commons.mcp.model;

import com.adobe.acs.commons.mcp.ProcessInstance;
import com.day.cq.commons.jcr.JcrUtil;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

/**
 * Describes a very simple table, which is up to the process definition to outline.
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class GenericReport {
    @Inject
    private List<String> columns;
    
    @Inject
    private List<ValueMap> rows;
    
    public String getResourceType() {
        return ProcessInstance.RESOURCE_TYPE + "/process-generic-report";
    }
    
    public void persist(ResourceResolver rr, String path) throws PersistenceException, RepositoryException {
        ModifiableValueMap jcrContent = ResourceUtil.getOrCreateResource(rr, path, getResourceType(), null, false).adaptTo(ModifiableValueMap.class);
        jcrContent.put("jcr:primaryType","nt:unstructured");
        jcrContent.put("columns", getColumns().toArray(new String[0]));
        rr.commit();
        rr.refresh();
        JcrUtil.createPath(path+"/rows", "nt:unstructured", rr.adaptTo(Session.class));
        int rowCounter = 0;
        for (Map<String, Object> row : rows) {
            rowCounter++;
            ResourceUtil.getOrCreateResource(rr, path+"/rows/row-"+rowCounter, row, null, true);
        }
        rr.commit();
        rr.refresh();
    }

    public <E extends Enum<E>, V> void setRows(Map<String, EnumMap<E, V>> reportData, String keyName, Class<E> enumClass) throws PersistenceException, RepositoryException {
        getColumns().clear();
        getColumns().add(keyName);
        Stream.of(enumClass.getEnumConstants()).map(Object::toString).collect(Collectors.toCollection(this::getColumns));
        getRows().clear();
        reportData.forEach((path, row)->{
            Map<String, Object> r = new LinkedHashMap<>();
            r.put(keyName, path);
            for (Enum<E> c : enumClass.getEnumConstants()) {
                if (row.containsKey(c)) {
                    r.put(c.toString(), row.get(c));
                }
            }
            getRows().add(new ValueMapDecorator(r));
        });
    }

    /**
     * @return the columns
     */
    public List<String> getColumns() {
        if (columns == null) {
            columns = new ArrayList<>();
        }
        return columns;
    }

    /**
     * @return the rows
     */
    public List<ValueMap> getRows() {
        if (rows == null) {
            rows = new ArrayList<>();
        }
        return rows;
    }
}
