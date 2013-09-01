package com.adobe.acs.commons.forms.helpers.impl;

import com.adobe.acs.commons.forms.Form;
import com.adobe.acs.commons.forms.helpers.FormHelper;
import com.adobe.acs.commons.forms.helpers.PostRedirectGetFormHelper;
import com.adobe.acs.commons.util.TypeUtil;
import com.day.cq.wcm.api.Page;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

@Component(label = "ACS AEM Commons - POST-Redirect-GET Form Helper", description = "POST-Redirect-GET Form Helper", enabled = true, metatype = false, immediate = false, inherit = true)
@Properties({ @Property(label = "Vendor", name = Constants.SERVICE_VENDOR, value = "ACS", propertyPrivate = true) })
@Service( value = { FormHelper.class, PostRedirectGetFormHelper.class })
public class PostRedirectGetFormHelperImpl extends PostFormHelperImpl implements PostRedirectGetFormHelper {
    private static final Logger log = LoggerFactory.getLogger(PostRedirectGetFormHelperImpl.class);

    @Override
    public Form getForm(final String formName, final SlingHttpServletRequest request) {
		if (this.doHandlePost(formName, request)) {
			log.debug("Getting FORM [ {} ] from POST parameters", formName);
			return this.getPostForm(formName, request);
		} else if(this.doHandleGet(formName, request)) {
            log.debug("Getting FORM [ {} ] from GET parameters", formName);
            return this.getGetForm(formName, request);
        }

        log.debug("Creating empty form for FORM [ {} ]", formName);
        return new Form(formName, request.getResource().getPath());
	}

    @Override
    public void sendRedirect(Form form, String path, SlingHttpServletResponse response) throws IOException, JSONException {
        this.sendRedirect(form, path, null, response);
    }

    @Override
    public void sendRedirect(Form form, Page page, SlingHttpServletResponse response) throws IOException, JSONException {
        this.sendRedirect(form, page, null, response);
    }

    @Override
    public void sendRedirect(Form form, Resource resource, SlingHttpServletResponse response) throws IOException, JSONException {
        this.sendRedirect(form, resource, null, response);
    }

    @Override
    public void sendRedirect(Form form, String path, String formSelector, SlingHttpServletResponse response) throws IOException, JSONException {
        final String url = this.getRedirectPath(form, path, formSelector);
        response.sendRedirect(url);
    }

    @Override
    public void sendRedirect(Form form, Page page, String formSelector, SlingHttpServletResponse response) throws IOException, JSONException {
        final String url = this.getRedirectPath(form, page, formSelector);
        response.sendRedirect(url);
    }

    @Override
    public void sendRedirect(Form form, Resource resource, String formSelector, SlingHttpServletResponse response) throws IOException, JSONException {
        final String url = this.getRedirectPath(form, resource, formSelector);
        response.sendRedirect(url);
    }

    @Override
    public void renderForm(final Form form, final Page page, final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws IOException, ServletException, JSONException {
        this.sendRedirect(form, page, this.getFormSelector(request), response);
    }

    @Override
    public void renderForm(final Form form, final Resource resource, final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws IOException, ServletException, JSONException {
        this.sendRedirect(form, resource, this.getFormSelector(request), response);
    }

    @Override
    public void renderForm(final Form form, final String path, final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws IOException, ServletException, JSONException {
        this.sendRedirect(form, path, this.getFormSelector(request), response);
    }

    @Override
    public void renderOtherForm(Form form, String path, String formSelector, SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException, ServletException, JSONException {
        this.sendRedirect(form, path, formSelector, response);
    }

    @Override
    public void renderOtherForm(Form form, Page page, String formSelector, SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException, ServletException, JSONException {
        this.sendRedirect(form, page, formSelector, response);
    }

    @Override
    public void renderOtherForm(Form form, Resource resource, String formSelector, SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException, ServletException, JSONException {
        this.sendRedirect(form, resource, formSelector, response);
    }

    /**
     * Determines if this Form Manager should handle this request
     *
     * @param formName
     * @param request
     * @return
     */
    protected boolean doHandle(final String formName, final SlingHttpServletRequest request) {
        return this.doHandleGet(formName, request) || this.doHandlePost(formName, request);
    }

    /**
     * Checks if this Form Manager should handle this request as a GET request
     *
     * @param formName
     * @param request
     * @return
     */
    protected boolean doHandleGet(final String formName, final SlingHttpServletRequest request) {
        //noinspection SimplifiableIfStatement
        if (StringUtils.equalsIgnoreCase("GET", request.getMethod())) {
            return (StringUtils.isNotBlank(request.getParameter(this.getGetLookupKey(formName))));
        } else {
            return false;
        }
    }

    /**
     * Derives the form from the request's Query Parameters as best it can
     *
     * Falls back to an empty form if it runs into problems.
     * Fallback is due to ease of (inadvertent) tampering with query params
     *
     * @param formName
     * @param request
     * @return
     */
    @SuppressWarnings({"unchecked"})
    protected Form getGetForm(final String formName, final SlingHttpServletRequest request) {
        Map<String, String> data = new HashMap<String, String>();
        Map<String, String> errors = new HashMap<String, String>();

        // Get the QP lookup for this form
        final String requestData = this.decode(request.getParameter(this.getGetLookupKey(formName)));

        if(StringUtils.isBlank(requestData)) {
            return new Form(formName, request.getResource().getPath());
        }

        try {
            final JSONObject jsonData = new JSONObject(requestData);

            final String incomingFormName = jsonData.optString(KEY_FORM_NAME);

            // Double-check the form names; only inject matching forms
            if(StringUtils.equals(incomingFormName, formName)) {
                final JSONObject incomingJsonForm = jsonData.optJSONObject(KEY_FORM);
                if(incomingJsonForm != null) {
                    data = TypeUtil.toMap(incomingJsonForm);
                    log.debug("Form data: {}", data);
                }

                final JSONObject incomingJsonErrors = jsonData.optJSONObject(KEY_ERRORS);

                if(incomingJsonErrors != null) {
                    errors = TypeUtil.toMap(incomingJsonErrors);
                    log.debug("Form data: {}", errors);
                }
            }
        } catch (JSONException e) {
            log.warn("Cannot parse query parameters for request: {}", requestData);
            return new Form(formName, request.getResource().getPath());
        }

        return new Form(formName,
                request.getResource().getPath(),
                this.getProtectedData(data),
                this.getProtectedErrors(errors));
    }

    /**
     * Returns the Query Parameter name for this form
     *
     * @param formName
     * @return
     */
    protected String getGetLookupKey(final String formName) {
        return KEY_PREFIX_FORM_NAME + formName;
    }

    /**
     * Created the URL to the failure page with re-population info and error info
     *
     * @param form
     * @param page
     * @return
     * @throws org.apache.sling.commons.json.JSONException
     * @throws java.io.UnsupportedEncodingException
     */
    protected String getRedirectPath(final Form form, final Page page, final String formSelector) throws JSONException,
            UnsupportedEncodingException {
        return getRedirectPath(form, page.adaptTo(Resource.class), formSelector);
    }

    /**
     * Created the URL to the failure page with re-population info and error info
     *
     * @param form
     * @param resource
     * @return
     * @throws org.apache.sling.commons.json.JSONException
     * @throws java.io.UnsupportedEncodingException
     */
    protected String getRedirectPath(final Form form, final Resource resource, final String formSelector) throws JSONException,
            UnsupportedEncodingException {
        return getRedirectPath(form, resource.getPath() + FormHelper.EXTENSION, formSelector);
    }

    /**
     * Created the URL to the failure page with re-population info and error info
     *
     * @param form
     * @param path
     * @return
     * @throws org.apache.sling.commons.json.JSONException
     */
    protected String getRedirectPath(final Form form, final String path, final String formSelector) throws JSONException {
        String redirectPath = path;
        redirectPath += this.getSuffix();
        redirectPath += "?";
        redirectPath += this.getQueryParameters(form, formSelector);
        return redirectPath;
    }

    /**
     * *
     * Returns the a string of query parameters that hold Form and Form Error
     * data
     *
     * @return
     * @throws org.apache.sling.commons.json.JSONException
     */
    protected String getQueryParameters(Form form, final String formSelector) throws JSONException {
        boolean hasData = false;
        boolean hasFormSelector = StringUtils.isNotBlank(formSelector);
        final JSONObject jsonData = new JSONObject();

        String params = "";
        form = this.clean(form);

        jsonData.put(KEY_FORM_NAME, form.getName());

        if(form.hasData()) {
            final JSONObject jsonForm = new JSONObject(form.getData());
            jsonData.put(KEY_FORM, jsonForm);
            hasData = true;
        }

        if(form.hasErrors()) {
            final JSONObject jsonError = new JSONObject(form.getErrors());
            jsonData.put(KEY_ERRORS, jsonError);
            hasData = true;
        }

        if(hasData) {
            params = this.getGetLookupKey(form.getName());
            params += "=";
            params += this.encode(jsonData.toString());

            if(hasFormSelector) {
                params += "&";
                params += QUERY_PARAM_FORM_SELECTOR;
                params += "=";
                params += this.encode(formSelector);
            }
        }

        return params;
    }
}