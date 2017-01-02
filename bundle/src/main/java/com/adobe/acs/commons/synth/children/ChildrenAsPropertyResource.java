package com.adobe.acs.commons.synth.children;

import org.apache.commons.collections.IteratorUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Class to wrapper a real resource to facilitate the persistence of children resources in a property (serialized as
 * JSON).
 *
 * Can be used as follows...
 *
 * To write data:
 *
 * Resource real = resolve.getResource("/content/real");
 * ChildrenAsPropertyResource wrapper = new ChildrenAsPropertyResource(real);
 * Resource child = wrapper.create("child-1", "nt:unstructured");
 * ModifiableValueMap mvm = child.adaptTo(ModifiableValueMap.class);
 * mvm.put("prop-1", "some data");
 * mvm.put("prop-2", Calendar.getInstance());
 * wrapper.persist();
 * resolver.commit();
 *
 * To read data:
 *
 * Resource real = resolve.getResource("/content/real");
 * ChildrenAsPropertyResource wrapper = new ChildrenAsPropertyResource(real);
 * for(Resource child : wrapper.getChildren()) {
 *     child.getValueMap().get("prop-1", String.class);
 * }
 *
 */
public class ChildrenAsPropertyResource extends ResourceWrapper {
    private static final Logger log = LoggerFactory.getLogger(ChildrenAsPropertyResource.class);

    private static final String EMPTY_JSON = "{}";

    private static final String DEFAULT_PROPERTY_NAME = "children";

    private final Resource resource;

    private final String propertyName;

    private Map<String, Resource> lookupCache = null;

    private Set<Resource> orderedCache = null;

    private Comparator<Resource> comparator = null;

    public static final Comparator<Resource> RESOURCE_NAME_COMPARATOR = new ResourceNameComparator();

    /**
     * ResourceWrapper that allows resource children to be modeled in data stored into a property using the default
     * property name of "children".
     *
     * @param resource     the resource to store the children as properties on
     * @throws InvalidDataFormatException
     */
    public ChildrenAsPropertyResource(Resource resource) throws InvalidDataFormatException {
        this(resource, DEFAULT_PROPERTY_NAME, null);
    }

    /**
     * ResourceWrapper that allows resource children to be modeled in data stored into a property.
     *
     * @param resource     the resource to store the children as properties on
     * @param propertyName the property name to store the children as properties in
     */
    public ChildrenAsPropertyResource(Resource resource, String propertyName) throws InvalidDataFormatException {
        this(resource, propertyName, null);
    }

    /**
     * ResourceWrapper that allows resource children to be modeled in data stored into a property.
     *
     * @param resource     the resource to store the children as properties on
     * @param propertyName the property name to store the children as properties in
     * @param comparator   the comparator used to order the serialized children
     * @throws InvalidDataFormatException
     */
    public ChildrenAsPropertyResource(Resource resource, String propertyName, Comparator<Resource> comparator)
            throws InvalidDataFormatException {
        super(resource);

        this.resource = resource;
        this.propertyName = propertyName;
        this.comparator = comparator;

        if (this.comparator == null) {
            this.orderedCache = new LinkedHashSet<Resource>();
        } else {
            this.orderedCache = new TreeSet<Resource>(this.comparator);
        }

        this.lookupCache = new HashMap<String, Resource>();

        for (SyntheticChildAsPropertyResource r : this.deserialize()) {
            this.orderedCache.add(r);
            this.lookupCache.put(r.getName(), r);
        }
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    public final Iterator<Resource> listChildren() {
        return IteratorUtils.getIterator(this.orderedCache);
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    public final Iterable<Resource> getChildren() {
        return this.orderedCache;
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    public final Resource getChild(String name) {
        return this.lookupCache.get(name);
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    public final Resource getParent() {
        return this.resource;
    }


    public final Resource create(String name, String primaryType) throws RepositoryException {
        return create(name, primaryType, null);
    }

    public final Resource create(String name, String primaryType, Map<String, Object> data) throws RepositoryException {
        if (data == null) {
            data = new HashMap<String, Object>();
        }

        if (data.containsKey(JcrConstants.JCR_PRIMARYTYPE) && primaryType != null) {
            data.put(JcrConstants.JCR_PRIMARYTYPE, primaryType);
        }

        final SyntheticChildAsPropertyResource child =
                new SyntheticChildAsPropertyResource(this.resource, name, data);

        if (this.lookupCache.containsKey(child.getName())) {
            log.info("Existing synthetic child [ {} ] overwritten", name);
        }

        this.lookupCache.put(child.getName(), child);
        this.orderedCache.add(child);

        return child;
    }

    /**
     * Deletes the named child.
     *
     * Requires subsequent call to persist().
     *
     * @param name the child node name to delete
     * @throws RepositoryException
     */
    public final void delete(String name) throws RepositoryException {
        if (this.lookupCache.containsKey(name)) {
            Resource tmp = this.lookupCache.get(name);
            this.orderedCache.remove(tmp);
            this.lookupCache.remove(name);
        }
    }

    /**
     * Delete all children.
     *
     * Requires subsequent call to persist().
     *
     * @throws InvalidDataFormatException
     */
    public final void deleteAll() throws InvalidDataFormatException {
        // Clear the caches; requires serialize
        if (this.comparator == null) {
            this.orderedCache = new LinkedHashSet<Resource>();
        } else {
            this.orderedCache = new TreeSet<Resource>(this.comparator);
        }

        this.lookupCache = new HashMap<String, Resource>();
    }

    /**
     * Persist changes to the underlying valuemap so they are available for persisting to the JCR.
     *
     * @throws RepositoryException
     */
    public final void persist() throws RepositoryException {
        this.serialize();
    }


    /**
     * Serializes all children data as JSON to the resource's propertyName.
     *
     * @throws InvalidDataFormatException
     */
    private void serialize() throws InvalidDataFormatException {
        final long start = System.currentTimeMillis();

        final ModifiableValueMap modifiableValueMap = this.resource.adaptTo(ModifiableValueMap.class);
        JSONObject childrenJSON = new JSONObject();

        try {
            // Add the new entries to the JSON
            for (Resource childResource : this.orderedCache) {
                childrenJSON.put(childResource.getName(), this.serializeToJSON(childResource));
            }

            if (childrenJSON.length() > 0) {
                // Persist the JSON back to the Node
                modifiableValueMap.put(this.propertyName, childrenJSON.toString());
            } else {
                // Nothing to persist; delete the property
                modifiableValueMap.remove(this.propertyName);
            }

            log.debug("Persist operation for [ {} ] in [ {} ms ]",
                    this.resource.getPath() + "/" + this.propertyName,
                    System.currentTimeMillis() - start);

        } catch (JSONException e) {
            throw new InvalidDataFormatException(this.resource, this.propertyName, childrenJSON.toString());
        } catch (NoSuchMethodException e) {
            throw new InvalidDataFormatException(this.resource, this.propertyName, childrenJSON.toString());
        } catch (IllegalAccessException e) {
            throw new InvalidDataFormatException(this.resource, this.propertyName, childrenJSON.toString());
        } catch (InvocationTargetException e) {
            throw new InvalidDataFormatException(this.resource, this.propertyName, childrenJSON.toString());
        }
    }

    /**
     * Convert the serialized JSON data found in the node property to Resources.
     *
     * @return the list of children sorting using the comparator.
     * @throws InvalidDataFormatException
     */
    private List<SyntheticChildAsPropertyResource> deserialize() throws InvalidDataFormatException {
        final long start = System.currentTimeMillis();

        final String propertyData = this.resource.getValueMap().get(this.propertyName, EMPTY_JSON);

        List<SyntheticChildAsPropertyResource> resources;

        try {
            resources = deserializeToSyntheticChildResources(new JSONObject(propertyData));
        } catch (JSONException e) {
            throw new InvalidDataFormatException(this.resource, this.propertyName, propertyData);
        }

        if (this.comparator != null) {
            Collections.sort(resources, this.comparator);
        }

        log.debug("Get operation for [ {} ] in [ {} ms ]",
                this.resource.getPath() + "/" + this.propertyName,
                System.currentTimeMillis() - start);

        return resources;
    }

    /**
     * Converts a list of SyntheticChildAsPropertyResource to their JSON representation, keeping the provided order.
     *
     * @param resourceToSerialize the resource to serialize to JSON.
     * @return the JSONObject representing the resources.
     * @throws JSONException
     */
    protected final JSONObject serializeToJSON(final Resource resourceToSerialize)
            throws JSONException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        final DateTimeFormatter dtf = ISODateTimeFormat.dateTime();
        final Map<String, Object> serializedData = new HashMap<String, Object>();

        for (Map.Entry<String, Object> entry : resourceToSerialize.getValueMap().entrySet()) {
            if (entry.getValue() instanceof Calendar) {
                final Calendar cal = (Calendar) entry.getValue();
                serializedData.put(entry.getKey(), dtf.print(cal.getTimeInMillis()));
            } else if (entry.getValue() instanceof Date) {
                final Date date = (Date) entry.getValue();
                serializedData.put(entry.getKey(), dtf.print(date.getTime()));
            } else {
                serializedData.put(entry.getKey(), entry.getValue());
            }
        }

        return new JSONObject(serializedData);
    }

    /**
     * Converts a JSONObject to the list of SyntheticChildAsPropertyResources.
     *
     * @param jsonObject the JSONObject to deserialize.
     * @return the list of SyntheticChildAsPropertyResources the jsonObject represents.
     * @throws JSONException
     */
    protected final List<SyntheticChildAsPropertyResource> deserializeToSyntheticChildResources(JSONObject jsonObject)
            throws JSONException {
        final List<SyntheticChildAsPropertyResource> resources = new ArrayList<SyntheticChildAsPropertyResource>();

        final Iterator<String> keys = jsonObject.keys();

        while (keys.hasNext()) {
            final String nodeName = keys.next();

            JSONObject entryJSON = jsonObject.optJSONObject(nodeName);

            if (entryJSON == null) {
                continue;
            }

            final ValueMap properties = new ValueMapDecorator(new HashMap<String, Object>());
            final Iterator<String> propertyNames = entryJSON.keys();

            while (propertyNames.hasNext()) {
                final String propName = propertyNames.next();
                properties.put(propName, entryJSON.optString(propName));
            }

            resources.add(new SyntheticChildAsPropertyResource(this.getParent(), nodeName, properties));
        }

        return resources;
    }

    /**
     * Sort by resource name ascending (resource.getName()).
     */
    private static final class ResourceNameComparator implements Comparator<Resource>, Serializable {
        private static final long serialVersionUID = 0L;

        @Override
        public int compare(final Resource o1, final Resource o2) {
            return o1.getName().compareTo(o2.getName().toString());
        }
    }
}
