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
package com.adobe.acs.commons.wcm.properties.shared.impl;

import com.adobe.acs.commons.wcm.PageRootProvider;
import com.adobe.acs.commons.wcm.properties.shared.SharedComponentProperties;
import com.adobe.acs.commons.wcm.properties.shared.SharedValueMapResourceAdapter;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicyOption;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component(policy = ConfigurationPolicy.REQUIRE)
@Service(value = {SharedComponentProperties.class, AdapterFactory.class})
@Properties(value = {
        @Property(name = SlingConstants.PROPERTY_ADAPTABLE_CLASSES, classValue = Resource.class),
        @Property(name = SlingConstants.PROPERTY_ADAPTER_CLASSES, classValue = SharedValueMapResourceAdapter.class),
})
public class SharedComponentPropertiesImpl implements SharedComponentProperties, AdapterFactory {
    private static final Logger LOG = LoggerFactory.getLogger(SharedComponentPropertiesImpl.class);
    private static final String INFIX_JCR_CONTENT = "/jcr:content/";
    /**
     * Bind if available, check for null when reading.
     */
    @Reference(policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    PageRootProvider pageRootProvider;

    /**
     * Construct a canonical resource type relative path for the provided resource type,
     * or null if the result is not acceptable.
     * Step 1: discard empty and JCR node types / sling:nonexisting (contains ":")
     * Step 2: return result if already relative (does not start with /)
     * Step 3: relativize an absolute path using elements of {@code searchPaths} and return the first match found.
     *
     * @param resourceType the request resource resourceType
     * @param searchPaths  {@link org.apache.sling.api.resource.ResourceResolver#getSearchPath()}
     * @return the canonical resource type or null
     */
    static String getCanonicalResourceTypeRelativePath(final String resourceType, final String[] searchPaths) {
        if (StringUtils.isEmpty(resourceType) || resourceType.contains(":")) {
            return null;
        }

        if (resourceType.charAt(0) != '/') {
            return resourceType;
        } else if (searchPaths != null) {
            for (final String searchPath : searchPaths) {
                if (resourceType.startsWith(searchPath)) {
                    return resourceType.substring(searchPath.length());
                }
            }
        }
        return null;
    }

    @Override
    public String getSharedPropertiesPagePath(final Resource resource) {
        if (pageRootProvider != null) {
            if (resource != null) {
                final String pagePath = pageRootProvider.getRootPagePath(resource.getPath());
                if (StringUtils.isNotBlank(pagePath)) {
                    return pagePath;
                } else {
                    LOG.debug("Could not determine shared properties page for resource {}", resource.getPath());
                }
            }
        } else {
            LOG.debug("Page Root Provider must be configured for shared component properties to be supported");
        }
        return null;
    }

    @Override
    public String getGlobalPropertiesPath(final Resource resource) {
        final String rootPagePath = getSharedPropertiesPagePath(resource);
        if (StringUtils.isNotBlank(rootPagePath)) {
            return rootPagePath + INFIX_JCR_CONTENT + NN_GLOBAL_COMPONENT_PROPERTIES;
        }

        return null;
    }

    @Override
    public ValueMap getGlobalProperties(final Resource resource) {
        if (resource == null) {
            return ValueMap.EMPTY;
        }
        return Optional.ofNullable(getGlobalPropertiesPath(resource))
                .map(resource.getResourceResolver()::getResource)
                .map(Resource::getValueMap)
                .orElse(ValueMap.EMPTY);
    }

    @Override
    public String getSharedPropertiesPath(final Resource resource) {
        final String rootPagePath = getSharedPropertiesPagePath(resource);
        if (StringUtils.isBlank(rootPagePath)) {
            return null;
        }

        final String resourceTypeRelativePath = getCanonicalResourceTypeRelativePath(resource.getResourceType(),
                resource.getResourceResolver().getSearchPath());
        if (resourceTypeRelativePath != null) {
            return rootPagePath + INFIX_JCR_CONTENT + NN_SHARED_COMPONENT_PROPERTIES + "/" + resourceTypeRelativePath;
        }
        return null;
    }

    @Override
    public ValueMap getSharedProperties(final Resource resource) {
        if (resource == null) {
            return ValueMap.EMPTY;
        }
        return Optional.ofNullable(getSharedPropertiesPath(resource))
                .map(resource.getResourceResolver()::getResource)
                .map(Resource::getValueMap)
                .orElse(ValueMap.EMPTY);
    }

    @Override
    public ValueMap mergeProperties(final ValueMap globalProperties,
                                    final ValueMap sharedProperties,
                                    final Resource resource) {
        final Map<String, Object> mergedProperties = new HashMap<>();
        if (globalProperties != null) {
            mergedProperties.putAll(globalProperties);
        }
        if (sharedProperties != null) {
            mergedProperties.putAll(sharedProperties);
        }
        if (resource != null) {
            mergedProperties.putAll(resource.getValueMap());
        }
        return new ValueMapDecorator(mergedProperties);
    }

    @SuppressWarnings("unchecked")
    @CheckForNull
    @Override
    public <AdapterType> AdapterType getAdapter(@Nonnull final Object adaptable,
                                                @Nonnull final Class<AdapterType> adapterType) {
        if (adaptable instanceof Resource && adapterType == SharedValueMapResourceAdapter.class) {
            return (AdapterType) getSharedValueMapResourceAdapter((Resource) adaptable);
        }
        return null;
    }

    @Nonnull
    SharedValueMapResourceAdapter getSharedValueMapResourceAdapter(@Nonnull final Resource adaptable) {
        final ValueMap globalProperties = getGlobalProperties(adaptable);
        final ValueMap sharedProperties = getSharedProperties(adaptable);
        final ValueMap mergedProperties = mergeProperties(globalProperties, sharedProperties, adaptable);
        return new SharedValueMapResourceAdapterImpl(globalProperties, sharedProperties, mergedProperties);
    }
}