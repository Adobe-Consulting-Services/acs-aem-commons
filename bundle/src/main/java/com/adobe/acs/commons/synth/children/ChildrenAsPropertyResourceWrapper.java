package com.adobe.acs.commons.synth.children;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * resource@animals="{
 * <p/>
 * animal-1: {
 * property-1: "cat",
 * property-2: "meow"
 * },
 * <p/>
 * animal-1: {
 * property-1: "dog",
 * property-2: "bark"
 * }
 * }"
 */


public class ChildrenAsPropertyResourceWrapper extends ResourceWrapper {
    private static final Logger log = LoggerFactory.getLogger(ChildrenAsPropertyResourceWrapper.class);

    private static final String EMPTY_JSON = "{}";

    private static final String CALENDAR_ID = "{" + Calendar.class.getName() + "}";

    private static final String DATE_ID = "{" + Date.class.getName() + "}";

    private final Resource resource;

    private final String propertyName;

    private Map<String, Resource> lookupCache = null;

    private Set<Resource> orderedCache = null;

    private Comparator<Resource> comparator = null;

    public static final Comparator<Resource> RESOURCE_NAME_COMPARATOR = new ResourceNameComparator();

    /**
     * ResourceWrapper that allows resource children to be modeled in data stored into a property.
     *
     * @param resource     the resource to store the children as properties on
     * @param propertyName the property name to store the children as properties in
     */
    public ChildrenAsPropertyResourceWrapper(Resource resource, String propertyName) throws InvalidDataFormatException {
        this(resource, propertyName, null);
    }

    public ChildrenAsPropertyResourceWrapper(Resource resource, String propertyName, Comparator<Resource> comparator) throws InvalidDataFormatException {
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

    @Override
    public final Iterator<Resource> listChildren() {
        return IteratorUtils.getIterator(this.orderedCache);
    }

    @Override
    public final Iterable<Resource> getChildren() {
        return this.orderedCache;
    }


    @Override
    public final Resource getChild(String name) {
        return this.lookupCache.get(name);
    }

    @Override
    public final Resource getParent() {
        return this.resource;
    }

    public final Resource createChild(String name, String primaryType, Map<String, Object> data) throws PersistenceException {
        if (data == null) {
            data = new HashMap<String, Object>();
        }

        if (!data.containsKey(JcrConstants.JCR_PRIMARYTYPE)) {
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

    public final void removeChild(String name) throws PersistenceException {
        if (this.lookupCache.containsKey(name)) {
            Resource tmp = this.lookupCache.get(name);
            this.orderedCache.remove(tmp);
            this.lookupCache.remove(name);
        }
    }

    public final void removeChildren() throws InvalidDataFormatException {
        // Clear the caches; requires serialize
        if (this.comparator == null) {
            this.orderedCache = new LinkedHashSet<Resource>();
        } else {
            this.orderedCache = new TreeSet<Resource>(this.comparator);
        }

        this.lookupCache = new HashMap<String, Resource>();
    }

    public final void persist() throws PersistenceException {
        this.serialize();
    }

    private void serialize() throws InvalidDataFormatException {
        final long start = System.currentTimeMillis();

        final ModifiableValueMap modifiableValueMap = this.resource.adaptTo(ModifiableValueMap.class);
        JSONObject childrenJSON = new JSONObject();

        try {
            // Add the new entries to the JSON
            for (Resource childResource : this.orderedCache) {
                childrenJSON.put(childResource.getName(), this.serializeToJSON(childResource));
            }

            // Persist the JSON back to the Node
            modifiableValueMap.put(this.propertyName, childrenJSON.toString());

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
     * @param resource the resources to serialize to JSON.
     * @return the JSONObject representing the resources.
     * @throws JSONException
     */
    protected final JSONObject serializeToJSON(Resource resource) throws JSONException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        final DateTimeFormatter dtf = ISODateTimeFormat.dateTime();
        final Map<String, Object> serializedData = new HashMap<String, Object>();

        for (Map.Entry<String, Object> entry : resource.getValueMap().entrySet()) {
            if (entry.getValue() instanceof Calendar) {
                final Calendar cal = (Calendar) entry.getValue();
                serializedData.put(entry.getKey(), CALENDAR_ID + dtf.print(cal.getTimeInMillis()));
            } else if (entry.getValue() instanceof Date) {
                final Date date = (Date) entry.getValue();
                serializedData.put(entry.getKey(), DATE_ID + dtf.print(date.getTime()));
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
    protected final List<SyntheticChildAsPropertyResource> deserializeToSyntheticChildResources(JSONObject jsonObject) throws JSONException {
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
                final String propertyName = propertyNames.next();
                final String propertyStrValue = entryJSON.optString(propertyName);

                boolean calendar = this.isCalendarPropertyValue(propertyStrValue);
                boolean date = this.isDatePropertyValue(propertyStrValue);

                if (calendar || date) {
                    final String dateTimeStr = this.getCalendarOrDatePropertyValue(propertyStrValue);
                    final DateTime dateTime = ISODateTimeFormat.dateTime().parseDateTime(dateTimeStr);

                    if (calendar) {
                        final Calendar cal = Calendar.getInstance();
                        cal.setTime(dateTime.toDate());
                        properties.put(propertyName, cal);
                    } else {
                        properties.put(propertyName, dateTime.toDate());
                    }
                } else {
                    properties.put(propertyName, entryJSON.get(propertyName));
                }
            }

            resources.add(new SyntheticChildAsPropertyResource(this.getParent(), nodeName, properties));
        }

        return resources;
    }

    private boolean isCalendarPropertyValue(String propertyValue) {
        return StringUtils.startsWith(propertyValue, CALENDAR_ID);
    }

    private boolean isDatePropertyValue(String propertyValue) {
        return StringUtils.startsWith(propertyValue, DATE_ID);
    }

    private String getCalendarOrDatePropertyValue(String propertyValue) {
        if (StringUtils.startsWith(propertyValue, CALENDAR_ID)) {
            return StringUtils.removeStart(propertyValue, CALENDAR_ID);
        } else if (StringUtils.startsWith(propertyValue, DATE_ID)) {
            return StringUtils.removeStart(propertyValue, DATE_ID);
        } else {
            return propertyValue;
        }
    }

    /**
     * Sort by resource name ascending (resource.getName()).
     */
    private final static class ResourceNameComparator implements Comparator<Resource> {
        @Override
        public int compare(final Resource o1, final Resource o2) {
            return o1.getName().compareTo(o2.getName().toString());
        }
    }
}
