package com.adobe.acs.commons.wcm.debug;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.PropertiesUtil;

@Component(
        label = "ACS AEM Commons - Sling Component Debug Filter",
        policy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        metatype = true
)
@Properties({
                    @Property(
                            name = "service.ranking",
                            intValue = -2000,
                            propertyPrivate = true
                    ),
                    @Property(
                            name = "sling.filter.scope",
                            value = "COMPONENT",
                            propertyPrivate = true
                    ),
                    @Property(
                            name = "sling.filter.pattern",
                            value = "/.*",
                            propertyPrivate = true
                    )
            })
@Service
public class SlingComponentDebugFilter implements Filter {

    @Property(label = "Is Enabled",
              description = "Enables/Disables the filter which prints resource debugging information in the HTML",
              boolValue = false)
    public static final String IS_ENABLED = "isEnabled";

    private boolean isEnabled;

    public class CharResponseWrapper extends HttpServletResponseWrapper {
        private CharArrayWriter output;

        public String toString() {
            return output.toString();
        }

        public CharResponseWrapper(final HttpServletResponse response) {
            super(response);
            output = new CharArrayWriter();
        }

        public PrintWriter getWriter() {
            return new PrintWriter(output);
        }
    }

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        final SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;
        final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
        final Resource resource = slingRequest.getResource();
        if (canDebugRequest(slingRequest)) {
            filterResponse(slingRequest, slingResponse, resource, filterChain);
        } else {
            filterChain.doFilter(slingRequest, response);
        }
    }

    private boolean canDebugRequest(final SlingHttpServletRequest request)  {
        boolean canDebugRequest = false;
        final String path = request.getPathInfo();
        if (isEnabled) {
            if (path.contains(".html")) {
                canDebugRequest = true;
            }
        }
        return canDebugRequest;
    }

    private void filterResponse(final SlingHttpServletRequest slingRequest, final SlingHttpServletResponse slingResponse,
            final Resource resource, final FilterChain chain) throws IOException, ServletException {

        final PrintWriter out = slingResponse.getWriter();
        final CharResponseWrapper responseWrapper = new CharResponseWrapper((HttpServletResponse) slingResponse);

        chain.doFilter(slingRequest, responseWrapper);

        final StringBuilder servletResponse = new StringBuilder(responseWrapper.toString());

        if (!servletResponse.toString().contains("acs:resourcePath")) {
            servletResponse.append("<!-- {acs:resourcePath:" + resource.getPath() + "-->");
            out.write(servletResponse.toString());
        } else {
            out.write(servletResponse.toString());
        }
    }

    @Activate
    protected void activate(final Map<String, String> properties) {
        this.isEnabled = PropertiesUtil.toBoolean(properties.get(IS_ENABLED), false);
    }

    @Override
    public void destroy() {

    }
}
