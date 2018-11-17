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
import com.adobe.acs.commons.forms.FormsRouter;
import com.adobe.acs.commons.forms.helpers.FormHelper;
import com.adobe.acs.commons.forms.impl.FormImpl;
import org.apache.sling.xss.XSSAPI;
import com.day.cq.wcm.api.Page;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * ACS AEM Commons - Forms - Abstract Form Helper
 * Abstract Form Helper. This provides common behaviors for handling POST-behaviors of the
 *  ACS AEM Commons Forms implementation.
 */
@Component(componentAbstract = true)
@Property(name = Constants.SERVICE_RANKING, intValue = FormHelper.SERVICE_RANKING_BASE)
public abstract class AbstractFormHelperImpl {
    private static final Logger log = LoggerFactory.getLogger(AbstractFormHelperImpl.class);

    static final String[] FORM_INPUTS = {FormHelper.FORM_NAME_INPUT, FormHelper.FORM_RESOURCE_INPUT};

    private static final String SERVICE_NAME = "form-helper";
    private static final Map<String, Object> AUTH_INFO;

    static {
        AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
    }

    @Reference
    private FormsRouter formsRouter;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private XSSAPI xss;

    public final String getFormInputsHTML(final Form form, final String... keys) {
        // The form objects data and errors should be xssProtected before being passed into this method
        StringBuilder html = new StringBuilder();

        appendHiddenTag(html, FormHelper.FORM_NAME_INPUT, form.getName());

        final String resourcePath = form.getResourcePath();
        appendHiddenTag(html, FormHelper.FORM_RESOURCE_INPUT, resourcePath);

        for (final String key : keys) {
            if (form.has(key)) {
                appendHiddenTag(html, key, form.get(key));
            }
        }

        return html.toString();
    }

    private void appendHiddenTag(StringBuilder html, String name, String value) {
        html.append("<input type=\"hidden\" name=\"").append(name).append("\" value=\"")
                .append(xss.encodeForHTMLAttr(value)).append("\"/>\n");
    }

    public final String getAction(final Page page) {
        return this.getAction(page.getPath());
    }

    public final String getAction(final Resource resource) {
        return this.getAction(resource.getPath());
    }

    public final String getAction(final String path) {
        return getAction(path, null);
    }

    public final String getAction(final Page page, final String formSelector) {
        return this.getAction(page.getPath(), formSelector);
    }

    public final String getAction(final Resource resource, final String formSelector) {
        return this.getAction(resource.getPath(), formSelector);
    }

    public final String getAction(final String path, final String formSelector) {
        String actionPath = path;

        try (ResourceResolver serviceResourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)){
            actionPath = serviceResourceResolver.map(path);
        } catch (LoginException e) {
            log.error("Could not attain an admin ResourceResolver to map the Form's Action URI");
            // Use the unmapped ActionPath
        }

        actionPath += FormHelper.EXTENSION + this.getSuffix();
        if (StringUtils.isNotBlank(formSelector)) {
            actionPath += "/" + formSelector;
        }

        return actionPath;
    }

    public final String getSuffix() {
        return formsRouter.getSuffix();
    }

    /**
     * Determines of this FormHelper should handle the POST request.
     *
     * @param formName
     * @param request
     * @return
     */
    protected final boolean doHandlePost(final String formName, final SlingHttpServletRequest request) {
        if (StringUtils.equalsIgnoreCase("POST", request.getMethod())) {
            // Form should have a hidden input with the name this.getLookupKey(..) and value formName
            return StringUtils.equals(formName, request.getParameter(this.getPostLookupKey(formName)));
        } else {
            return false;
        }
    }

    /**
     * Gets the Form from POST requests.
     *
     * @param formName
     * @param request
     * @return
     */
    protected final Form getPostForm(final String formName,
                               final SlingHttpServletRequest request) {
        final Map<String, String> map = new HashMap<String, String>();


        final RequestParameterMap requestMap = request.getRequestParameterMap();

        for (final String key : requestMap.keySet()) {
            // POST LookupKey formName param does not matter
            if (StringUtils.equals(key, this.getPostLookupKey(null))) {
                continue;
            }

            final RequestParameter[] values = requestMap.getValues(key);

            if (values == null || values.length == 0) {
                log.debug("Value did not exist for key: {}", key);
            } else if (values.length == 1) {
                log.debug("Adding to form data: {} ~> {}", key, values[0].toString());
                map.put(key, values[0].getString());
            } else {
                // Requires support for transporting them and re-writing them back into HTML Form on error
                for (final RequestParameter value : values) {
                    // Use the first non-blank value, or use the last value (which will be blank or not-blank)
                    final String tmp = value.toString();
                    map.put(key, tmp);

                    if (StringUtils.isNotBlank(tmp)) {
                        break;
                    }
                }
            }
        }

        return this.clean(new FormImpl(formName, request.getResource().getPath(), map));
    }

    /**
     * Gets the Key used to look up the form during handling of POST requests.
     *
     * @param formName
     * @return
     */
    @SuppressWarnings("squid:S1172")
    protected final String getPostLookupKey(final String formName) {
        // This may change; keeping as method call to ease future refactoring
        return FormHelper.FORM_NAME_INPUT;
    }

    /**
     * Removes unused Map entries from the provided map.
     *
     * @param form
     * @return
     */
    protected final Form clean(final Form form) {
        final Map<String, String> map = form.getData();
        final Map<String, String> cleanedMap = new HashMap<String, String>();

        for (final Map.Entry<String, String> entry : map.entrySet()) {
            if (!ArrayUtils.contains(FORM_INPUTS, entry.getKey()) && StringUtils.isNotBlank(entry.getValue())) {
                cleanedMap.put(entry.getKey(), entry.getValue());
            }
        }

        return new FormImpl(form.getName(), form.getResourcePath(), cleanedMap, form.getErrors());
    }

    /**
     * Protect a Form in is entirety (data and errors).
     *
     * @param form
     * @return
     */
    protected final Form getProtectedForm(final Form form) {
        return new FormImpl(form.getName(),
                form.getResourcePath(),
                this.getProtectedData(form.getData()),
                this.getProtectedErrors(form.getErrors()));
    }

    /**
     * Protect a Map representing Form Data.
     *
     * @param data
     * @return
     */
    protected final Map<String, String> getProtectedData(final Map<String, String> data) {
        final Map<String, String> protectedData = new HashMap<String, String>();

        // Protect data for HTML Attributes
        for (final Map.Entry<String, String> entry : data.entrySet()) {
            protectedData.put(entry.getKey(), xss.encodeForHTMLAttr(entry.getValue()));
        }

        return protectedData;
    }

    /**
     * Protect a Map representing Form Errors.
     *
     * @param errors
     * @return
     */
    protected final Map<String, String> getProtectedErrors(final Map<String, String> errors) {
        final Map<String, String> protectedErrors = new HashMap<String, String>();

        // Protect data for HTML
        for (final Map.Entry<String, String> entry : errors.entrySet()) {
            protectedErrors.put(entry.getKey(), xss.encodeForHTML(entry.getValue()));
        }

        return protectedErrors;
    }


    public final boolean hasValidSuffix(final SlingHttpServletRequest slingRequest) {
        return formsRouter.hasValidSuffix(slingRequest);
    }

    /**
     * Gets the Form Selector for the form POST request.
     *
     * @param slingRequest
     * @return
     */
    public final String getFormSelector(final SlingHttpServletRequest slingRequest) {
        return formsRouter.getFormSelector(slingRequest);
    }

    /**
     * Encodes URL data.
     *
     * @param unencoded
     * @return
     */
    protected String encode(String unencoded) {
        if (StringUtils.isBlank(unencoded)) {
            return "";
        }

        try {
            return java.net.URLEncoder.encode(unencoded, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return unencoded;
        }
    }

    /**
     * Decodes URL data.
     *
     * @param encoded
     * @return
     */
    protected String decode(String encoded) {
        if (StringUtils.isBlank(encoded)) {
            return "";
        }

        try {
            return java.net.URLDecoder.decode((encoded), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return encoded;
        }
    }
}