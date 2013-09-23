package com.adobe.acs.commons.wcm.impl;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.commons.json.jcr.JsonItemWriter;
import org.apache.sling.commons.osgi.PropertiesUtil;

import com.adobe.acs.commons.util.PathInfoUtil;
import com.adobe.granite.xss.XSSAPI;

/**
 * Servlets which allows for dynamic selection of RTE configuration. To use in a 
 * component, specify the xtype of <pre>slingscriptinclude</pre> and set the script
 * to <pre>rte.CONFIGNAME.FIELDNAME.json.jsp</pre>. This will iterate through nodes
 * under /etc/rteconfig to find a matching site (by regex). Then, look for a node named
 * CONFIGNAME and use that configuration.
 */
@SuppressWarnings("serial")
@SlingServlet(extensions = "json", selectors = "rte", resourceTypes = "sling/servlet/default")
public class RTEConfigurationServlet extends SlingSafeMethodsServlet {

    private static final String DEFAULT_CONFIG_NAME = "default";

    @Reference
    private XSSAPI xssApi;

    private static final String DEFAULT_CONFIG = "/libs/foundation/components/text/dialog/items/tab1/items/text";

    private static final String DEFAULT_ROOT_PATH = "/etc/rteconfig";

    @Property(value = DEFAULT_ROOT_PATH)
    private static final String PROP_ROOT_PATH = "root.path";

    private String rootPath;

    @Activate
    protected void activate(Map<String, Object> props) {
        rootPath = PropertiesUtil.toString(props.get(PROP_ROOT_PATH), DEFAULT_ROOT_PATH);
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException,
            IOException {
        String componentPath = request.getResource().getPath();

        String configName = PathInfoUtil.getSelector(request, 1, DEFAULT_CONFIG_NAME);

        // the actual property name
        String rteName = PathInfoUtil.getSelector(request, 2, "text");

        Resource root = request.getResourceResolver().getResource(rootPath);
        if (root != null) {
            Iterator<Resource> children = root.listChildren();
            while (children.hasNext()) {
                Resource child = children.next();
                if (matches(componentPath, child)) {
                    Resource config = child.getChild(configName);
                    if (config == null) {
                        config = child.getChild(DEFAULT_CONFIG_NAME);
                    }
                    if (config != null) {
                        try {
                            writeConfigResource(config, rteName, response);
                        } catch (JSONException e) {
                            throw new ServletException(e);
                        }

                        return;
                    }
                }
            }
        }

        returnDefault(rteName, request, response);

    }

    private boolean matches(String componentPath, Resource resource) {
        ValueMap map = resource.adaptTo(ValueMap.class);
        if (map == null) {
            return false;
        }
        String pattern = map.get("pattern", String.class);
        if (pattern == null) {
            return false;
        }
        return componentPath.matches(pattern);
    }

    private void returnDefault(String rteName, SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException, ServletException {
        response.setContentType("application/json");
        try {
            Resource root = request.getResourceResolver().getResource(DEFAULT_CONFIG);
            if (root == null) {
                writeEmptyRTE(rteName, response);
                return;
            }

            writeConfigResource(root, rteName, response);
        } catch (JSONException e) {
            throw new ServletException(e);
        }
    }

    private void writeConfigResource(Resource resource, String rteName, SlingHttpServletResponse response)
            throws IOException, JSONException, ServletException {
        Node node = resource.adaptTo(Node.class);
        if (node == null) {
            writeEmptyRTE(rteName, response);
            return;
        }

        JsonItemWriter writer = new JsonItemWriter(null);
        StringWriter string = new StringWriter();
        try {
            writer.dump(node, string, -1);
        } catch (RepositoryException e) {
            throw new ServletException(e);
        }
        JSONObject config = new JSONObject(string.toString());
        config.put("name", "./" + xssApi.encodeForJSString(rteName));

        JSONObject parent = new JSONObject();
        parent.put("xtype", "dialogfieldset");
        parent.put("border", false);
        parent.put("padding", 0);

        // these two size properties seem to be necessary to get the size correct
        // in a component dialog
        config.put("width", 430);
        config.put("height", 200);
        parent.accumulate("items", config);
        parent.write(response.getWriter());
    }

    private void writeEmptyRTE(String rteName, SlingHttpServletResponse response) throws IOException, JSONException {
        JSONWriter writer = new JSONWriter(response.getWriter());
        writer.object();
        writer.key("xtype");
        writer.value("richtext");
        writer.key("name");
        writer.value("./" + xssApi.encodeForJSString(rteName));
        writer.value("hideLabel");
        writer.value(true);
        writer.key("jcr:primaryType");
        writer.value("cq:Widget");
    }

}
