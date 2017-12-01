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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * ACS AEM Commons - Property Merge Sling POST Processor
 */
@Component
@Service
public class PropertyMergePostProcessor implements SlingPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(PropertyMergePostProcessor.class);

    private static final String AT_SUFFIX = "@PropertyMerge";
    private static final String ALLOW_DUPLICATES_SUFFIX = AT_SUFFIX + ".AllowDuplicates";
    private static final String TYPE_HINT_SUFFIX = AT_SUFFIX + ".TypeHint";
    private static final String IGNORE_PREFIX = ":";

    @Override
    public final void process(final SlingHttpServletRequest request,
            final List<Modification> modifications) throws Exception {

        final List<PropertyMerge> propertyMerges = this.getPropertyMerges(request.getRequestParameterMap());

        final Resource resource = request.getResource();

        for (final PropertyMerge propertyMerge : propertyMerges) {
            this.merge(resource,
                    propertyMerge.getDestination(),
                    propertyMerge.getSources(),
                    propertyMerge.getTypeHint(),
                    propertyMerge.isAllowDuplicates())
                    .ifPresent(modifiedResource -> {
                        modifications.add(Modification.onModified(modifiedResource.getPath()));
                        log.debug("Merged property values from {} into [ {} ]",
                                propertyMerge.getSources(),
                                propertyMerge.getDestination());
                    });
        }
    }

    /**
     * Gets the corresponding list of PropertyMerge directives from the
     * RequestParams.
     *
     * @param requestParameterMap the Request Param Map
     * @return a list of the PropertyMerge directives by Destination
     */
    @SuppressWarnings("squid:S3776")
    private List<PropertyMerge> getPropertyMerges(final RequestParameterMap requestParameterMap) {
        final HashMap<String, List<String>> mapping = new HashMap<>();
        boolean isBulkUpdate = Boolean.valueOf(requestParameterMap.getValue("dam:bulkUpdate").getString());

        // Collect the Destination / Source mappings
        requestParameterMap.forEach((key, values) -> {
            if (!StringUtils.endsWith(key, AT_SUFFIX)) {
                // Not a @PropertyMerge request param
                return;
            }

            final String source = StringUtils.removeStart(StringUtils.substringBefore(key, AT_SUFFIX), IGNORE_PREFIX);

            Stream.of(values)
                    .filter(Objects::nonNull)
                    .map(RequestParameter::getString)
                    .map(s -> StringUtils.removeStart(StringUtils.stripToNull(s), IGNORE_PREFIX))
                    .filter(Objects::nonNull)
                    .forEach(destination -> {
                        // if this is a DAM bulk update, search all request params ending with this value
                        if (isBulkUpdate) {
                            requestParameterMap.keySet().stream()
                                    .map(String::valueOf)
                                    .filter((paramName) -> (paramName.endsWith("/" + source)))
                                    .forEach(param -> {
                                        String newDestination = param.substring(0, param.indexOf('/', 2)) + "/" + destination;
                                        List<String> sources = mapping.getOrDefault(newDestination, new ArrayList<>());
                                        sources.add(param);
                                        mapping.put(newDestination, sources);
                                    });
                        } else {
                            List<String> sources = mapping.getOrDefault(destination, new ArrayList<>());
                            sources.add(source);
                            mapping.put(destination, sources);
                        }
                    });
        });

        // Convert the Mappings into PropertyMerge objects
        final List<PropertyMerge> propertyMerges = new ArrayList<>();

        mapping.forEach((destination, sources) -> {
            RequestParameter allowDuplicatesParam = requestParameterMap.getValue(IGNORE_PREFIX + destination
                    + ALLOW_DUPLICATES_SUFFIX);

            final boolean allowDuplicates = allowDuplicatesParam != null && Boolean.valueOf(allowDuplicatesParam.getString());

            RequestParameter typeHintParam = requestParameterMap.getValue(IGNORE_PREFIX + destination
                    + TYPE_HINT_SUFFIX);

            final String typeHint
                    = typeHintParam != null ? typeHintParam.getString() : String.class.getSimpleName();

            propertyMerges.add(new PropertyMerge(destination, sources, allowDuplicates, typeHint));
        });

        return propertyMerges;
    }

    /**
     * Merges the values found in the the source properties into the destination
     * property as a multi-value. The values of the source properties and
     * destination properties must all be the same property type.
     *
     * The unique set of properties will be stored in
     *
     * @param resource the resource to look for the source and destination
     * properties on
     * @param destination the property to store the collected properties.
     * @param sources the properties to collect values from for merging
     * @param typeHint the data type that should be used when reading and
     * storing the data
     * @param allowDuplicates true to allow duplicates values in the destination
     * property; false to make values unique
     * @return Optional resource updated, if any
     */
    protected final <T> Optional<Resource> merge(final Resource resource, final String destination,
            final List<String> sources, final Class<T> typeHint,
            final boolean allowDuplicates) throws PersistenceException {

        // Create an empty array of type T
        @SuppressWarnings("unchecked")
        final T[] emptyArray = (T[]) Array.newInstance(typeHint, 0);

        final String targetProperty;

        Collection<T> collectedValues = null;

        if (allowDuplicates) {
            collectedValues = new ArrayList<>();
        } else {
            collectedValues = new LinkedHashSet<>();
        }

        for (final String source : sources) {
            final T[] tmp;
            Resource sourceProperties = resource;
            // Get the source value as type T
            String sourceParam = source;
            if (source.contains("/")) {
                sourceProperties = resource.getResourceResolver().getResource(resource, source.substring(0, source.lastIndexOf('/')));
                sourceParam = source.substring(source.lastIndexOf('/') + 1);
            }

            tmp = sourceProperties.adaptTo(ModifiableValueMap.class).get(sourceParam, emptyArray);

            // If the value is not null, add to collectedValues
            if (tmp != null) {
                collectedValues.addAll(Arrays.asList(tmp));
            }
        }

        Resource targetResource;
        if (destination.contains("/")) {
            targetProperty = destination.substring(destination.lastIndexOf('/') + 1);
            targetResource = resource.getResourceResolver().getResource(resource, destination.substring(0, destination.lastIndexOf('/')));
        } else {
            targetProperty = destination;
            targetResource = resource;
        }
        ModifiableValueMap targetProperties = targetResource.adaptTo(ModifiableValueMap.class);

        final T[] currentValues = targetProperties.get(targetProperty, emptyArray);

        if (!collectedValues.equals(Arrays.asList(currentValues))) {
            targetProperties.put(targetProperty, collectedValues.toArray(emptyArray));

            return Optional.of(targetResource);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Encapsulates a PropertyMerge configuration by Destination.
     */
    private static class PropertyMerge {

        private boolean allowDuplicates;
        private Class<?> typeHint;
        private String destination;
        private List<String> sources;

        public PropertyMerge(String destination, List<String> sources, boolean allowDuplicates, String typeHint) {
            this.destination = destination;
            this.sources = sources;
            this.allowDuplicates = allowDuplicates;
            this.typeHint = this.convertTypeHint(typeHint);
        }

        /**
         * Converts the String type hint to the corresponding class. If not
         * valid conversion can be found, default to String.
         *
         * @param typeHintStr the String representation of the type hint
         * @return the Class of the type hint
         */
        private Class<?> convertTypeHint(final String typeHintStr) {
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

        public Class<?> getTypeHint() {
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
