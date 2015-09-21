package com.adobe.acs.commons.analysis.jcrchecksum.impl;
import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.Externalizer;

/**
 * This filter adds CORS headers to allow Cross-Origin ajax requests for the Checksum Generator and JSONDumpServlet.
 *
 */
@Component(immediate=true, metatype=false)
@Service(value=javax.servlet.Filter.class)
@Properties({
    @Property(name="service.description", value="ACS AEM Commons CORS Filter"),
    @Property(name="pattern", value=ChecksumGeneratorServlet.SERVLET_PATH + ".*"),
    @Property(name="service.ranking", value="2147483647"),
})
public class CORSOptionsFilter implements Filter {
    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    private static final String ORIGIN = "Origin";
    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, policy = ReferencePolicy.STATIC)
    protected Externalizer externalizer;
    
    public void init(FilterConfig filterConfig) throws ServletException {
        // Nothing to do
        log.debug("CORS Filter initialize");
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
        ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            
            //Check that it is an options request.
            boolean isOptions = httpRequest.getMethod().equalsIgnoreCase("OPTIONS");
            if(isOptions) {
                if(httpRequest.getRequestURI().startsWith(ChecksumGeneratorServlet.SERVLET_PATH)) {
                    final String origin = httpRequest.getHeader(ORIGIN);
                    if (origin != null && origin.length() > 0) {
                        httpResponse.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                        httpResponse.setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
                        httpResponse.setHeader(ACCESS_CONTROL_ALLOW_HEADERS, "Authorization");
                        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST");
                        if(isOptions) httpResponse.setStatus(200);
                        log.debug("Set CORS Headers");
                        return;
                    }
                }
            }
            chain.doFilter(request, response);
        } else {
            chain.doFilter(request, response);
        }
   }

   public void destroy() {
        // Nothing to do
   }

}