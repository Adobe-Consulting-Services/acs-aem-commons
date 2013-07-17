package com.adobe.acs.commons.forms.helpers.impl;


import com.adobe.acs.commons.forms.Form;
import com.adobe.acs.commons.forms.helpers.FormHelper;
import com.adobe.acs.commons.forms.helpers.ForwardFormHelper;
import com.adobe.acs.commons.forms.helpers.impl.synthetics.SyntheticHttpServletGetRequest;
import com.adobe.acs.commons.forms.helpers.impl.synthetics.SyntheticSlingHttpServletGetRequest;
import com.day.cq.wcm.api.Page;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Component(label = "ACS AEM Commons - Forward Form Manager", description = "Internal Forward-as-GET Form Helper", enabled = true, metatype = false, immediate = false)
@Properties({ @Property(label = "Vendor", name = Constants.SERVICE_VENDOR, value = "ACS", propertyPrivate = true) })
@Service( value = { FormHelper.class, ForwardFormHelper.class })
public class ForwardFormHelperImpl extends AbstractFormHelperImpl implements ForwardFormHelper {
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    /**
     *
     * @param formName
     * @param request
     * @return
     */
    @Override
    public Form getForm(final String formName, final HttpServletRequest request) {
        if(this.doHandlePost(formName, request)) {
            // Read the request from the POST parameters
            return this.getPostForm(formName, request);
        } else {
            final String key = this.getLookupKey(formName);
            final Object obj = request.getAttribute(key);
            if (obj instanceof Form) {
                return (Form) obj;
            } else {
                log.info("Unable to find Form in request attribute: [ {} => {} ]", key, obj);
                return new Form(formName);
            }
        }
    }

    /**
     *
     * @param formName
     * @param request
     * @return
     */
    protected boolean doHandle(String formName, HttpServletRequest request) {
       if(this.doHandlePost(formName, request)) {
           return true;
       } else if(!StringUtils.equalsIgnoreCase("GET", request.getMethod())) {
            // If not a *valid* POST request, then only accept GET requests
            return false;
        }

        final String key = this.getLookupKey(formName);
        final Object obj = request.getAttribute(key);
        if (obj instanceof Form) {
            return true;
        } else {
            return false;
        }
    }

    /**
     *
     * @param form
     * @param path
     * @param request
     * @param response
     * @throws javax.servlet.ServletException
     * @throws java.io.IOException
     */
    @Override
    public void forwardAsGet(final Form form, final String path,
                             final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        this.setRequestAttrForm(request, form);

        final HttpServletRequest syntheticRequest = new SyntheticHttpServletGetRequest(
                request);

        request.getRequestDispatcher(path).forward(syntheticRequest, response);
    }

    /**
     *
     * @param form
     * @param page
     * @param request
     *
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public void forwardAsGet(final Form form, final Page page,
                             final SlingHttpServletRequest request,
                             final SlingHttpServletResponse response) throws ServletException,
            IOException {
        forwardAsGet(form, page.adaptTo(Resource.class), request, response);
    }

    /**
     *
     * @param form
     * @param resource
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public void forwardAsGet(final Form form, final Resource resource,
                             final SlingHttpServletRequest request,
                             final SlingHttpServletResponse response) throws ServletException,
            IOException {
        final RequestDispatcherOptions options = new RequestDispatcherOptions();
        forwardAsGet(form, resource, request, response, options);
    }

    /**
     *
     * @param form
     * @param resource
     * @param request
     * @param response
     * @param options
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public void forwardAsGet(final Form form, final Resource resource,
                             final SlingHttpServletRequest request,
                             final SlingHttpServletResponse response,
                             final RequestDispatcherOptions options) throws ServletException,
            IOException {
        this.setRequestAttrForm(request, form);

        final SlingHttpServletRequest syntheticRequest = new SyntheticSlingHttpServletGetRequest(
                request);

        final String path = resource.getPath() + ".html";
        request.getRequestDispatcher(path, options).forward(syntheticRequest, response);
    }

    /**
     * Private methods
     **/

    /**
     *
     * @param formName
     * @return
     */
    private String getLookupKey(String formName) {
        return REQUEST_ATTR_FORM_KEY + formName;
    }

    /**
     *
     * @param request
     * @param form
     */
    private void setRequestAttrForm(final HttpServletRequest request,
                                    final Form form) {
        final String key = this.getLookupKey(form.getName());
        request.setAttribute(key, form);
    }
}
