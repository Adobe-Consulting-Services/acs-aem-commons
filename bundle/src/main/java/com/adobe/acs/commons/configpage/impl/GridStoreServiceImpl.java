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
package com.adobe.acs.commons.configpage.impl;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.configpage.GridOperationFailedException;
import com.adobe.acs.commons.configpage.GridStoreService;
import com.day.cq.dam.api.cache.BufferedImageCache.Entry;

@Component(label = "ACS AEM Commons - Edit Grid - Grid store service", description = "Facilitates updating/deleting a grid row", immediate = false, metatype = true)
@Service
public class GridStoreServiceImpl implements GridStoreService {
    private static final Logger log = LoggerFactory
            .getLogger(GridStoreServiceImpl.class);

    @Override
    public boolean deleteRows(ResourceResolver resolver, List<String> keys,
            Resource resource) throws GridOperationFailedException {
        try {
            Session session = resolver.adaptTo(Session.class);
            for (String key : keys) {

                Resource rowResource = resource.getChild(key);
                if (rowResource != null) {
                    Node node = rowResource.adaptTo(Node.class);
                    node.remove();
                }
            }

            session.save();
            return true;
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            throw new GridOperationFailedException(e.getMessage());
        }

    }

    @Override
    public boolean addOrUpdateRows(ResourceResolver resolver,
            List<Map<String, String>> rows, Resource resource)
            throws GridOperationFailedException {
        Node gridNode = resource.adaptTo(Node.class);
        Session session = resolver.adaptTo(Session.class);
        try {
            for (Map<String, String> row : rows) {
                Node rowNode = JcrUtils.getOrCreateUniqueByPath(gridNode,
                        row.get(COLUMN_UID), JcrConstants.NT_UNSTRUCTURED);
                for (Map.Entry entry : row.entrySet()) {
                    rowNode.setProperty((String) entry.getKey(),
                            (String) entry.getValue());
                }
            }
            session.save();

        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            throw new GridOperationFailedException(e.getMessage());
        }
        return true;
    }
}
