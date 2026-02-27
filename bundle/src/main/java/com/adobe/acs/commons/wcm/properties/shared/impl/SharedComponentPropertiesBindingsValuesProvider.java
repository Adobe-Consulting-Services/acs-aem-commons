package com.adobe.acs.commons.wcm.properties.shared.impl;

import com.adobe.acs.commons.wcm.properties.shared.SharedComponentProperties;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;

/**
 * Bindings Values Provider that adds bindings for globalProperties,
 * sharedProperties, and mergedProperties maps.
 *
 * Default: enabled=true (for minor release backward compatibility).
 */
@Component(service = BindingsValuesProvider.class)
@Designate(ocd = SharedComponentPropertiesBindingsValuesProvider.Config.class)
public class SharedComponentPropertiesBindingsValuesProvider
        implements BindingsValuesProvider {

    private static final Logger log =
            LoggerFactory.getLogger(SharedComponentPropertiesBindingsValuesProvider.class);

    @ObjectClassDefinition(
            name = "ACS Commons - Shared Component Properties Bindings Values Provider",
            description = "Controls whether shared/global/merged property bindings are applied"
    )
    public @interface Config {

        @AttributeDefinition(
                name = "Enabled",
                description = "Enable shared/global/merged component property bindings for Sling scripts"
        )
        boolean enabled() default true;
    }

    @Reference(
            policyOption = ReferencePolicyOption.GREEDY,
            cardinality = ReferenceCardinality.OPTIONAL
    )
    private SharedComponentProperties sharedComponentProperties;

    private volatile boolean enabled;

    @Activate
    protected void activate(Config config) {
        this.enabled = config.enabled();
    }

    @Override
    public void addBindings(final Bindings bindings) {

        final Resource resource =
                (Resource) bindings.get(SlingBindings.RESOURCE);

        if (enabled) {
            final SlingHttpServletRequest request =
                    (SlingHttpServletRequest) bindings.get(SlingBindings.REQUEST);

            if (request != null && resource != null) {
                final SharedPropertiesRequestCache cache =
                        SharedPropertiesRequestCache.fromRequest(request);

                if (sharedComponentProperties != null) {
                    setSharedProperties(bindings, resource, cache);
                } else {
                    log.debug("Shared Component Properties must be configured to enable this provider");
                }
            }
        }

        setDefaultBindings(bindings, resource);
    }

    private void setSharedProperties(final Bindings bindings,
                                     final Resource resource,
                                     final SharedPropertiesRequestCache cache) {

        String rootPagePath =
                sharedComponentProperties.getSharedPropertiesPagePath(resource);

        if (rootPagePath != null) {

            bindings.put(
                    SharedComponentProperties.SHARED_PROPERTIES_PAGE_PATH,
                    rootPagePath
            );

            String globalPropsPath =
                    sharedComponentProperties.getGlobalPropertiesPath(resource);

            if (globalPropsPath != null) {
                bindings.putAll(cache.getBindings(globalPropsPath, newBindings -> {
                    final Resource globalPropsResource =
                            resource.getResourceResolver().getResource(globalPropsPath);

                    if (globalPropsResource != null) {
                        newBindings.put(
                                SharedComponentProperties.GLOBAL_PROPERTIES,
                                globalPropsResource.getValueMap()
                        );
                        newBindings.put(
                                SharedComponentProperties.GLOBAL_PROPERTIES_RESOURCE,
                                globalPropsResource
                        );
                    }
                }));
            }

            final String sharedPropsPath =
                    sharedComponentProperties.getSharedPropertiesPath(resource);

            if (sharedPropsPath != null) {
                bindings.putAll(cache.getBindings(sharedPropsPath, newBindings -> {
                    Resource sharedPropsResource =
                            resource.getResourceResolver().getResource(sharedPropsPath);

                    if (sharedPropsResource != null) {
                        newBindings.put(
                                SharedComponentProperties.SHARED_PROPERTIES,
                                sharedPropsResource.getValueMap()
                        );
                        newBindings.put(
                                SharedComponentProperties.SHARED_PROPERTIES_RESOURCE,
                                sharedPropsResource
                        );
                    }
                }));

                bindings.put(
                        SharedComponentProperties.SHARED_PROPERTIES_PATH,
                        sharedPropsPath
                );
            }

            final String mergedPropertiesPath = resource.getPath();

            bindings.putAll(cache.getBindings(mergedPropertiesPath, newBindings -> {
                ValueMap globalPropertyMap =
                        (ValueMap) bindings.get(SharedComponentProperties.GLOBAL_PROPERTIES);

                ValueMap sharedPropertyMap =
                        (ValueMap) bindings.get(SharedComponentProperties.SHARED_PROPERTIES);

                newBindings.put(
                        SharedComponentProperties.MERGED_PROPERTIES,
                        sharedComponentProperties.mergeProperties(
                                globalPropertyMap,
                                sharedPropertyMap,
                                resource
                        )
                );
            }));

            bindings.put(
                    SharedComponentProperties.MERGED_PROPERTIES_PATH,
                    resource.getPath()
            );
        }
    }

    private void setDefaultBindings(final Bindings bindings,
                                    final Resource resource) {

        if (!bindings.containsKey(SharedComponentProperties.GLOBAL_PROPERTIES)) {
            bindings.put(
                    SharedComponentProperties.GLOBAL_PROPERTIES,
                    ValueMap.EMPTY
            );
        }

        if (!bindings.containsKey(SharedComponentProperties.SHARED_PROPERTIES)) {
            bindings.put(
                    SharedComponentProperties.SHARED_PROPERTIES,
                    ValueMap.EMPTY
            );
        }

        if (!bindings.containsKey(SharedComponentProperties.MERGED_PROPERTIES)) {
            bindings.put(
                    SharedComponentProperties.MERGED_PROPERTIES,
                    resource == null
                            ? ValueMap.EMPTY
                            : resource.getValueMap()
            );
        }
    }
}
