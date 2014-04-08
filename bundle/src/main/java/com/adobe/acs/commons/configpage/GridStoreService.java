/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
package com.adobe.acs.commons.configpage;

import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Service used to add, update and delete the configuration rows in the grid
 * config page.
 *  
 * 
 */
public interface GridStoreService {
    public static final String COLUMN_UID = "uid";

    /**
     * The method argument rows contains the list of uids to be deleted.This
     * method deletes all the nodes with the with these uids as nodenames.
     * 
     * @param resolver
     * @param rows
     * @param resource
     * @return
     * @throws GridOperationFailedException
     */
    public boolean deleteRows(ResourceResolver resolver, List<String> rows,
            Resource resource) throws GridOperationFailedException;

    /**
     * This method adds new nodes(with uid as node name) or updates existing
     * nodes with the given list of values.
     * 
     * @param resolver
     * @param rows
     * @param resource
     * @return
     * @throws GridOperationFailedException
     */
    public boolean addOrUpdateRows(ResourceResolver resolver,
            List<Map<String, String>> rows, Resource resource)
            throws GridOperationFailedException;
}
