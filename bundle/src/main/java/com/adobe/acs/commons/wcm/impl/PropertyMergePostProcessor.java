/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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

import com.day.cq.tagging.TagManager;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ResourceResolver;

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
    protected static final String OPERATION_ALL_TAGS = "Operation.mergeAllTags";
    private static final String VALID_JCR_NAME = "[^:/\\[\\]\\|\\s*]+";
    private static final Pattern VALID_TAG = Pattern.compile("^" + VALID_JCR_NAME + ":(" + VALID_JCR_NAME + "/)*(" + VALID_JCR_NAME + ")?$");

    @Override
    public final void process(final SlingHttpServletRequest request,
            final List<Modification> modifications) throws Exception {

        final List<PropertyMerge> propertyMerges = this.getPropertyMerges(request);

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
     * @param requestParameters the Request Param Map
     * @return a list of the PropertyMerge directives by Destination
     */
    @SuppressWarnings("squid:S3776")
    private List<PropertyMerge> getPropertyMerges(final SlingHttpServletRequest request) {
        final RequestParameterMap requestParameters = request.getRequestParameterMap();
        final HashMap<String, Set<String>> mapping = new HashMap<>();
        boolean isBulkUpdate = Boolean.valueOf(getParamValue(requestParameters, "dam:bulkUpdate"));

        // Collect the Destination / Source mappings
        requestParameters.forEach((key, values) -> {
            if (!StringUtils.endsWith(key, AT_SUFFIX)) {
                // Not a @PropertyMerge request param
                return;
            }

            Function<String, String> stripPrefix = (s -> StringUtils.removeStart(StringUtils.stripToNull(s), IGNORE_PREFIX));
            final String source = stripPrefix.apply(StringUtils.substringBefore(key, AT_SUFFIX));

            Stream.of(values)
                    .map(RequestParameter::getString)
                    .map(stripPrefix)
                    .filter(Objects::nonNull)
                    .forEach(destination -> {
                        if (source.equalsIgnoreCase(OPERATION_ALL_TAGS)) {
                            // if this is a request for merging all tags, look at everyting that might be a tag
                            trackAllTagsMergeParameters(request, destination, mapping);
                        } else if (isBulkUpdate) {
                            // if this is a DAM bulk update, search all request params ending with this value
                            trackAssetMergeParameters(requestParameters, source, destination, mapping);
                        } else {
                            trackMergeParameters(mapping, source, destination);
                        }
                    });
        });

        // Convert the Mappings into PropertyMerge objects
        return mapping.entrySet().stream().map(
                entry -> new PropertyMerge(
                        entry.getKey(),
                        entry.getValue(),
                        areDuplicatesAllowed(requestParameters, entry.getKey()),
                        getFieldTypeHint(requestParameters, entry.getKey())
                ))
                .collect(Collectors.toList());
    }

    private void trackMergeParameters(final HashMap<String, Set<String>> mapping, final String source, String destination) {
        mapping.merge(destination, new HashSet<>(), (a, b) -> a).add(source);
    }

    private void trackAssetMergeParameters(final RequestParameterMap requestParameters, final String source, String destination, final HashMap<String, Set<String>> mapping) {
        requestParameters.keySet().stream()
                .map(String::valueOf)
                .filter((paramName) -> (paramName.endsWith("/" + source)))
                .forEach(adjustedSource -> {
                    String adjustedDest = alignDestinationPath(adjustedSource, destination);
                    trackMergeParameters(mapping, adjustedSource, adjustedDest);
                });
    }

    private void trackAllTagsMergeParameters(SlingHttpServletRequest request, String destination, HashMap<String, Set<String>> mapping) {
        request.getRequestParameterMap().forEach((source, value) -> {
            if (hasTags(request.getResourceResolver(), value)) {
                String newDestination = alignDestinationPath(source, destination);
                trackMergeParameters(mapping, source, newDestination);
            }
        });
    }

    protected static boolean hasTags(ResourceResolver rr, RequestParameter[] params) {
        if (params == null) {
            return false;
        } else {
            TagManager tagManager = rr.adaptTo(TagManager.class);
            return Stream.of(params).allMatch(param
                    -> looksLikeTag(param.getString())
                    && tagManager.resolve(param.getString()) != null
            );
        }
    }

    protected static boolean looksLikeTag(String value) {
        return VALID_TAG.asPredicate().test(value);
    }

    protected static boolean areDuplicatesAllowed(RequestParameterMap params, String field) {
        return Boolean.valueOf(
                getParamValue(params, IGNORE_PREFIX + field + ALLOW_DUPLICATES_SUFFIX)
        );
    }

    protected static String getFieldTypeHint(RequestParameterMap params, String field) {
        return StringUtils.defaultString(
                getParamValue(params, IGNORE_PREFIX + field + TYPE_HINT_SUFFIX),
                String.class.getSimpleName()
        );
    }

    protected static String alignDestinationPath(String source, String destination) {
        if (source.contains(JcrConstants.JCR_CONTENT)) {
            return StringUtils.substringBeforeLast(source, JcrConstants.JCR_CONTENT) + destination;
        } else {
            return destination;
        }
    }

    protected static String getParamValue(RequestParameterMap params, String paramName) {
        RequestParameter param = params.getValue(paramName);
        return param == null ? null : param.getString();
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
            final Collection<String> sources, final Class<T> typeHint,
            final boolean allowDuplicates) throws PersistenceException {

        ResourceResolver rr = resource.getResourceResolver();

        // Create an empty array of type T
        @SuppressWarnings("unchecked")
        final T[] emptyArray = (T[]) Array.newInstance(typeHint, 0);

        Collection<T> collectedValues;

        if (allowDuplicates) {
            collectedValues = new ArrayList<>();
        } else {
            collectedValues = new LinkedHashSet<>();
        }

        for (final String source : sources) {
            Resource sourceProperties = resource;
            String sourceParam = source;
            if (source.contains("/")) {
                sourceParam = StringUtils.substringAfterLast(source, "/");
                sourceProperties = rr.getResource(resource, StringUtils.substringBeforeLast(source, "/"));
            }
            T[] tmp = sourceProperties.adaptTo(ModifiableValueMap.class).get(sourceParam, emptyArray);
            collectedValues.addAll(Arrays.asList(tmp));
        }

        Resource targetResource = resource;
        String targetProperty = destination;
        if (destination.contains("/")) {
            targetProperty = StringUtils.substringAfterLast(destination, "/");
            targetResource = rr.getResource(resource, StringUtils.substringBeforeLast(destination, "/"));
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
        private Collection<String> sources;

        public PropertyMerge(String destination, Collection<String> sources, boolean allowDuplicates, String typeHint) {
            this.destination = destination;
            this.sources = Optional.ofNullable(sources)
                    .map(coll -> (Set<String>) new HashSet<>(coll))
                    .orElse(Collections.emptySet());
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

        public Collection<String> getSources() {
            return Collections.unmodifiableCollection(sources);
        }
    }
}
