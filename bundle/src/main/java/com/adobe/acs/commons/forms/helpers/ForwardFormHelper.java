package com.adobe.acs.commons.forms.helpers;

import com.adobe.acs.commons.forms.Form;
import com.day.cq.wcm.api.Page;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface ForwardFormHelper extends FormHelper {
    public final String REQUEST_ATTR_FORM_KEY = "__ACS AEM COMMONS : Request Attr ~> Form : ";

    /**
     * Creates a synthetic GET request that can be used in the context of a real
     * POST request to retrieve GET renditions of resources.
     *
     * @param form
     * @param resource
     * @param request
     * @param response
     * @param options
     * @throws javax.servlet.ServletException
     * @throws java.io.IOException
     */
    public void forwardAsGet(final Form form, final Resource resource,
                             final SlingHttpServletRequest request,
                             final SlingHttpServletResponse response,
                             final RequestDispatcherOptions options) throws ServletException, IOException;

    public void forwardAsGet(final Form form, final Page page,
                             final SlingHttpServletRequest request,
                             final SlingHttpServletResponse response) throws ServletException,IOException;


    public void forwardAsGet(final Form form, final Resource resource,
                             final SlingHttpServletRequest request,
                             final SlingHttpServletResponse response) throws ServletException, IOException;

    public void forwardAsGet(final Form form, final String path,
                             final HttpServletRequest request,
                             final HttpServletResponse response) throws ServletException, IOException;
}