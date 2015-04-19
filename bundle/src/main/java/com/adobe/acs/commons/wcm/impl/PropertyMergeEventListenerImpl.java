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
package com.adobe.acs.commons.wcm.impl;

import com.adobe.acs.commons.util.InfoWriter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEventListener;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Component(
        label = "ACS AEM Commons - Property Merge Factory",
        description = "Merges the values from multiple source properties into a single property as a multi-value.",
        immediate = true,
        metatype = true,
        configurationFactory = true,
        policy = ConfigurationPolicy.REQUIRE
)
@Properties({
        @Property(
                label = "Event Topics",
                value = {SlingConstants.TOPIC_RESOURCE_ADDED, SlingConstants.TOPIC_RESOURCE_CHANGED},
                description = "[Required] Event Topics this event handler will to respond to.",
                name = EventConstants.EVENT_TOPIC,
                propertyPrivate = true
        )
})
@Service
public final class PropertyMergeEventListenerImpl implements EventHandler, TopologyEventListener {
    private static final Logger log = LoggerFactory.getLogger(PropertyMergeEventListenerImpl.class);
    private static final long TOO_LONG_IN_MS = 500;

    private static final String PROPERTY_TYPE_BOOLEAN = "boolean";
    // Underlying type used is Calendar to support ModifiableValueMap
    private static final String PROPERTY_TYPE_DATE = "date";
    private static final String PROPERTY_TYPE_DOUBLE = "double";
    private static final String PROPERTY_TYPE_LONG = "long";
    private static final String PROPERTY_TYPE_STRING = "string";

    private boolean isLeader = false;

    private static final String DEFAULT_PROPERTY_TYPE = PROPERTY_TYPE_STRING;
    private Class propertyType = String[].class;
    @Property(label = "Property Type",
            description = "The property type of the Destinations and Source properties.",
            value = PROPERTY_TYPE_STRING,
            options = {
                    @PropertyOption(name = PROPERTY_TYPE_BOOLEAN, value = "Boolean"),
                    @PropertyOption(name = PROPERTY_TYPE_DATE, value = "Date"),
                    @PropertyOption(name = PROPERTY_TYPE_DOUBLE, value = "Double"),
                    @PropertyOption(name = PROPERTY_TYPE_LONG, value = "Long"),
                    @PropertyOption(name = PROPERTY_TYPE_STRING, value = "String")
            })
    public static final String PROP_PROPERTY_TYPE = "property-type";

    private static final String[] DEFAULT_NODE_TYPES = new String[]{};
    private List<String> nodeTypes = new ArrayList<String>();
    @Property(label = "Node Types",
            description = "The node types to merge tags against. Leave blank for any.",
            cardinality = Integer.MAX_VALUE,
            value = {})
    public static final String PROP_NODE_TYPES = "node-types";

    private static final String[] DEFAULT_RESOURCE_TYPES = new String[]{};
    private List<String> resourceTypes = new ArrayList<String>();
    @Property(label = "Resource Types",
            description = "The resource types to merge tags against. Leave blank for any.",
            cardinality = Integer.MAX_VALUE,
            value = {})
    public static final String PROP_RESOURCE_TYPES = "resource-types";


    private static final String DEFAULT_DESTINATION_PROPERTY = "";
    private String destinationProperty = DEFAULT_DESTINATION_PROPERTY;
    @Property(label = "Destination Property",
            description = "The property to merge Tags into.",
            value = DEFAULT_DESTINATION_PROPERTY)
    public static final String PROP_DESTINATION_PROPERTY = "destination-property";


    private static final String[] DEFAULT_SOURCE_PROPERTIES = new String[]{};
    @Property(label = "Source Properties",
            description = "The properties to collect the tags for merging from.",
            cardinality = Integer.MAX_VALUE,
            value = {})
    public static final String PROP_SOURCE_PROPERTIES = "source-properties";

    private List<String> sourceProperties = new ArrayList<String>();

    private static final boolean DEFAULT_ALLOW_DUPLICATES = false;
    private boolean allowDuplicates = DEFAULT_ALLOW_DUPLICATES;
    @Property(label = "Allow duplicates",
            description = "True allows duplicate values to be added to the destination. "
                    + "False forces a unique set of values.",
            boolValue = DEFAULT_ALLOW_DUPLICATES)
    public static final String PROP_ALLOW_DUPLICATES = "allow-duplicate-values";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public void handleEvent(final Event event) {
        if (!this.isLeader) {
            return;
        }

        final long start = System.currentTimeMillis();

        final String[] added = (String[]) event.getProperty(SlingConstants.PROPERTY_ADDED_ATTRIBUTES);
        final String[] changed = (String[]) event.getProperty(SlingConstants.PROPERTY_CHANGED_ATTRIBUTES);
        final String[] removed = (String[]) event.getProperty(SlingConstants.PROPERTY_REMOVED_ATTRIBUTES);

        final List<String> delta = new ArrayList<String>();

        if (ArrayUtils.isNotEmpty(added)) {
            delta.addAll(Arrays.asList(added));
        }

        if (ArrayUtils.isNotEmpty(changed)) {
            delta.addAll(Arrays.asList(changed));
        }

        if (ArrayUtils.isNotEmpty(removed)) {
            delta.addAll(Arrays.asList(removed));
        }

        if (CollectionUtils.containsAny(this.sourceProperties, delta)) {

            final String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);

            ResourceResolver resourceResolver = null;
            try {
                resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

                final Resource resource = resourceResolver.getResource(path);
                if (resource == null) {
                    log.error("Property merge event handler attempting to work on a non-existing resource [ {} ]",
                            path);
                    return;
                }

                // Check Node Types

                boolean acceptNodeType = true;
                if (CollectionUtils.isNotEmpty(this.nodeTypes)) {
                    final Node node = resource.adaptTo(Node.class);
                    if (node == null) {
                        log.warn("Property merge event handler attempting to work on a non-existing node [ {} ]",
                                path);
                        return;
                    }

                    acceptNodeType = false;

                    for (final String nodeType : this.nodeTypes) {
                        if (node.isNodeType(nodeType)) {
                            acceptNodeType = true;
                            break;
                        }
                    }
                }

                if (!acceptNodeType) {
                    log.debug("Rejecting Property merge for [ {} ] due to node type mismatch", path);
                    return;
                }

                // Check Resource Types

                boolean acceptResourceType = true;
                if (CollectionUtils.isNotEmpty(this.resourceTypes)) {
                    acceptResourceType = false;

                    for (final String resourceType : this.resourceTypes) {
                        if (resource.isResourceType(resourceType)) {
                            acceptResourceType = true;
                            break;
                        }
                    }
                }

                if (!acceptResourceType) {
                    log.debug("Rejecting Property merge for [ {} ] due to resource type mismatch", path);
                    return;
                }

                this.merge(resource, this.destinationProperty, this.sourceProperties, this.propertyType);

            } catch (LoginException e) {
                log.error("Could not obtain a ResourceResolver for Property merging", e);
            } catch (PersistenceException e) {
                log.error("Could not persist tag merging", e);
            } catch (RepositoryException e) {
                log.error("Could not check the Node Type of the resource for Property merging", e);
            } finally {
                if (resourceResolver != null) {
                    resourceResolver.close();
                }

                final long duration = System.currentTimeMillis() - start;

                if (duration > TOO_LONG_IN_MS) {
                    log.warn("Property merge an alarming long time of {} ms. "
                                    + "Long running events may become blacklisted.",
                            duration);
                }
            }
        }

    }

    /**
     * Merges the values found in the the source properties into the destination property as a multi-value.
     * The values of the source properties and destination properties must all be the same property type.
     * <p/>
     * The unique set of properties will be stored in
     *
     * @param resource    the resource to look for the source and destination properties on
     * @param destination the property to store the collected properties.
     * @param sources
     */
    protected <T> void merge(final Resource resource, final String destination,
                                   final List<String> sources, Class<T> klass) throws PersistenceException {

        // Create an empty array of type T
        @SuppressWarnings("unchecked")
        final T[] emptyArray = (T[]) Array.newInstance(klass, 0);

        final ModifiableValueMap properties = resource.adaptTo(ModifiableValueMap.class);

        Collection<T> collectedValues = null;

        if (this.allowDuplicates) {
            collectedValues = new ArrayList<T>();
        } else {
            collectedValues = new LinkedHashSet<T>();
        }

        for (final String source : sources) {
            // Get the source value as type T
            final T[] tmp = properties.get(source, emptyArray);

            // If the value is not null, add to collectedValues
            if (tmp != null) {
                collectedValues.addAll(Arrays.asList(tmp));
            }
        }

        final T[] currentValues = properties.get(destination, emptyArray);

        if (!collectedValues.equals(Arrays.asList(currentValues))) {
            properties.put(destination, collectedValues.toArray(emptyArray));

            if (resource.getResourceResolver().hasChanges()) {
                resource.getResourceResolver().commit();
                log.info("Property merge performed at [ " + resource.getPath() + "@{} ] with values {}",
                        destination, collectedValues);
            }
        }
    }

    @Activate
    protected void activate(final Map<String, String> config) {
        final InfoWriter iw = new InfoWriter();

        iw.title("Property Merge Configuration");


        // Allow Duplicate Values

        this.allowDuplicates = PropertiesUtil.toBoolean(config.get(PROP_ALLOW_DUPLICATES), DEFAULT_ALLOW_DUPLICATES);

        iw.message("Allow Duplicates: {}", this.allowDuplicates);

        // Property Type

        final String propType = PropertiesUtil.toString(config.get(PROP_PROPERTY_TYPE), DEFAULT_PROPERTY_TYPE);
        if (PROPERTY_TYPE_BOOLEAN.equals(propType)) {
            this.propertyType = Boolean.class;
        } else if (PROPERTY_TYPE_DATE.equals(propType)) {
            this.propertyType = Calendar.class;
        } else if (PROPERTY_TYPE_DOUBLE.equals(propType)) {
            this.propertyType = Double.class;
        } else if (PROPERTY_TYPE_LONG.equals(propType)) {
            this.propertyType = Long.class;
        } else {
            this.propertyType = String.class;
        }

        iw.message("Property Type: {}", this.propertyType.getSimpleName());

        // Node Types

        this.nodeTypes = new ArrayList<String>();
        String[] tmp = PropertiesUtil.toStringArray(config.get(PROP_NODE_TYPES),
                DEFAULT_NODE_TYPES);

        for (final String t : tmp) {
            if (StringUtils.isNotBlank(t)) {
                this.nodeTypes.add(t);
            }
        }

        if (CollectionUtils.isEmpty(this.nodeTypes)) {
            iw.message("Node Types: All Node Types");
        } else {
            iw.message("Node Types: {}", this.nodeTypes);
        }

        // Resource Types

        this.resourceTypes = new ArrayList<String>();
        tmp = PropertiesUtil.toStringArray(config.get(PROP_RESOURCE_TYPES),
                DEFAULT_RESOURCE_TYPES);

        for (final String t : tmp) {
            if (StringUtils.isNotBlank(t)) {
                this.resourceTypes.add(t);
            }
        }

        if (CollectionUtils.isEmpty(this.resourceTypes)) {
            iw.message("Resource Types: All Resource Types");
        } else {
            iw.message("Node Types: {}", this.resourceTypes);
        }

        // Destination Property

        this.destinationProperty = PropertiesUtil.toString(config.get(PROP_DESTINATION_PROPERTY),
                DEFAULT_DESTINATION_PROPERTY);

        iw.message("Destination Property: {}", this.destinationProperty);
        if (StringUtils.isBlank(this.destinationProperty)) {
            log.warn("Property Merge destination property is Empty.");
        }

        // Source Property

        this.sourceProperties = Arrays.asList(PropertiesUtil.toStringArray(config.get(PROP_SOURCE_PROPERTIES),
                DEFAULT_SOURCE_PROPERTIES));

        iw.message("Source Properties: {}", this.sourceProperties);
        if (CollectionUtils.isEmpty(this.sourceProperties)) {
            log.warn("Property Merge source properties list is Empty.");
        }


        iw.close();
        log.info(iw.toString());
    }

    @Override
    public void handleTopologyEvent(final TopologyEvent event) {
        if (event.getType() == TopologyEvent.Type.TOPOLOGY_CHANGED
                || event.getType() == TopologyEvent.Type.TOPOLOGY_INIT) {
            this.isLeader = event.getNewView().getLocalInstance().isLeader();
        }
    }
}
