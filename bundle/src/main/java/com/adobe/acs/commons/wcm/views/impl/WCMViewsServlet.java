package com.adobe.acs.commons.wcm.views.impl;

import com.adobe.acs.commons.util.OsgiPropertyUtil;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMMode;
import com.day.cq.wcm.api.components.Component;
import com.day.cq.wcm.commons.WCMUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.AbstractResourceVisitor;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;

@SlingServlet(
        label = "ACS AEM Commons - WCM Views Servlet",
        description = "Sample implementation of a Sling All Methods Servlet.",
        methods = {"GET"},
        resourceTypes = {"cq/Page"},
        selectors = {"wcm-views"},
        extensions = {"json"},
        metatype = true
)
public class WCMViewsServlet extends SlingSafeMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(WCMViewsServlet.class);

    private static final String[] DEFAULT_VIEWS = new String[]{};
    private Map<String, String[]> defaultViews = new HashMap<String, String[]>();
    @Property(label = "WCM Views by Path",
            description = "Views to add to the Sidekick by default. Takes format [/path=view-1,view-2]",
            cardinality = Integer.MAX_VALUE,
            value = {})
    public static final String PROP_DEFAULT_VIEWS = "wcm-views";
    
    @Override
    protected final void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws
            ServletException, IOException {

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        if (WCMMode.DISABLED.equals(WCMMode.fromRequest(request))) {
            response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("");
            return;
        }
        
        /* Valid WCMMode */

        final PageManager pageManager = request.getResourceResolver().adaptTo(PageManager.class);
        final Page page = pageManager.getContainingPage(request.getResource());

        final WCMViewsResourceVisitor visitor = new WCMViewsResourceVisitor();
        visitor.accept(page.getContentResource());

        final Set<String> viewSet = new HashSet<String>(visitor.getWCMViews());

        // Get the Views provided by the Servlet
        for(final Map.Entry<String, String[]> entry : this.defaultViews.entrySet()) {
            if(StringUtils.startsWith(page.getPath(), entry.getKey())) {
                viewSet.addAll(Arrays.asList(entry.getValue()));
            }
        }
        
        final List<String> views = new ArrayList<String>(viewSet);
        
        Collections.sort(views);

        log.debug("Collected WCM Views {} for Page [ {} ]", views, page.getPath());
        
        final JSONArray jsonArray = new JSONArray();

        for (final String view : views) {
            final JSONObject json = new JSONObject();

            try {
                json.put("title", StringUtils.capitalize(view) + " View");
                json.put("value", view);

                jsonArray.put(json);
            } catch (JSONException e) {
                log.error("Unable to build WCM Views JSON output.", e);
            }
        }

        response.getWriter().write(jsonArray.toString());
    }

    private class WCMViewsResourceVisitor extends AbstractResourceVisitor {
        final Set<String> views = new TreeSet<String>();

        public final List<String> getWCMViews() {
            return new ArrayList(this.views);
        }

        @Override
        protected void visit(Resource resource) {
            final ValueMap properties = resource.adaptTo(ValueMap.class);
            final String[] resourceViews = properties.get(WCMViewsFilter.PN_WCM_VIEWS, String[].class);

            if (ArrayUtils.isNotEmpty(resourceViews)) {
                this.views.addAll(Arrays.asList(resourceViews));
            }

            final Component component = WCMUtils.getComponent(resource);
            if (component != null) {
                final String[] componentViews = component.getProperties().get(WCMViewsFilter.PN_WCM_VIEWS, String[].class);

                if (ArrayUtils.isNotEmpty(componentViews)) {
                    this.views.addAll(Arrays.asList(componentViews));
                }
            }
        }
    }

    @Activate
    protected final void activate(final Map<String, String> config) {
        final String[] tmp = PropertiesUtil.toStringArray(config.get(PROP_DEFAULT_VIEWS), DEFAULT_VIEWS);
        this.defaultViews = OsgiPropertyUtil.toMap(tmp, "=", ",");
    }
}