/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.adobe.acs.commons.forms.helpers.impl;

import com.adobe.acs.commons.forms.Form;
import com.adobe.acs.commons.forms.helpers.FormHelper;
import com.adobe.acs.commons.forms.helpers.PostFormHelper;
import com.adobe.acs.commons.util.PathInfoUtil;
import com.adobe.granite.xss.XSSAPI;
import com.day.cq.wcm.api.Page;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

@Component(label = "ACS AEM Commons - Abstract POST Form Helper", description = "Abstract Form Helper. Do not use directly; instead use the PostRedirectGetFormHelper or ForwardAsGetFormHelper.", enabled = true, metatype = false, immediate = false)
@Service(value = PostFormHelper.class)
public class PostFormHelperImpl implements PostFormHelper {
    private static final Logger log = LoggerFactory.getLogger(PostFormHelperImpl.class);
    static final String[] FORM_INPUTS = { FORM_NAME_INPUT, FORM_RESOURCE_INPUT };

    @Reference
    protected ResourceResolverFactory resourceResolverFactory;

    @Reference
    protected XSSAPI xssApi;

    /**
     * OSGi Properties *
     */
    private static final String DEFAULT_SUFFIX = "/submit/form";
    private String suffix = DEFAULT_SUFFIX;
    @Property(label = "Suffix", description = "Forward-as-GET Request Suffix used to identify Forward-as-GET POST Request", value = DEFAULT_SUFFIX)
    private static final String PROP_SUFFIX = "prop.form-suffix";

    @Override
    public Form getForm(String formName, SlingHttpServletRequest request) {
        throw new UnsupportedOperationException("Do not call AbstractFormHelper.getForm(..) direct. This is an abstract service.");
    }

    @Override
	public String getFormInputsHTML(final Form form, final String... keys) {
        // The form objects data and errors should be xssProtected before being passed into this method
		StringBuffer html = new StringBuffer();

        html.append("<input type=\"hidden\" name=\"").append(FORM_NAME_INPUT).append("\" value=\"")
                .append(xssApi.encodeForHTMLAttr(form.getName())).append("\"/>\n");

        final String resourcePath = form.getResourcePath();
        html.append("<input type=\"hidden\" name=\"").append(FORM_RESOURCE_INPUT).append("\" value=\"")
                .append(xssApi.encodeForHTMLAttr(resourcePath)).append("\"/>\n");

		for (final String key : keys) {
			if (form.has(key)) {
				html.append("<input type=\"hidden\" name=\"").append(key).append("\" value=\"")
						.append(form.get(key)).append("\"/>\n");
			}
		}

		return html.toString();
	}

    @Override
    public String getAction(final String path) {
        return getAction(path, null);
    }

    @Override
    public String getAction(final String path, final String formSelector) {
        String actionPath = path;

        ResourceResolver adminResourceResolver = null;
        try {
            adminResourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            actionPath = adminResourceResolver.map(path);
        } catch (LoginException e) {
            log.error("Could not attain an admin ResourceResolver to map the Form's Action URI");
            // Use the unmapped ActionPath
        } finally {
            if(adminResourceResolver != null && adminResourceResolver.isLive()) {
                adminResourceResolver.close();
            }
        }

        actionPath += FormHelper.EXTENSION + this.getSuffix();
        if(StringUtils.isNotBlank(formSelector)) {
            actionPath += "/" + formSelector;
        }

        return actionPath;
    }

    @Override
    public void renderForm(Form form, String path, SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException, ServletException, JSONException {
        throw new UnsupportedOperationException("Use a specific Forms implementation helper.");
    }

    @Override
    public void renderForm(Form form, Page page, SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException, ServletException, JSONException {
        throw new UnsupportedOperationException("Use a specific Forms implementation helper.");
    }

    @Override
    public void renderForm(Form form, Resource resource, SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException, ServletException, JSONException {
        throw new UnsupportedOperationException("Use a specific Forms implementation helper.");
    }

    @Override
    public void renderOtherForm(Form form, String path, String selectors, SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException, ServletException, JSONException {
        throw new UnsupportedOperationException("Use a specific Forms implementation helper.");
    }

    @Override
    public void renderOtherForm(Form form, Page page, String selectors, SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException, ServletException, JSONException {
        throw new UnsupportedOperationException("Use a specific Forms implementation helper.");
    }

    @Override
    public void renderOtherForm(Form form, Resource resource, String selectors, SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException, ServletException, JSONException {
        throw new UnsupportedOperationException("Use a specific Forms implementation helper.");
    }

    @Override
    public String getAction(final Resource resource) {
        return getAction(resource.getPath());
    }

    @Override
    public String getAction(final Resource resource, final String formSelector) {
        return getAction(resource.getPath(), formSelector);
    }

    @Override
    public String getAction(final Page page) {
        return getAction(page.getPath());
    }

    @Override
    public String getAction(final Page page, final String formSelector) {
        return getAction(page.getPath(), formSelector);
    }

    @Override
    public String getSuffix() {
        return this.suffix;
    }

    /**
     * Determines of this FormHelper should handle the POST request
     *
     * @param formName
     * @param request
     * @return
     */
    protected boolean doHandlePost(final String formName, final SlingHttpServletRequest request) {
        if(StringUtils.equalsIgnoreCase("POST", request.getMethod())) {
            // Form should have a hidden input with the name this.getLookupKey(..) and value formName
            return StringUtils.equals(formName, request.getParameter(this.getPostLookupKey(formName)));
        } else {
            return false;
        }
    }

    /**
     * Gets the Form from POST requests
     *
     * @param formName
     * @param request
     * @return
     */
    protected Form getPostForm(final String formName,
                            final SlingHttpServletRequest request) {
        final Map<String, String> map = new HashMap<String, String>();


        final RequestParameterMap requestMap = request.getRequestParameterMap();

        for (final String key : requestMap.keySet()) {
            // POST LookupKey formName param does not matter
            if(StringUtils.equals(key, this.getPostLookupKey(null))) { continue; }

            final RequestParameter[] values = requestMap.getValues(key);

            if (values == null || values.length == 0) {
                log.debug("Value did not exist for key: {}", key);
            } else if (values.length == 1) {
                log.debug("Adding to form data: {} ~> {}", key, values[0].toString());
                map.put(key, values[0].getString());
            } else {
                // TODO: Handle multi-value parameter values; Requires support for transporting them and re-writing them back into HTML Form on error
                for(final RequestParameter value : values) {
                    // Use the first non-blank value, or use the last value (which will be blank or not-blank)
                    final String tmp = value.toString();
                    map.put(key, tmp);

                    if(StringUtils.isNotBlank(tmp)) {
                        break;
                    }
                }
            }
        }

        return this.clean(new Form(formName, request.getResource().getPath(), map));
    }

    /**
     * Gets the Key used to look up the form during handling of POST requests
     *
     * @param formName
     * @return
     */
    protected String getPostLookupKey(final String formName) {
        // This may change; keeping as method call to ease future refactoring
        return FORM_NAME_INPUT;
    }

    /**
     * Removes unused Map entries from the provided map
     *
     * @param form
     * @return
     */
    protected Form clean(final Form form) {
        final Map<String, String> map = form.getData();
        final Map<String, String> cleanedMap = new HashMap<String, String>();

        for(final Map.Entry<String, String> entry : map.entrySet()) {
            if(!ArrayUtils.contains(FORM_INPUTS, entry.getKey()) && StringUtils.isNotBlank(entry.getValue())) {
                cleanedMap.put(entry.getKey(), entry.getValue());
            }
        }

        return new Form(form.getName(), form.getResourcePath(), cleanedMap, form.getErrors());
    }

    /**
     * Protect a Form in is entirety (data and errors)
     *
     * @param form
     * @return
     */
    protected Form getProtectedForm(final Form form) {
        return new Form(form.getName(),
                form.getResourcePath(),
                this.getProtectedData(form.getData()),
                this.getProtectedErrors(form.getErrors()));
    }

    /**
     * Protect a Map representing Form Data
     *
     * @param data
     * @return
     */
    protected Map<String, String> getProtectedData(final Map<String, String> data) {
        final Map<String, String> protectedData = new HashMap<String, String>();

        // Protect data for HTML Attributes
        for (final Map.Entry<String, String> entry : data.entrySet()) {
            protectedData.put(entry.getKey(), xssApi.encodeForHTMLAttr(entry.getValue()));
        }

        return protectedData;
    }

    /**
     * Protect a Map representing Form Errors
     *
     * @param errors
     * @return
     */
    protected Map<String, String> getProtectedErrors(final Map<String, String> errors) {
        final Map<String, String> protectedErrors = new HashMap<String, String>();

        // Protect data for HTML
        for (final Map.Entry<String, String> entry : errors.entrySet()) {
            protectedErrors.put(entry.getKey(), xssApi.encodeForHTML(entry.getValue()));
        }

        return protectedErrors;
    }


    public boolean hasValidSuffix(final SlingHttpServletRequest slingRequest) {
        final String requestSuffix = slingRequest.getRequestPathInfo().getSuffix();
        if(StringUtils.equals(requestSuffix, this.getSuffix()) ||
                StringUtils.startsWith(requestSuffix, this.getSuffix() + "/")) {
            return true;
        }

        return false;
    }

    /**
     * Gets the Form Selector for the form POST request
     *
     * @param slingRequest
     * @return
     */
    public String getFormSelector(final SlingHttpServletRequest slingRequest) {
        final String requestSuffix = slingRequest.getRequestPathInfo().getSuffix();
        if(StringUtils.equals(requestSuffix, this.getSuffix()) ||
                !StringUtils.startsWith(requestSuffix, this.getSuffix() + "/")) {
            return null;
        }

        final int segments = StringUtils.split(this.getSuffix(), '/').length;
        if(segments < 1) {
            return null;
        }

        final String formSelector = PathInfoUtil.getSuffixSegment(slingRequest, segments);
        return StringUtils.stripToNull(formSelector);
    }

    /**
     * Encodes URL data
     *
     * @param unencoded
     * @return
     */
    protected String encode(String unencoded) {
        if(StringUtils.isBlank(unencoded)) {
            return "";
        }

        try {
            return java.net.URLEncoder.encode(unencoded, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return unencoded;
        }
    }

    /**
     * Decodes URL data
     *
     * @param encoded
     * @return
     */
    protected String decode(String encoded) {
        if(StringUtils.isBlank(encoded)) {
            return "";
        }

        try {
            return java.net.URLDecoder.decode((encoded), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return encoded;
        }
    }

    @Activate
    protected void activate(final Map<String, String> properties) {
        this.suffix = PropertiesUtil.toString(properties.get(PROP_SUFFIX), DEFAULT_SUFFIX);
        if(StringUtils.isBlank(this.suffix)) {
            // No whitespace please
            this.suffix = DEFAULT_SUFFIX;
        }
    }
}