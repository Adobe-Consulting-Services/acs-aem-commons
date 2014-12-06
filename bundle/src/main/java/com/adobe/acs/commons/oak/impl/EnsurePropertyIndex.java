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
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.util.AemCapabilityHelper;

@Component(configurationFactory = true, metatype = true, label = "ACS AEM Commons - Ensure Oak Property Index",
        description = "Component Factory to create Oak property indexes.")
public class EnsurePropertyIndex {

    private static class IndexDefinition {
        private boolean async;
        private String[] declaringNodeTypes;
        private String propertyName;
        private boolean unique;
    }

    private static final boolean DEFAULT_ASYNC = false;

    private static final boolean DEFAULT_UNIQUE = false;

    private static final Logger log = LoggerFactory.getLogger(EnsurePropertyIndex.class);

    private static final String NN_OAK_INDEX = "oak:index";

    private static final String NT_QID = "oak:QueryIndexDefinition";

    private static final String PN_ASYNC = "async";

    private static final String PN_DECLARING_NODE_TYPES = "declaringNodeTypes";

    private static final String PN_PROPERTY_NAMES = "propertyNames";

    private static final String PN_REINDEX = "reindex";

    private static final String PN_TYPE = "type";

    private static final String PN_UNIQUE = "unique";

    @Property(label = "Async?", description = "Is this index async?", boolValue = DEFAULT_ASYNC)
    private static final String PROP_ASYNC = "index.async";

    @Property(label = "Index Name", description = "Will be used as the index node name.")
    private static final String PROP_INDEX_NAME = "index.name";

    @Property(label = "Declaring Node Types", description = "Declaring Node Types", unbounded = PropertyUnbounded.ARRAY)
    private static final String PROP_NODE_TYPES = "node.types";
    
    @Property(label = "Property Name", description = "Property name to index.")
    private static final String PROP_PROPERTY_NAME = "property.name";

    @Property(label = "Unique", description = "Is in this index unique?", boolValue = DEFAULT_UNIQUE)
    private static final String PROP_UNIQUE = "unique";

    private static final String TYPE_PROPERTY = "property";

    @Reference
    private AemCapabilityHelper capabilityHelper;

    @Reference
    private SlingRepository repository;

    private void createOrUpdateIndex(Node indexNode, IndexDefinition def) throws RepositoryException {
        ValueFactory valueFactory = indexNode.getSession().getValueFactory();

        indexNode.setProperty(PN_TYPE, TYPE_PROPERTY);
        indexNode.setProperty(PN_PROPERTY_NAMES,
                new Value[] { valueFactory.createValue(def.propertyName, PropertyType.NAME) });
        if (def.async) {
            indexNode.setProperty(PN_ASYNC, PN_ASYNC);
        } else if (indexNode.hasProperty(PN_ASYNC)) {
            indexNode.getProperty(PN_ASYNC).remove();
        }
        if (def.unique) {
            indexNode.setProperty(PN_UNIQUE, true);
        } else if (indexNode.hasProperty(PN_UNIQUE)) {
            indexNode.getProperty(PN_UNIQUE).remove();
        }
        if (def.declaringNodeTypes != null && def.declaringNodeTypes.length > 0) {
            Value[] values = new Value[def.declaringNodeTypes.length];
            for (int i = 0; i < def.declaringNodeTypes.length; i++) {
                values[i] = valueFactory.createValue(def.declaringNodeTypes[0], PropertyType.NAME);
            }
            indexNode.setProperty(PN_DECLARING_NODE_TYPES, values);
        } else if (indexNode.hasProperty(PN_DECLARING_NODE_TYPES)) {
            indexNode.getProperty(PN_DECLARING_NODE_TYPES).remove();
        }
    }

    private boolean needsUpdate(Node indexNode, IndexDefinition def) throws RepositoryException {
        Value[] currentPropertyNames = indexNode.getProperty(PN_PROPERTY_NAMES).getValues();
        return !currentPropertyNames[0].getString().equals(def.propertyName);
    }

    private void updateIndex(Node indexNode, IndexDefinition def) throws RepositoryException {
        createOrUpdateIndex(indexNode, def);
        indexNode.setProperty(PN_REINDEX, true);
    }

    @Activate
    protected void activate(Map<String, Object> properties) throws RepositoryException {
        if (capabilityHelper.isOak()) {
            String name = PropertiesUtil.toString(properties.get(PROP_INDEX_NAME), null);

            IndexDefinition def = new IndexDefinition();
            def.propertyName = PropertiesUtil.toString(properties.get(PROP_PROPERTY_NAME), null);
            def.async = PropertiesUtil.toBoolean(properties.get(PROP_ASYNC), DEFAULT_ASYNC);
            def.unique = PropertiesUtil.toBoolean(properties.get(PROP_UNIQUE), DEFAULT_UNIQUE);
            def.declaringNodeTypes = PropertiesUtil.toStringArray(properties.get(PROP_NODE_TYPES), new String[0]);

            if (name == null || def.propertyName == null) {
                log.warn("Incomplete configure; name or property name is null.");
                return;
            }

            Session session = null;
            try {
                session = repository.loginAdministrative(null);

                Node oakIndexContainer = session.getRootNode().getNode(NN_OAK_INDEX);
                if (oakIndexContainer.hasNode(name)) {
                    Node indexNode = oakIndexContainer.getNode(name);
                    if (needsUpdate(indexNode, def)) {
                        log.info("updating index {}", name);
                        updateIndex(indexNode, def);
                    } else {
                        log.debug("index {} does not need updating", name);
                    }
                } else {
                    log.info("creating index {}", name);
                    createOrUpdateIndex(oakIndexContainer.addNode(name, NT_QID), def);
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

}
