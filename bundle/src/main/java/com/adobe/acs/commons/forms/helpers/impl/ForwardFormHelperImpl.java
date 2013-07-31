package com.adobe.acs.commons.forms.helpers.impl;

import com.adobe.acs.commons.forms.Form;
import com.adobe.acs.commons.forms.helpers.FormHelper;
import com.adobe.acs.commons.forms.helpers.ForwardFormHelper;
import com.adobe.acs.commons.forms.helpers.impl.synthetics.SyntheticSlingHttpServletGetRequest;
import com.day.cq.wcm.api.Page;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;

@Component(label = "ACS AEM Commons - Forward Form Manager", description = "Internal Forward-as-GET Form Helper", enabled = true, metatype = true, immediate = false, inherit = true)
@Properties({ @Property(label = "Vendor", name = Constants.SERVICE_VENDOR, value = "ACS", propertyPrivate = true) })
@Service( value = { FormHelper.class, ForwardFormHelper.class })
public class ForwardFormHelperImpl extends PostFormHelperImpl implements ForwardFormHelper {
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    @Override
    public Form getForm(final String formName, final SlingHttpServletRequest request) {
        if(this.doHandlePost(formName, request)) {
            // Read the request from the POST parameters
            return this.getPostForm(formName, request);
        } else {
            final String key = this.getLookupKey(formName);
            final Object obj = request.getAttribute(key);
            if (obj instanceof Form) {
                Form form = this.getProtectedForm((Form) obj);
                form = this.setResourcePath(form, request);
                return form;
            } else {
                log.info("Unable to find Form in Request attribute: [ {} => {} ]", key, obj);
                Form form = new Form(formName);
                form = this.setResourcePath(form, request);
                return form;
            }
        }
    }

    @Override
    public void forwardAsGet(final Form form, final Page page,
                             final SlingHttpServletRequest request,
                             final SlingHttpServletResponse response) throws ServletException,
            IOException {
        final RequestDispatcherOptions options = new RequestDispatcherOptions();
        options.setReplaceSelectors("");
        options.setForceResourceType("cq/Page");
        final String path = page.getPath() + FormHelper.EXTENSION;
        forwardAsGet(form, path, request, response, options);
    }

    @Override
    public void forwardAsGet(final Form form, final Resource resource,
                             final SlingHttpServletRequest request,
                             final SlingHttpServletResponse response) throws ServletException,
            IOException {
        final RequestDispatcherOptions options = new RequestDispatcherOptions();
        forwardAsGet(form, resource, request, response, options);
    }

    @Override
    public void forwardAsGet(final Form form, final Resource resource,
                             final SlingHttpServletRequest request,
                             final SlingHttpServletResponse response,
                             final RequestDispatcherOptions options) throws ServletException,
            IOException {
        final String path = resource.getPath();
        forwardAsGet(form, path, request, response, options);
    }

    @Override
    public void forwardAsGet(final Form form, final String path,
                             final SlingHttpServletRequest request,
                             final SlingHttpServletResponse response,
                             final RequestDispatcherOptions options) throws ServletException,
            IOException {
        this.setRequestAttrForm(request, form);

        final SlingHttpServletRequest syntheticRequest = new SyntheticSlingHttpServletGetRequest(
                request);
        log.debug("Forwarding as GET to path: {} ", path);
        log.debug("Forwarding as GET w/ replace selectors: {} ", options.getReplaceSelectors());
        log.debug("Forwarding as GET w/ add selectors: {} ", options.getAddSelectors());
        log.debug("Forwarding as GET w/ suffix: {} ", options.getReplaceSuffix());
        log.debug("Forwarding as GET w/ forced resourceType: {} ", options.getForceResourceType());

        request.getRequestDispatcher(path, options).forward(syntheticRequest, response);
    }

    /**
     * Private methods
     **/

   private String getLookupKey(String formName) {
        return REQUEST_ATTR_FORM_KEY + formName;
    }

    private void setRequestAttrForm(final SlingHttpServletRequest request,
                                    final Form form) {
        final String key = this.getLookupKey(form.getName());
        request.setAttribute(key, form);
    }
}
