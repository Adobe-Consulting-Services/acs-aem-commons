package com.adobe.acs.commons.synth.children;

import org.apache.commons.lang.StringUtils;
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
import java.util.List;
import java.util.Map;

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
    private static final String CALENDAR_ID = "<SERIALIZED:CALENDAR>";
    private static final String DATE_ID = "<SERIALIZED:DATE>";

    private final Resource resource;

    private final String propertyName;

    public static final Comparator<Resource> RESOURCE_NAME_COMPARATOR = new ResourceNameComparator();

    /**
     * ResourceWrapper that allows resource children to be modeled in data stored into a property.
     *
     * @param resource     the resource to store the children as properties on
     * @param propertyName the property name to store the children as properties in
     */
    public ChildrenAsPropertyResourceWrapper(Resource resource, String propertyName) {
        super(resource);

        this.resource = resource;
        this.propertyName = propertyName;
    }

    /**
     * Add a resource representation to the existing list.
     * <p/>
     * Note: If this is called repeatedly, it is more efficient to queue up all additions and pass them into the List
     * based add(..) method.
     *
     * @param childResource the resource to add
     * @param comparator    used to define the persisted order of the children resources; optional - pass null if unneeded
     * @throws PersistenceException
     * @throws InvalidDataFormatException
     */
    public void add(SyntheticChildAsPropertyResource childResource, Comparator<Resource> comparator)
            throws PersistenceException, InvalidDataFormatException {

        final List<SyntheticChildAsPropertyResource> childResources = new ArrayList<SyntheticChildAsPropertyResource>();
        childResources.add(childResource);
        add(childResources, comparator);
    }

    /**
     * Add multiple resource representations to the existing list.
     *
     * @param childResources the resources to add
     * @param comparator     used to define the persisted order of the children resources; optional - pass null if unneeded
     * @throws PersistenceException
     * @throws InvalidDataFormatException
     */
    public void add(List<SyntheticChildAsPropertyResource> childResources, Comparator<Resource> comparator)
            throws PersistenceException, InvalidDataFormatException {

        final long start = System.currentTimeMillis();

        final ModifiableValueMap modifiableValueMap = this.resource.adaptTo(ModifiableValueMap.class);
        final String propertyData = modifiableValueMap.get(this.propertyName, EMPTY_JSON);

        try {
            JSONObject childrenJSON = new JSONObject(propertyData);

            // Add the new entries to the JSON
            for (final SyntheticChildAsPropertyResource childResource : childResources) {
                childrenJSON.put(childResource.getName(), this.serializeToJSON(childResource));
            }

            // Sort as needed
            if (comparator != null) {
                List<SyntheticChildAsPropertyResource> resources = deserializeToSyntheticChildResources(childrenJSON);
                Collections.sort(resources, comparator);
                childrenJSON = serializeToJSON(resources);
            }

            // Persist the JSON back to the Node
            modifiableValueMap.put(propertyName, childrenJSON.toString());

            log.debug("Add operation for [ {} ] in [ {} ms ]",
                    this.resource.getPath() + "/" + this.propertyName,
                    System.currentTimeMillis() - start);

        } catch (JSONException e) {
            throw new InvalidDataFormatException(this.resource, this.propertyName, propertyData);
        } catch (NoSuchMethodException e) {
            throw new InvalidDataFormatException(this.resource, this.propertyName, propertyData);
        } catch (IllegalAccessException e) {
            throw new InvalidDataFormatException(this.resource, this.propertyName, propertyData);
        } catch (InvocationTargetException e) {
            throw new InvalidDataFormatException(this.resource, this.propertyName, propertyData);
        }
    }

    /**
     * Replace existing data with this list.
     * <p/>
     * Similar to add(..) but replaces any existing data.
     *
     * @param childResource the resource to add
     * @param comparator    used to define the persisted order of the children resources; optional - pass null if unneeded
     * @throws PersistenceException
     * @throws InvalidDataFormatException
     */
    public void put(SyntheticChildAsPropertyResource childResource, Comparator<Resource> comparator)
            throws PersistenceException, InvalidDataFormatException {

        final List<SyntheticChildAsPropertyResource> childResources = new ArrayList<SyntheticChildAsPropertyResource>();
        childResources.add(childResource);
        put(childResources, comparator);
    }

    public void put(List<SyntheticChildAsPropertyResource> childResources, Comparator<Resource> comparator)
            throws PersistenceException, InvalidDataFormatException {

        final long start = System.currentTimeMillis();

        final ModifiableValueMap modifiableValueMap = this.resource.adaptTo(ModifiableValueMap.class);
        final String propertyData = modifiableValueMap.get(this.propertyName, EMPTY_JSON);

        try {
            JSONObject childrenJSON = new JSONObject();

            // Add the new entries to the JSON
            for (SyntheticChildAsPropertyResource childResource : childResources) {
                childrenJSON.put(childResource.getName(), this.serializeToJSON(childResource));
            }

            // Sort as needed
            if (comparator != null) {
                List<SyntheticChildAsPropertyResource> resources = deserializeToSyntheticChildResources(childrenJSON);
                Collections.sort(resources, comparator);
                childrenJSON = serializeToJSON(resources);
            }

            // Persist the JSON back to the Node
            modifiableValueMap.put(this.propertyName, childrenJSON.toString());

            log.debug("Put operation for [ {} ] in [ {} ms ]",
                    this.resource.getPath() + "/" + this.propertyName,
                    System.currentTimeMillis() - start);

        } catch (JSONException e) {
            throw new InvalidDataFormatException(this.resource, this.propertyName, propertyData);
        } catch (NoSuchMethodException e) {
            throw new InvalidDataFormatException(this.resource, this.propertyName, propertyData);
        } catch (IllegalAccessException e) {
            throw new InvalidDataFormatException(this.resource, this.propertyName, propertyData);
        } catch (InvocationTargetException e) {
            throw new InvalidDataFormatException(this.resource, this.propertyName, propertyData);
        }
    }

    /**
     * @return the list of children using the persisted order.
     * @throws InvalidDataFormatException
     */
    public List<Resource> get() throws InvalidDataFormatException {
        return get(null);
    }

    /**
     * @param comparator used to define the persisted order of the children resources; optional - pass null if unneeded
     * @return the list of children sorting using the comparator.
     * @throws InvalidDataFormatException
     */
    public List<Resource> get(Comparator<Resource> comparator) throws InvalidDataFormatException {
        final long start = System.currentTimeMillis();

        final String propertyData = this.resource.getValueMap().get(this.propertyName, EMPTY_JSON);

        List<SyntheticChildAsPropertyResource> resources;

        try {
            resources = deserializeToSyntheticChildResources(new JSONObject(propertyData));
        } catch (JSONException e) {
            throw new InvalidDataFormatException(this.resource, this.propertyName, propertyData);
        }

        if (comparator != null) {
            Collections.sort(resources, comparator);
        }

        log.debug("Get operation for [ {} ] in [ {} ms ]",
                this.resource.getPath() + "/" + this.propertyName,
                System.currentTimeMillis() - start);

        return (List<Resource>) (List<?>) resources;
    }

    /**
     * Removes a resource from the list.
     *
     * @param resourceName the resource to remove.
     * @return true if a resource could be found and it was removed, false otherwise.
     * @throws PersistenceException
     * @throws InvalidDataFormatException
     */
    public boolean remove(String resourceName) throws PersistenceException, InvalidDataFormatException {
        final List<String> resourceNames = new ArrayList<String>();
        resourceNames.add(resourceName);

        return remove(resourceNames) > 0;
    }

    /**
     * @param resourceNames
     * @return
     * @throws PersistenceException
     * @throws InvalidDataFormatException
     */
    public int remove(List<String> resourceNames) throws PersistenceException, InvalidDataFormatException {
        final long start = System.currentTimeMillis();

        int removalCount = 0;
        final ModifiableValueMap modifiableValueMap = this.resource.adaptTo(ModifiableValueMap.class);
        final String propertyData = modifiableValueMap.get(this.propertyName, EMPTY_JSON);

        try {
            final JSONObject childrenJSON = new JSONObject(propertyData);

            for (String resourceName : resourceNames) {
                if (childrenJSON.opt(resourceName) != null) {
                    childrenJSON.remove(resourceName);
                    removalCount++;
                }
            }

            if (removalCount > 0) {
                // Persist the JSON back to the Node
                modifiableValueMap.put(propertyName, childrenJSON.toString());
            }

            log.debug("Remove operation for [ {} ] in [ {} ms ]",
                    this.resource.getPath() + "/" + this.propertyName,
                    System.currentTimeMillis() - start);

        } catch (JSONException e) {
            throw new InvalidDataFormatException(this.resource, this.propertyName, propertyData);
        }

        return removalCount;
    }

    /**
     * Creates a SyntheticChildAsPropertyResource.
     *
     * @param resourceName the resource's name
     * @param valueMap     the resource's valueMap.
     * @return a new SyntheticChildAsPropertyResource object
     */
    public SyntheticChildAsPropertyResource build(String resourceName, Map<String, Object> valueMap) {
        return new SyntheticChildAsPropertyResource(this.resource, resourceName, new ValueMapDecorator(valueMap));
    }

    /**
     * Converts a list of SyntheticChildAsPropertyResource to their JSON representation, keeping the provided order.
     *
     * @param resource the resources to serialize to JSON.
     * @return the JSONObject representing the resources.
     * @throws JSONException
     */
    private static JSONObject serializeToJSON(SyntheticChildAsPropertyResource resource) throws JSONException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
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

    private static JSONObject serializeToJSON(List<SyntheticChildAsPropertyResource> resources) throws JSONException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        final JSONObject jsonObject = new JSONObject();

        for (final SyntheticChildAsPropertyResource resource : resources) {
            jsonObject.put(resource.getName(), serializeToJSON(resource));
        }

        return jsonObject;
    }

    /**
     * Converts a JSONObject to the list of SyntheticChildAsPropertyResources.
     *
     * @param jsonObject the JSONObject to deserialize.
     * @return the list of SyntheticChildAsPropertyResources the jsonObject represents.
     * @throws JSONException
     */
    private List<SyntheticChildAsPropertyResource> deserializeToSyntheticChildResources(JSONObject jsonObject) throws JSONException {
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

                    if(calendar) {
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

            resources.add(this.build(nodeName, properties));
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
