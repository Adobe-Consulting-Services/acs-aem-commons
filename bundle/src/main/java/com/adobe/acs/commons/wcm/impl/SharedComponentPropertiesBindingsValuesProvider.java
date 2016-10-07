package com.adobe.acs.commons.wcm.impl;

import com.adobe.acs.commons.wcm.PageRootProvider;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.components.Component;
import com.day.cq.wcm.commons.WCMUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import java.util.HashMap;
import java.util.Map;

/**
 * Bindings Values Provider that adds bindings for globalProperties,
 * sharedProperties, and mergedProperties maps.
 *
 * globalProperties contains the shared properties accessible by
 * all components.
 *
 * sharedProperties contains the shared properties specific to the
 * current component.
 *
 * mergedProperties is a merge of the instance-level, shared, and
 * global properties for the current component, giving preference
 * to instance-level values, then shared values, and finally global
 * values when properties exist at multiple levels with the same name.
 */
@org.apache.felix.scr.annotations.Component(
    label = "ACS AEM Commons - Shared Component Properties Provider",
    description = "Adds bindings for 'globalProperties', 'sharedProperties', and 'mergedProperties'."
)
@Service
public class SharedComponentPropertiesBindingsValuesProvider implements BindingsValuesProvider {

    private static final Logger log = LoggerFactory.getLogger(SharedComponentPropertiesBindingsValuesProvider.class);

    @Reference
    private PageRootProvider pageRootProvider;

    @Override
    public void addBindings(Bindings bindings) {
        Resource resource = (Resource) bindings.get("resource");
        Component component = WCMUtils.getComponent(resource);
        if (component != null) {
            if (pageRootProvider != null) {
                setSharedProperties(bindings, resource, component);
            } else {
                log.debug("Page Root Provider must be configured for shared component properties to be supported");
            }
            setMergedProperties(bindings, resource);
        }
    }

    private void setSharedProperties(Bindings bindings, Resource resource, Component component) {
        Page pageRoot = pageRootProvider.getRootPage(resource);
        if (pageRoot != null) {
            String globalPropsPath = pageRoot.getPath() + "/jcr:content/global-component-properties";
            Resource globalPropsResource = resource.getResourceResolver().getResource(globalPropsPath);
            if (globalPropsResource != null) {
                bindings.put("globalProperties", globalPropsResource.getValueMap());
            }

            String sharedPropsPath = pageRoot.getPath() + "/jcr:content/shared-component-properties/"
                    + component.getResourceType();
            Resource sharedPropsResource = resource.getResourceResolver().getResource(sharedPropsPath);
            if (sharedPropsResource != null) {
                bindings.put("sharedProperties", sharedPropsResource.getValueMap());
            }
        } else {
            log.debug("Could not determine shared properties root for resource {}", resource.getPath());
        }
    }

    private void setMergedProperties(Bindings bindings, Resource resource) {
        ValueMap globalPropertyMap = (ValueMap) bindings.get("globalProperties");
        ValueMap sharedPropertyMap = (ValueMap) bindings.get("sharedProperties");
        ValueMap localPropertyMap = resource.getValueMap();

        bindings.put("mergedProperties", mergeProperties(localPropertyMap, sharedPropertyMap, globalPropertyMap));
    }

    private Map<String, Object> mergeProperties(ValueMap instanceProperties, ValueMap sharedProperties, ValueMap globalProperties) {
        Map<String, Object> mergedProperties = new HashMap<String, Object>();

        // Add Component Global Configs
        if (globalProperties != null) {
            mergedProperties.putAll(globalProperties);
        }

        // Add Component Shared Configs
        if (sharedProperties != null) {
            mergedProperties.putAll(sharedProperties);
        }

        // Merge in the Component Local Configs
        if (instanceProperties != null) {
            mergedProperties.putAll(instanceProperties);
        }

        return mergedProperties;
    }
}
