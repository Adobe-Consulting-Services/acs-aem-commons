package com.adobe.acs.commons.oak.impl;

import com.adobe.acs.commons.oak.EnsureOakIndexManager;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * HTTP POST /system/console/ensure-oak-index
 * Parameters
 * force = true | false (optional; when blank defaults to false)
 * path = /abs/path/to/ensure/definition (optional; when blank indicates all)
 */
//@formatter:off
@Component(immediate = true)
@Properties({
        @Property(
                name = "felix.webconsole.label",
                value = "Ensure Oak Index"
        )
})
@Service(Servlet.class)
//@formatter:on
public class EnsureOakIndexServlet extends HttpServlet {
    //@formatter:off
    private static final String PARAM_FORCE = "force";
    private static final String PARAM_PATH = "path";

    @Reference
    private EnsureOakIndexManager ensureOakIndexManager;
    //@formatter:on

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {

        response.getWriter().println("<pre>");
        response.getWriter().println();
        response.getWriter().println();
        response.getWriter().println("HTTP method GET is not supported by this URL");
        response.getWriter().println("Use HTTP POST to access this end-point");
        response.getWriter().println("--------------------------------------------");
        response.getWriter().println("HTTP POST /system/console/ensure-oak-index");
        response.getWriter().println(" Parameters");
        response.getWriter().println("   * force = true | false (optional; when blank defaults to false)");
        response.getWriter().println("   * path = /abs/path/to/ensure/definition (optional; when blank indicates all)");
        response.getWriter().println();
        response.getWriter().println();
        response.getWriter().println("Example: curl --user admin:admin --data \"force=true\" https://localhost:4502/system/console/ensure-oak-index");
        response.getWriter().println("</pre>");

        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String forceParam = StringUtils.defaultIfEmpty(request.getParameter(PARAM_FORCE), "false");
        boolean force = Boolean.valueOf(forceParam);

        String path = StringUtils.stripToNull(request.getParameter(PARAM_PATH));

        int count = 0;
        if (StringUtils.isBlank(path)) {
            count = ensureOakIndexManager.ensureAll(force);
        } else {
            count = ensureOakIndexManager.ensure(force, path);
        }

        response.setContentType("text/plain; charset=utf-8");
        response.getWriter().println("Initiated the ensuring of " + count + " oak indexes");
        response.setStatus(HttpServletResponse.SC_OK);
    }
}