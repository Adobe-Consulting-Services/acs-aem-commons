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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import com.adobe.acs.commons.mcp.util.StringUtil;

/**
 * The abstract base class for a report;
 *
 */

public abstract class AbstractReport {


    // the implementation needs to take care of filling these variables with the
    // correct data
    protected List<String> columnsData;
    protected List<ValueMap> rowsData;
    protected String nameData = "report";

    /**
     * Persist all data stored in the properties
     * 
     * @param rr   a resourceresolver to use
     * @param path the path to store the report at
     * @throws PersistenceException in case of problems
     * @throws RepositoryException  in case of problems
     */
    public abstract void persist(ResourceResolver rr, String path) throws PersistenceException, RepositoryException;
    
    
    public <E extends Enum<E>, V> void setRows(Map<String, EnumMap<E, V>> reportData, String keyName,
            Class<E> enumClass) throws PersistenceException, RepositoryException {
        getColumns().clear();
        getColumns().add(keyName);
        Stream.of().map(Object::toString).collect(Collectors.toCollection(this::getColumns));
        for (Enum e : enumClass.getEnumConstants()) {
            this.getColumns().add(e.toString());
            ValueFormat format = ValueFormat.forField(e);
            if (format.columnCount > 1) {
                this.getColumns().add(e.toString() + format.suffix);
            }
        }
        getRows().clear();
        reportData.forEach((path, row) -> {
            Map<String, Object> rowData = new LinkedHashMap<>();
            rowData.put(keyName, path);
            for (Enum<E> c : enumClass.getEnumConstants()) {
                if (row.containsKey(c)) {
                    ValueFormat format = ValueFormat.forField(c);
                    rowData.put(c.toString(), row.get(c));
                    if (format.columnCount > 1) {
                        rowData.put(c.toString() + format.suffix, format.getAlternateValue(row.get(c)));
                    }
                }
            }
            getRows().add(new ValueMapDecorator(rowData));
        });
    }

    public <E extends Enum<E>, V> void setRows(List<EnumMap<E, V>> reportData, Class<E> enumClass)
            throws PersistenceException, RepositoryException {
        getColumns().clear();
        Stream.of().map(Object::toString).collect(Collectors.toCollection(this::getColumns));
        for (Enum e : enumClass.getEnumConstants()) {
            this.getColumns().add(e.toString());
            ValueFormat format = ValueFormat.forField(e);
            if (format.columnCount > 1) {
                this.getColumns().add(e.toString() + format.suffix);
            }
        }
        getRows().clear();
        reportData.forEach(row -> {
            Map<String, Object> rowData = new LinkedHashMap<>();
            for (Enum<E> c : enumClass.getEnumConstants()) {
                if (row.containsKey(c)) {
                    ValueFormat format = ValueFormat.forField(c);
                    rowData.put(c.toString(), row.get(c));
                    if (format.columnCount > 1) {
                        rowData.put(c.toString() + format.suffix, format.getAlternateValue(row.get(c)));
                    }
                }
            }
            getRows().add(new ValueMapDecorator(rowData));
        });
    }

    /**
     * @return the columns
     */
    public List<String> getColumns() {
        if (columnsData == null) {
            columnsData = new ArrayList<>();
        }
        return columnsData;
    }

    /**
     * @return the rows
     */
    public List<ValueMap> getRows() {
        if (rowsData == null) {
            rowsData = new ArrayList<>();
        }
        return rowsData;
    }

    public String getName() {
        return nameData;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.nameData = name;
    }

    public List<String> getColumnNames() {
        return columnsData.stream().map(StringUtil::getFriendlyName).collect(Collectors.toList());
    }

}
