package com.adobe.acs.commons.wcm.tags;

import aQute.bnd.annotation.ProviderType;
import com.adobe.acs.commons.wcm.PageRootProvider;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.components.Component;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.JcrPropertyMap;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tldgen.BodyContentType;
import tldgen.Tag;

import javax.jcr.Node;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.util.HashMap;
import java.util.Map;

/**
 * This tag is similar to the OOTB cq:defineObjects tag which adds
 * component instance-level properties to the pageContext of a JSP,
 * but it instead sets sharedComponentProperties and
 * mergedProperties maps.
 *
 * sharedComponentProperties contains the shared properties for
 * the current component.
 *
 * mergedProperties is a merge of the instance-level and shared
 * properties for the current component, giving preference to an
 * instance-level property value when a shared property exists
 * with the same name.
 */
@ProviderType
@Tag(bodyContentType = BodyContentType.JSP, value = "defineObjects")
public class DefineObjects extends BodyTagSupport {

    private static final Logger log = LoggerFactory.getLogger(DefineObjects.class);

    private PageRootProvider pageRootProvider;

    @Override
    public int doEndTag() {
        setPageRootProvider();
        if (pageRootProvider != null) {
            setSharedProperties();
        } else {
            log.debug("Page Root Provider must be configured for shared component properties to be supported");
        }
        setMergedProperties();
        return EVAL_PAGE;
    }

    private void setPageRootProvider() {
        BundleContext bundleContext = FrameworkUtil.getBundle(PageRootProvider.class).getBundleContext();
        ServiceReference factoryRef = bundleContext.getServiceReference(PageRootProvider.class.getName());
        if (factoryRef != null) {
            pageRootProvider = (PageRootProvider) bundleContext.getService(factoryRef);
        }
    }

    private void setSharedProperties() {
        Resource currentResource = (Resource) pageContext.findAttribute("resource");
        ResourceResolver resourceResolver = (ResourceResolver) pageContext.findAttribute("resourceResolver");

        // Build the path to the global config for this component
        // <page root>/jcr:content/sharedcomponentproperties/<component resource type>
        Page pageRoot = pageRootProvider.getRootPage(currentResource);
        if (pageRoot != null) {
            String globalPropsPath = pageRoot.getPath() + "/jcr:content/sharedcomponentproperties/";
            Component component = (Component) pageContext.findAttribute("component");
            globalPropsPath = globalPropsPath + component.getResourceType();

            Resource r = resourceResolver.getResource(globalPropsPath);

            // Send the Node back if it exists
            if (r != null) {
                JcrPropertyMap jpm = new JcrPropertyMap(r.adaptTo(Node.class));
                pageContext.setAttribute("sharedComponentProperties", jpm);
            }
        } else {
            log.debug("Could not determine shared properties root for resource {}", currentResource.getPath());
        }
    }

    private void setMergedProperties() {
        JcrPropertyMap sharedComponentPropertyMap = (JcrPropertyMap) pageContext.getAttribute("sharedComponentProperties");
        JcrPropertyMap localPropertyMap = (JcrPropertyMap) pageContext.getAttribute("properties");

        pageContext.setAttribute("mergedProperties", mergeProperties(localPropertyMap, sharedComponentPropertyMap));
    }

    private Map<String, Object> mergeProperties(JcrPropertyMap instanceProperties, JcrPropertyMap sharedProperties) {
        Map<String, Object> mergedProperties = new HashMap<String, Object>();

        // Add Component Global Configs
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
