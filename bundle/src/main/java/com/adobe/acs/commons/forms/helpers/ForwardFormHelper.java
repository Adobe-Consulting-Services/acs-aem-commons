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

/**
 * Interface used for working with ACS-AEM-Commons forms.
 */
public interface ForwardFormHelper extends FormHelper {
    public final String REQUEST_ATTR_FORM_KEY = "__acs-aem-commons_form_";

    /**
     * Creates a synthetic GET request that can be used in the context of a real
     * POST request to retrieve GET renditions of resources.
     *
     * This method is best used for AJAX-based forms, as the result on error is
     * only the markup associated with the erroring form (not the whole page)
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

    /**
     * Creates a synthetic GET request that can be used in the context of a real
     * POST request to retrieve GET renditions of resources.
     *
     * This method is best used for AJAX-based forms, as the result on error is
     * only the markup associated with the erroring form (not the whole page)
     *
     * Same as above, but uses empty RequestDispatcherOptions.
     *
     * @param form
     * @param resource
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public void forwardAsGet(final Form form, final Resource resource,
                             final SlingHttpServletRequest request,
                             final SlingHttpServletResponse response) throws ServletException, IOException;

    /**
     * Creates a synthetic GET request that can be used in the context of a real
     * POST request to retrieve GET renditions of resources.
     *
     * This method is best used for full POST-back forms that perform a full synchronous POST
     * on submission.
     *
     * Note: BrowserMap JS may auto-redirect the result of this page, the the target Page loads BrowserMap JS libs.
     *
     * @param form
     * @param page
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public void forwardAsGet(final Form form, final Page page,
                             final SlingHttpServletRequest request,
                             final SlingHttpServletResponse response) throws ServletException,IOException;


    /**
     /**
     * Creates a synthetic GET request that can be used in the context of a real
     * POST request to retrieve GET renditions of resources.
     *
     * This method is best used for a customized scenarios where the current resource or currentPage do not suffice.
     *
     * Note: BrowserMap JS may auto-redirect the result is a CQ Page that loads BrowserMap.
     *
     * @param form
     * @param path
     * @param request
     * @param response
     * @param options
     * @throws ServletException
     * @throws IOException
     */
    public void forwardAsGet(final Form form, final String path,
                             final SlingHttpServletRequest request,
                             final SlingHttpServletResponse response,
                             final RequestDispatcherOptions options) throws ServletException, IOException;

    /**
     /**
     * Creates a synthetic GET request that can be used in the context of a real
     * POST request to retrieve GET renditions of resources.
     *
     * This method is best used for a customized scenarios where the current resource or currentPage do not suffice.
     *
     * Same as above, but uses empty RequestDispatcherOptions.
     *
     * Note: BrowserMap JS may auto-redirect the result is a CQ Page that loads BrowserMap.
     *
     * @param form
     * @param path
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public void forwardAsGet(final Form form, final String path,
                             final HttpServletRequest request,
                             final HttpServletResponse response) throws ServletException, IOException;
}