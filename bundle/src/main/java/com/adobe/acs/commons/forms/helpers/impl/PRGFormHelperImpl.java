package com.adobe.acs.commons.forms.helpers.impl;

import com.adobe.acs.commons.forms.Form;
import com.adobe.acs.commons.forms.helpers.FormHelper;
import com.adobe.acs.commons.forms.helpers.PRGFormHelper;
import com.adobe.acs.commons.util.TypeUtil;
import com.day.cq.wcm.api.Page;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;


@Component(label = "ACS AEM Commons - PRG Form Helper", description = "POST-Redirect-GET Form Helper", enabled = true, metatype = false, immediate = false)
@Properties({ @Property(label = "Vendor", name = Constants.SERVICE_VENDOR, value = "ACS", propertyPrivate = true) })
@Service( value = { FormHelper.class, PRGFormHelper.class })
public class PRGFormHelperImpl extends AbstractFormHelperImpl implements PRGFormHelper {
	private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    /**
     * Checks if this Form Manager should handle this request
     *
     * @param formName
     * @param request
     * @return
     */
    protected boolean doHandle(final String formName, final HttpServletRequest request) {
        return this.doHandleGet(formName, request) || this.doHandlePost(formName, request);
    }

    /**
     *
     * @param formName
     * @param request
     * @return
     */
    @Override
    public Form getForm(final String formName, final HttpServletRequest request) {
		if (this.doHandlePost(formName, request)) {
			log.debug("Getting FORM {} from POST parameters", formName);
			return this.getGetForm(formName, request);
		} else if(this.doHandleGet(formName, request)) {
            log.debug("Getting FORM {} from GET parameters", formName);
            return this.getPostForm(formName, request);
        }

        log.debug("Creating empty form for FORM {}", formName);
        return new Form(formName);
	}

    /**
     *
     * @param form
     * @param path
     * @param response
     * @throws IOException
     * @throws JSONException
     */
    @Override
    public void sendRedirect(Form form, String path, HttpServletResponse response) throws IOException, JSONException {
        final String url = this.getRedirectPath(form, path);
        response.sendRedirect(url);
    }

    /**
     *
     * @param form
     * @param page
     * @param response
     * @throws IOException
     * @throws JSONException
     */
    @Override
    public void sendRedirect(Form form, Page page, HttpServletResponse response) throws IOException, JSONException {
        final String url = this.getRedirectPath(form, page);
        response.sendRedirect(url);
    }

    /**
     *
     * @param form
     * @param resource
     * @param response
     * @throws IOException
     * @throws JSONException
     */
    @Override
    public void sendRedirect(Form form, Resource resource, HttpServletResponse response) throws IOException, JSONException {
        final String url = this.getRedirectPath(form, resource);
        response.sendRedirect(url);
    }

    /**
     * Checks if this Form Manager should handle this request
     *
     * @param formName
     * @param request
     * @return
     */
    protected boolean doHandleGet(final String formName, final HttpServletRequest request) {
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
     * Fallback is due to ease of (inadvertant) tampering with query params
     *
     * @param formName
     * @param request
     * @return
     */
    protected Form getGetForm(final String formName, final HttpServletRequest request) {
        Map<String, String> form = new HashMap<String, String>();
        Map<String, String> errors = new HashMap<String, String>();

        // Get the QP lookup for this form
        final String data = this.decode(request.getParameter(this.getGetLookupKey(formName)));

        if(StringUtils.isBlank(data)) {
            return new Form(formName);
        }

        try {
            final JSONObject jsonData = new JSONObject(data);

            final String incomingFormName = jsonData.optString(KEY_FORM_NAME);

            // Double-check the form names; only inject matching forms
            if(StringUtils.equals(incomingFormName, formName)) {
                final JSONObject incomingJsonForm = jsonData.optJSONObject(KEY_FORM);
                if(incomingJsonForm != null) {
                    form = TypeUtil.toMap(incomingJsonForm);
                }

                final JSONObject incomingJsonErrors = jsonData.optJSONObject(KEY_ERRORS);

                if(incomingJsonErrors != null) {
                    errors = TypeUtil.toMap(incomingJsonErrors);
                }
            }
        } catch (JSONException e) {
            log.warn("Cannot parse query parameters for request: {}", data);
            return new Form(formName);
        }

        return new Form(formName, form, errors);
    }

    /**
     * Returns the Query Parameter name for this form
     *
     * @param formName
     * @return
     */
    protected String getGetLookupKey(final String formName) {
        return "form_" + formName;
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
    public String getRedirectPath(Form form, Page page) throws JSONException,
            UnsupportedEncodingException {
        return getRedirectPath(form, page.adaptTo(Resource.class));
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
    public String getRedirectPath(Form form, Resource resource) throws JSONException,
            UnsupportedEncodingException {
        return getRedirectPath(form, resource.getPath().concat(".html"));
    }

    /**
     * Created the URL to the failure page with re-population info and error info
     *
     * @param form
     * @param path
     * @return
     * @throws org.apache.sling.commons.json.JSONException
     * @throws java.io.UnsupportedEncodingException
     */
    public String getRedirectPath(Form form, String path) throws JSONException,
            UnsupportedEncodingException {
        return path.concat("?").concat(this.getQueryParameters(form));
    }

    /**
     * *
     * Returns the a string of query parameters that hold Form and Form Error
     * data
     *
     * @return
     * @throws org.apache.sling.commons.json.JSONException
     * @throws java.io.UnsupportedEncodingException
     */
    protected String getQueryParameters(Form form) throws UnsupportedEncodingException, JSONException {
        boolean hasData = false;
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
        }

        return params;
    }
}