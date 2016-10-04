package com.adobe.acs.commons.wcm.impl;

import com.adobe.acs.commons.wcm.PageRootProvider;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.components.Component;
import com.day.cq.wcm.commons.WCMUtils;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.resource.JcrPropertyMap;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.script.Bindings;
import java.util.HashMap;
import java.util.Map;

/**
 * Bindings Values Provider that adds bindings for sharedProperties
 * and mergedProperties maps.
 *
 * sharedProperties contains the shared properties for the current
 * component.
 *
 * mergedProperties is a merge of the instance-level and shared properties
 * for the current component, giving preference to an instance-level
 * property value when a shared property exists with the same name.
 */
@org.apache.felix.scr.annotations.Component(
    label = "ACS AEM Commons - Shared Component Properties Provider",
    description = "Adds bindings for \"sharedProperties\" and \"mergedProperties\"."
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
        // Build the path to the global config for this component
        // <page root>/jcr:content/sharedcomponentproperties/<component resource type>
        Page pageRoot = pageRootProvider.getRootPage(resource);
        if (pageRoot != null) {
            String sharedPropsPath = pageRoot.getPath() + "/jcr:content/sharedcomponentproperties/";
            sharedPropsPath = sharedPropsPath + component.getResourceType();

            Resource sharedPropsResource = resource.getResourceResolver().getResource(sharedPropsPath);
            if (sharedPropsResource != null) {
                JcrPropertyMap jpm = new JcrPropertyMap(sharedPropsResource.adaptTo(Node.class));
                bindings.put("sharedProperties", jpm);
            }
        } else {
            log.debug("Could not determine shared properties root for resource {}", resource.getPath());
        }
    }

    private void setMergedProperties(Bindings bindings, Resource resource) {
        JcrPropertyMap sharedPropertyMap = (JcrPropertyMap) bindings.get("sharedProperties");
        JcrPropertyMap localPropertyMap = (JcrPropertyMap) resource.getValueMap();

        bindings.put("mergedProperties", mergeProperties(localPropertyMap, sharedPropertyMap));
    }

    private Map<String, Object> mergeProperties(JcrPropertyMap instanceProperties, JcrPropertyMap sharedProperties) {
        Map<String, Object> mergedProperties = new HashMap<String, Object>();

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
