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
import java.util.Collection;
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

    @Override
    public final void process(final SlingHttpServletRequest request,
                              final List<Modification> modifications) throws Exception {

        final Map<String, List<String>> mappings = this.getDestinationToSourceMapping(request.getRequestParameterMap());

        final Resource resource = request.getResource();

        for (final Map.Entry<String, List<String>> entry : mappings.entrySet()) {
            final String destination = entry.getKey();
            final List<String> sources = entry.getValue();

            if (this.merge(resource, destination, sources, String.class, false)) {
                modifications.add(Modification.onModified(resource.getPath()));
                log.debug("Merged property values from {} into [ {} ]", sources, destination);
            }
        }
    }

    private Map<String, List<String>> getDestinationToSourceMapping(final RequestParameterMap requestParameterMap) {
        final HashMap<String, List<String>> mapping = new HashMap<String, List<String>>();

        // primaryTags@PropertyMerge=cq:tags
        // secondaryTags@PropertyMerge=cq:tags
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

        return mapping;
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
    protected final <T> boolean merge(final Resource resource, final String destination,
                                      final List<String> sources, final Class<T> klass,
                                      final boolean allowDuplicates) throws PersistenceException {

        // Create an empty array of type T
        @SuppressWarnings("unchecked")
        final T[] emptyArray = (T[]) Array.newInstance(klass, 0);

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
}
