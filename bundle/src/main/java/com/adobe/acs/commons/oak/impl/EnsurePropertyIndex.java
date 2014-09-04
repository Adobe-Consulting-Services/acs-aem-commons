/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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
package com.adobe.acs.commons.oak.impl;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.util.AemCapabilityHelper;

@Component(configurationFactory = true, metatype = true, label = "ACS AEM Commons - Ensure Oak Property Index",
        description = "Component Factory to create Oak property indexes.")
public class EnsurePropertyIndex {

    private static final String PN_REINDEX = "reindex";

    private static final String TYPE_PROPERTY = "property";

    private static final String PN_PROPERTY_NAMES = "propertyNames";

    private static final String NN_OAK_INDEX = "oak:index";

    private static final String NT_QID = "oak:QueryIndexDefinition";

    private static final String PN_TYPE = "type";

    @Property
    private static final String PROP_INDEX_NAME = "index.name";

    @Property
    private static final String PROP_PROPERTY_NAME = "property.name";

    private static final Logger log = LoggerFactory.getLogger(EnsurePropertyIndex.class);

    @Reference
    private SlingRepository repository;

    @Reference
    private AemCapabilityHelper capabilityHelper;

    @Activate
    protected void activate(Map<String, Object> properties) throws RepositoryException {
        if (capabilityHelper.isOak()) {
            String name = PropertiesUtil.toString(properties.get(PROP_INDEX_NAME), null);
            String propertyName = PropertiesUtil.toString(properties.get(PROP_PROPERTY_NAME), null);

            if (name == null && propertyName == null) {
                log.warn("Incomplete configure; name or property name is null.");
                return;
            }

            Session session = null;
            try {
                session = repository.loginAdministrative(null);

                Node oakIndexContainer = session.getRootNode().getNode(NN_OAK_INDEX);
                if (oakIndexContainer.hasNode(name)) {
                    Node indexNode = oakIndexContainer.getNode(name);
                    if (needsUpdate(indexNode, propertyName)) {
                        updateIndex(indexNode, propertyName);
                    }
                } else {
                    createIndex(oakIndexContainer.addNode(name, NT_QID), propertyName);
                }
                session.save();

            } catch (RepositoryException e) {
                log.error("Unable to create index", e);
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        } else {
            log.info("Cowardly refusing to create indexes on non-Oak instance.");
        }
    }

    private boolean needsUpdate(Node indexNode, String propertyName) throws RepositoryException {
        Value[] currentPropertyNames = indexNode.getProperty(PN_PROPERTY_NAMES).getValues();
        return !currentPropertyNames[0].getString().equals(propertyName);
    }

    private void updateIndex(Node indexNode, String propertyName) throws RepositoryException {
        createIndex(indexNode, propertyName);
        indexNode.setProperty(PN_REINDEX, true);
    }

    private void createIndex(Node indexNode, String propertyName) throws RepositoryException {
        ValueFactory valueFactory = indexNode.getSession().getValueFactory();

        indexNode.setProperty(PN_TYPE, TYPE_PROPERTY);
        indexNode.setProperty(PN_PROPERTY_NAMES, new Value[] { valueFactory.createValue(propertyName, PropertyType.NAME) });
    }

}
