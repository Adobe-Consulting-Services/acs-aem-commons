package com.adobe.acs.commons.wcm.tags;

import aQute.bnd.annotation.ProviderType;
import com.day.cq.commons.LanguageUtil;
import com.day.cq.wcm.api.components.Component;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.JcrPropertyMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tldgen.BodyContentType;
import tldgen.Tag;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This tag is similar to the OOTB cq:defineObjects tag which adds
 * component instance-level properties to the pageContext of a JSP,
 * but it instead sets sitewideProperties and mergedProperties maps.
 *
 * sitewideProperties contains the site-wide properties for the
 * current component.
 *
 * mergedProperties is a merge of the instance-level and site-wide
 * properties for the current component, giving preference to an
 * instance-level property value when a site-wide property exists
 * with the same name.
 */
@ProviderType
@Tag(bodyContentType = BodyContentType.JSP, value = "defineObjects")
public class DefineObjects extends BodyTagSupport {

    private static final Logger log = LoggerFactory.getLogger(DefineObjects.class);

    @Override
    public int doEndTag() {
        log.info("Starting the doEndTag");
        setSitewideProperties();
        setMergedProperties();
        return EVAL_PAGE;
    }

    private void setSitewideProperties() {
        Node currentNode = (Node) this.pageContext.findAttribute("currentNode");
        try {
            ResourceResolver resourceResolver = (ResourceResolver) pageContext.findAttribute("resourceResolver");

            // Build the path to the global config for this component
            // <Lang Root>/jcr:content/sitewideprops/<component resource type>
            String languageRoot = LanguageUtil.getLanguageRoot(currentNode.getPath());
            String globalPropsPath = languageRoot + "/jcr:content/sitewideprops/";
            Component component = (Component) pageContext.findAttribute("component");
            globalPropsPath = globalPropsPath + component.getResourceType();

            Resource r = resourceResolver.getResource(globalPropsPath);

            // Send the Node back if it exist
            if (r != null) {
                JcrPropertyMap jpm = new JcrPropertyMap(r.adaptTo(Node.class));
                pageContext.setAttribute("sitewideProperties", jpm);
            }

        } catch (RepositoryException e) {
            log.error("Could node get current node info.", e);
        }
    }

    private void setMergedProperties() {
        JcrPropertyMap sitewidePropertyMap = (JcrPropertyMap) pageContext.getAttribute("sitewideProperties");
        JcrPropertyMap localPropertyMap = (JcrPropertyMap) pageContext.getAttribute("properties");

        pageContext.setAttribute("mergedProperties", mergeProperties(localPropertyMap, sitewidePropertyMap));
    }

    private Map<String, Object> mergeProperties(JcrPropertyMap instanceProps, JcrPropertyMap sitewideProps) {
        Map<String, Object> mergedProperties = new HashMap<String, Object>();

        // Add Component Global Configs
        if (sitewideProps != null) {
            Set<String> sitewideKeys = sitewideProps.keySet();
            for (String sitewideKey : sitewideKeys) {
                mergedProperties.put(sitewideKey, sitewideProps.get(sitewideKey));
            }
        }

        // Merge in the Component Local Configs
        if (instanceProps != null) {
            Set<String> instanceKeys = instanceProps.keySet();
            for (String instanceKey : instanceKeys) {
                mergedProperties.put(instanceKey, instanceProps.get(instanceKey));
            }
        }

        return mergedProperties;
    }
}
