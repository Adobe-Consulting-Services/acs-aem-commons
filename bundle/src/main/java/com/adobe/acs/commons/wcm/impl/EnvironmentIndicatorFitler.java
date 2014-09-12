package com.adobe.acs.commons.wcm.impl;

import com.adobe.acs.commons.util.BufferingResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

@Component(
        label = "ACS AEM Commons - WebUI Environment Indicator",
        metatype = true
)
@Properties({
    @Property(
            name = "pattern",
            value = ".*",
            propertyPrivate = true
    )
})
@Service
public class EnvironmentIndicatorFitler implements Filter {
    private static final Logger log = LoggerFactory.getLogger(EnvironmentIndicatorFitler.class);

    private static final String DEFAULT_COLOR = "red";
    private String color = DEFAULT_COLOR;
    @Property(label = "Color",
            description = "",
            value = DEFAULT_COLOR)
    public static final String PROP_COLOR = "style-color";

    private static final String DEFAULT_STYLE_OVERRIDE = "";
    private String styleOverride = DEFAULT_STYLE_OVERRIDE;
    @Property(label = "Style Override",
            description = "",
            value = DEFAULT_STYLE_OVERRIDE)
    public static final String PROP_STYLE_OVERRIDE = "style-override";

    private static final String[] REJECT_PATH_PREFIXES = new String[]{
    };

    private String style = "";

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public final void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
                               final FilterChain filterChain) throws IOException, ServletException {

        if(!(servletRequest instanceof HttpServletRequest)
                || !(servletResponse instanceof HttpServletResponse)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (!this.accepts(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        final BufferingResponse capturedResponse = new BufferingResponse(response);

        filterChain.doFilter(request, capturedResponse);

        // Get contents
        final String contents = capturedResponse.getContents();

        if (contents != null) {
            if (StringUtils.contains(response.getContentType(), "html")) {

                final int bodyIndex = contents.indexOf("</body>");

                if (bodyIndex != -1) {
                    final PrintWriter printWriter = response.getWriter();

                    printWriter.write(contents.substring(0, bodyIndex));
                    printWriter.write("<div id=\"acs-aem-commons-env-indicator\" style=\"" + style + "\"></div>");
                    printWriter.write(contents.substring(bodyIndex));
                    return;
                }
            }
        }

        if (contents != null) {
            response.getWriter().write(contents);
        }
    }

    @Override
    public void destroy() {

    }

    private boolean accepts(final HttpServletRequest request) {

        if (!StringUtils.equalsIgnoreCase("get", request.getMethod())) {
            // Only inject on GET requests
            return false;
        } else if (StringUtils.startsWithAny(request.getRequestURI(), REJECT_PATH_PREFIXES)) {
            // Reject any request to well-known rejection-worthy path prefixes
            return false;
        } else if (StringUtils.equals(request.getHeader("X-Requested-With"), "XMLHttpRequest")) {
            // Do not inject into XHR requests
            return false;
        }
        return true;
    }

    private String createStyle(String color) {
        return "background-color:" + color
                + ";position:fixed;left:0;top:0;right:0;height:2px;z-index:100000000000000";
    }

    @Activate
    protected final void activate(final Map<String, String> config) throws IOException, RepositoryException, LoginException {
        color = PropertiesUtil.toString(config.get(PROP_COLOR), DEFAULT_COLOR);
        styleOverride = PropertiesUtil.toString(config.get(PROP_STYLE_OVERRIDE), DEFAULT_STYLE_OVERRIDE);

        if(StringUtils.isBlank(styleOverride)) {
            style = createStyle(color);
        } else {
            style = styleOverride;
        }
    }
}
