package com.adobe.acs.commons.wcm.tags;

import aQute.bnd.annotation.ProviderType;
import com.day.cq.wcm.api.components.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tldgen.BodyContentType;
import tldgen.Tag;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
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
            log.info("Current node is " + currentNode.getPath());
            Node baseNode = currentNode.getParent().getParent();
            log.info("Base node is " + baseNode.getPath());
            Node sitewideProps = baseNode.getNode("sitewideprops");
            log.info("sitewideprops path is " + sitewideProps.getPath());

            Component component = (Component) pageContext.findAttribute("component");
            log.info("Component path is " + component.getPath());

            Node sitewidePropsComponent = sitewideProps.getNode("." + component.getPath());
            log.info("sitewideprops component path is " + sitewidePropsComponent.getPath());

            return sitewidePropsComponent;
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
                        Value v = p.getValue();
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
