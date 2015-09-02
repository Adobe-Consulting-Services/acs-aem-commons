package com.adobe.acs.commons.synth;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SlingServlet(
        paths = "/services/synthesizer"
)
public class SynthesizerServlet extends SlingSafeMethodsServlet {

    private static final String P_PREFIX = "p.";

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        String resourceType = request.getParameter("resourceType");

        List<RequestParameter> params = request.getRequestParameterList();
        Map<String, Object> props = new HashMap<String, Object>(params.size());
        for (RequestParameter p : params) {
            if (p.getName().startsWith(P_PREFIX)) {
                String propName = p.getName().substring(P_PREFIX.length(), p.getName().length());
                props.put(propName, p.getString());
            }
        }

        Resource resource = Synthesizer.buildResource(request.getResourceResolver(), resourceType, props);
        String output = Synthesizer.render(resource, request, response);

        response.getWriter().append(output);
    }
}
