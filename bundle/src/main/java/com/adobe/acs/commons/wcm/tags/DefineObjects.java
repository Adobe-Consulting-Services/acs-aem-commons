package com.adobe.acs.commons.wcm.tags;

import aQute.bnd.annotation.ProviderType;
import com.day.cq.commons.LanguageUtil;
import com.day.cq.wcm.api.components.Component;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tldgen.BodyContentType;
import tldgen.Tag;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.servlet.ServletRequest;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by derek on 6/9/16.
 */
@ProviderType
@Tag(bodyContentType = BodyContentType.JSP, value = "defineObjects")
public class DefineObjects extends BodyTagSupport {

    private static final Logger log = LoggerFactory.getLogger(DefineObjects.class);

    public DefineObjects() {

    }

    @Override
    public int doEndTag() {
        log.info("Starting the doEndTag");
        Node componentPropertyHome = getComponentPropertyHome();
        setProperties(componentPropertyHome);
        return EVAL_PAGE;
    }

    protected Node getComponentPropertyHome() {
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
                return r.adaptTo(Node.class);
            }

        } catch (RepositoryException e) {
            log.error("Could node get current node info.", e);
        }

        return null;
    }

    protected void setProperties(Node componentPropertyHome) {
        try {
            if (componentPropertyHome != null) {
                ServletRequest req = this.pageContext.getRequest();
                PropertyIterator it = componentPropertyHome.getProperties();
                Map<String,  String> propertyMap = new HashMap<String,  String>();
                while (it.hasNext()) {
                    Property p = it.nextProperty();
                    String propName = p.getName();
                    if (!"jcr:primaryType".equalsIgnoreCase(propName)) {
                        javax.jcr.Value v = p.getValue();
                        if (v != null) {
                            propertyMap.put(p.getName(), v.toString());
                        } else {
                            propertyMap.put(p.getName(), null);
                        }
                    }
                }

                pageContext.setAttribute("sitewideProperties", propertyMap);
            }
        } catch (RepositoryException e) {
            log.error("Could node set properties on node.", e);
        }
    }

    protected Map<String, String> mergeProperties(Map<String, String> instanceProps, Map<String, String> sitewideProps) {
        Map<String, String> mergedProperties = new HashMap<String, String>();

        Set<String> instanceKeys = instanceProps.keySet();
        for (String instanceKey : instanceKeys) {
            mergedProperties.put(instanceKey, instanceProps.get(instanceKey));
        }

        Set<String> sitewideKeys = sitewideProps.keySet();
        for (String sitewideKey : sitewideKeys) {
            if (!mergedProperties.containsKey(sitewideKey)) {
                mergedProperties.put(sitewideKey, sitewideProps.get(sitewideKey));
            }
        }

        return mergedProperties;
    }
}
