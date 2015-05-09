package com.adobe.acs.commons.wcm.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Component(
        label = "ACS AEM Commons - Property Merge Sling POST Processor"
)
@Service
public class PropertyMergePostProcessor implements SlingPostProcessor {
    private static final Logger log = LoggerFactory.getLogger(PropertyMergePostProcessor.class);

    private static final String AT_SUFFIX = "@PropertyMerge";
    private static final String ALLOW_DUPLICATES_SUFFIX = AT_SUFFIX + ".AllowDuplicates";
    private static final String TYPE_HINT_SUFFIX = AT_SUFFIX + ".TypeHint";

    @Override
    public final void process(final SlingHttpServletRequest request,
                              final List<Modification> modifications) throws Exception {

        final List<PropertyMerge> propertyMerges = this.getPropertyMerges(request.getRequestParameterMap());

        final Resource resource = request.getResource();

        for (final PropertyMerge propertyMerge : propertyMerges) {

            if (this.merge(resource,
                    propertyMerge.getDestination(),
                    propertyMerge.getSources(),
                    propertyMerge.getTypeHint(),
                    propertyMerge.isAllowDuplicates())) {
                modifications.add(Modification.onModified(resource.getPath()));
                log.debug("Merged property values from {} into [ {} ]",
                        propertyMerge.getSources(),
                        propertyMerge.getDestination());
            }
        }
    }

    /**
     * Gets the corresponding list of PropertyMerge directives from the RequestParams.
     *
     * @param requestParameterMap the Request Param Map
     * @return a list of the PropertyMerge directives by Destination
     */
    private List<PropertyMerge> getPropertyMerges(final RequestParameterMap requestParameterMap) {
        final HashMap<String, List<String>> mapping = new HashMap<String, List<String>>();

        // Collect the Destination / Source mappings

        for (final RequestParameterMap.Entry<String, RequestParameter[]> entry : requestParameterMap.entrySet()) {
            if (!StringUtils.endsWith(entry.getKey(), AT_SUFFIX)) {
                // Not a @PropertyMerge request param
                continue;
            }

            final String source = StringUtils.substringBefore(entry.getKey(), AT_SUFFIX);

            for (final RequestParameter requestParameter : entry.getValue()) {
                if (requestParameter != null) {
                    final String destination = StringUtils.stripToNull(requestParameter.getString());

                    if (destination != null) {
                        List<String> sources = mapping.get(destination);

                        if (sources == null) {
                            sources = new ArrayList<String>();
                        }

                        sources.add(source);
                        mapping.put(StringUtils.strip(requestParameter.getString()), sources);
                    }
                }
            }
        }

        // Convert the Mappings into PropertyMerge objects

        final List<PropertyMerge> propertyMerges = new ArrayList<PropertyMerge>();

        for (final Map.Entry<String, List<String>> entry : mapping.entrySet()) {
            final String destination = entry.getKey();
            final List<String> sources = entry.getValue();

            RequestParameter allowDuplicatesParam = requestParameterMap.getValue(destination
                    + ALLOW_DUPLICATES_SUFFIX);

            final boolean allowDuplicates =
                    allowDuplicatesParam != null ? Boolean.valueOf(allowDuplicatesParam.getString()) : false;

            RequestParameter typeHintParam = requestParameterMap.getValue(destination
                    + TYPE_HINT_SUFFIX);

            final String typeHint =
                    typeHintParam != null ? typeHintParam.getString() : String.class.getSimpleName();

            propertyMerges.add(new PropertyMerge(destination, sources, allowDuplicates, typeHint));
        }

        return propertyMerges;
    }


    /**
     * Merges the values found in the the source properties into the destination property as a multi-value.
     * The values of the source properties and destination properties must all be the same property type.
     *
     * The unique set of properties will be stored in
     *
     * @param resource    the resource to look for the source and destination properties on
     * @param destination the property to store the collected properties.
     * @param sources  the properties to collect values from for merging
     * @param typeHint the data type that should be used when reading and storing the data
     * @param allowDuplicates true to allow duplicates values in the destination property; false to make values unique
     * @return true if changes were made to the destination property
     */
    protected final <T> boolean merge(final Resource resource, final String destination,
                                      final List<String> sources, final Class<T> typeHint,
                                      final boolean allowDuplicates) throws PersistenceException {

        // Create an empty array of type T
        @SuppressWarnings("unchecked")
        final T[] emptyArray = (T[]) Array.newInstance(typeHint, 0);

        final ModifiableValueMap properties = resource.adaptTo(ModifiableValueMap.class);

        Collection<T> collectedValues = null;

        if (allowDuplicates) {
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

            return true;
        } else {
            return false;
        }
    }


    /**
     * Encapsulates a PropertyMerge configuration by Destination.
     */
    private class PropertyMerge {
        private boolean allowDuplicates;
        private Class typeHint;
        private String destination;
        private List<String> sources;

        public PropertyMerge(String destination, List<String> sources, boolean allowDuplicates, String typeHint) {
            this.destination = destination;
            this.sources = sources;
            this.allowDuplicates = allowDuplicates;
            this.typeHint = this.convertTypeHint(typeHint);
        }

        /**
         * Converts the String type hint to the corresponding class.
         * If not valid conversion can be found, default to String.
         *
         * @param typeHintStr the String representation of the type hint
         * @return the Class of the type hint
         */
        private Class convertTypeHint(final String typeHintStr) {
            if (Boolean.class.getSimpleName().equalsIgnoreCase(typeHintStr)) {
                return Boolean.class;
            } else if (Double.class.getSimpleName().equalsIgnoreCase(typeHintStr)) {
                return Double.class;
            } else if (Long.class.getSimpleName().equalsIgnoreCase(typeHintStr)) {
                return Long.class;
            } else if (Date.class.getSimpleName().equalsIgnoreCase(typeHintStr)
                    || Calendar.class.getSimpleName().equalsIgnoreCase(typeHintStr)) {
                return Calendar.class;
            } else {
                return String.class;
            }
        }

        public boolean isAllowDuplicates() {
            return allowDuplicates;
        }

        public Class getTypeHint() {
            return typeHint;
        }

        public String getDestination() {
            return destination;
        }

        public List<String> getSources() {
            return sources;
        }
    }
}
