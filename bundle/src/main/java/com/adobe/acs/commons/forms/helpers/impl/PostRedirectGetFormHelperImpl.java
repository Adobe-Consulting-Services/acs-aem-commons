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
import com.adobe.acs.commons.forms.helpers.PostRedirectGetFormHelper;
import com.adobe.acs.commons.forms.impl.FormImpl;
import com.adobe.acs.commons.json.JsonObjectUtil;
import com.day.cq.wcm.api.Page;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Map;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.adobe.acs.commons.json.JsonObjectUtil.*;

/**
 * ACS AEM Commons - Forms - POST-Redirect-GET Form Helper
 *
 */
@Component(inherit = true)
@Property(label = "Service Ranking",
        name = Constants.SERVICE_RANKING,
        intValue = FormHelper.SERVICE_RANKING_POST_REDIRECT_GET)
@Service(value = {FormHelper.class, PostRedirectGetFormHelper.class})
public class PostRedirectGetFormHelperImpl extends AbstractFormHelperImpl implements PostRedirectGetFormHelper {

    private static final Logger log = LoggerFactory.getLogger(PostRedirectGetFormHelperImpl.class);

    @Override
    public final Form getForm(final String formName, final SlingHttpServletRequest request) {
        return getForm(formName, request, null);
    }

    @Override
    public final Form getForm(final String formName, final SlingHttpServletRequest request, final SlingHttpServletResponse response) {
        if (this.doHandlePost(formName, request)) {
            log.debug("Getting FORM [ {} ] from POST parameters", formName);
            return this.getPostForm(formName, request);
        } else if (this.doHandleGet(formName, request)) {
            log.debug("Getting FORM [ {} ] from GET parameters", formName);
            return this.getGetForm(formName, request, response);
        }

        log.debug("Creating empty form for FORM [ {} ]", formName);
        return new FormImpl(formName, request.getResource().getPath());
    }

    @Override
    public final void sendRedirect(Form form, String path, SlingHttpServletResponse response) throws IOException {
        this.sendRedirect(form, path, null, response);
    }

    @Override
    public final void sendRedirect(Form form, Page page, SlingHttpServletResponse response) throws IOException {
        this.sendRedirect(form, page, null, response);
    }

    @Override
    public final void sendRedirect(Form form, Resource resource, SlingHttpServletResponse response) throws
            IOException {
        this.sendRedirect(form, resource, null, response);
    }

    @Override
    public void sendRedirect(Form form, String path, String formSelector, SlingHttpServletResponse response)
            throws IOException {
        final String url = this.getRedirectPath(form, path, formSelector);
        response.sendRedirect(url);
    }

    @Override
    public void sendRedirect(Form form, Page page, String formSelector, SlingHttpServletResponse response)
            throws IOException {
        final String url = this.getRedirectPath(form, page, formSelector);
        response.sendRedirect(url);
    }

    @Override
    public void sendRedirect(Form form, Resource resource, String formSelector,
            SlingHttpServletResponse response) throws IOException {
        final String url = this.getRedirectPath(form, resource, formSelector);
        response.sendRedirect(url);
    }

    @Override
    public final void renderForm(final Form form, final Page page, final SlingHttpServletRequest request,
            final SlingHttpServletResponse response)
            throws IOException, ServletException {
        this.sendRedirect(form, page, this.getFormSelector(request), response);
    }

    @Override
    public final void renderForm(final Form form, final Resource resource, final SlingHttpServletRequest request,
            final SlingHttpServletResponse response)
            throws IOException, ServletException {
        this.sendRedirect(form, resource, this.getFormSelector(request), response);
    }

    @Override
    public final void renderForm(final Form form, final String path, final SlingHttpServletRequest request,
            final SlingHttpServletResponse response)
            throws IOException, ServletException {
        this.sendRedirect(form, path, this.getFormSelector(request), response);
    }

    @Override
    public final void renderOtherForm(Form form, String path, String formSelector, SlingHttpServletRequest request,
            SlingHttpServletResponse response)
            throws IOException, ServletException {
        this.sendRedirect(form, path, formSelector, response);
    }

    @Override
    public final void renderOtherForm(Form form, Page page, String formSelector, SlingHttpServletRequest request,
            SlingHttpServletResponse response)
            throws IOException, ServletException {
        this.sendRedirect(form, page, formSelector, response);
    }

    @Override
    public final void renderOtherForm(Form form, Resource resource, String formSelector,
            SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException, ServletException {
        this.sendRedirect(form, resource, formSelector, response);
    }

    /**
     * Determines if this Form Manager should handle this request.
     *
     * @param formName
     * @param request
     * @return
     */
    protected final boolean doHandle(final String formName, final SlingHttpServletRequest request) {
        return this.doHandleGet(formName, request) || this.doHandlePost(formName, request);
    }

    /**
     * Checks if this Form Manager should handle this request as a GET request.
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
     * <p>
     * Falls back to an empty form if it runs into problems. Fallback is due to
     * ease of (inadvertent) tampering with query params
     *
     * @param formName
     * @param request
     * @return
     */
    protected Form getGetForm(final String formName, final SlingHttpServletRequest request, final SlingHttpServletResponse response) {
        Map<String, String> data = Collections.EMPTY_MAP;
        Map<String, String> errors = Collections.EMPTY_MAP;
        final String requestData = getRawFormData(formName, request, response);

        if (StringUtils.isBlank(requestData)) {
            return new FormImpl(formName, request.getResource().getPath());
        }

        try {
            Gson gson = new Gson();
            final JsonObject jsonData = JsonObjectUtil.toJsonObject(requestData);

            final String incomingFormName = getString(jsonData, KEY_FORM_NAME);

            // Double-check the form names; only inject matching forms
            if (StringUtils.equals(incomingFormName, formName)) {
                data = getOptionalObject(jsonData, KEY_FORM)
                        .map(incomingJsonForm -> gson.fromJson(incomingJsonForm, Map.class))
                        .orElse(Collections.EMPTY_MAP);
                if (data.size() > 0) {
                    log.debug("Form data: {}", data);
                }

                errors = getOptionalObject(jsonData, KEY_ERRORS)
                        .map(incomingJsonForm -> gson.fromJson(incomingJsonForm, Map.class))
                        .orElse(Collections.EMPTY_MAP);
                if (errors.size() > 0) {
                    log.debug("Form errors: {}", data);
                }
            }
        } catch (JsonParseException e) {
            log.warn("Cannot parse query parameters for request: {}", requestData);
            return new FormImpl(formName, request.getResource().getPath());
        }

        return new FormImpl(formName,
                request.getResource().getPath(),
                this.getProtectedData(data),
                this.getProtectedErrors(errors));
    }

    protected String getRawFormData(final String formName, final SlingHttpServletRequest request,
            final SlingHttpServletResponse response) {
        // Get the QP lookup for this form
        return this.decode(request.getParameter(this.getGetLookupKey(formName)));
    }

    /**
     * Returns the Query Parameter name for this form.
     *
     * @param formName
     * @return
     */
    protected final String getGetLookupKey(final String formName) {
        return KEY_PREFIX_FORM_NAME + formName;
    }

    /**
     * Created the URL to the failure page with re-population info and error
     * info.
     *
     * @param form
     * @param page
     * @return
     *
     * @throws java.io.UnsupportedEncodingException
     *
     */
    protected final String getRedirectPath(final Form form, final Page page, final String formSelector) throws
            UnsupportedEncodingException {
        return getRedirectPath(form, page.adaptTo(Resource.class), formSelector);
    }

    /**
     * Created the URL to the failure page with re-population info and error
     * info.
     *
     * @param form
     * @param resource
     * @return
     *
     * @throws java.io.UnsupportedEncodingException
     *
     */
    protected final String getRedirectPath(final Form form, final Resource resource, final String formSelector) throws
            UnsupportedEncodingException {
        return getRedirectPath(form, resource.getPath() + FormHelper.EXTENSION, formSelector);
    }

    /**
     * Created the URL to the failure page with re-population info and error
     * info.
     *
     * @param form
     * @param path
     * @return
     *
     */
    protected String getRedirectPath(final Form form, final String path, final String formSelector) {
        String redirectPath = path;
        redirectPath += this.getSuffix();
        if (StringUtils.isNotBlank(formSelector)) {
            redirectPath += "/" + formSelector;
        }
        redirectPath += "?";
        redirectPath += this.getQueryParameters(form);
        return redirectPath;
    }

    /**
     * *
     * Returns the a string of query parameters that hold Form and Form Error.
     * data
     *
     * @return
     *
     */
    protected final String getQueryParameters(Form form) {
        String params = "";
        form = this.clean(form);

        if (form.hasData() || form.hasErrors()) {
            params = this.getGetLookupKey(form.getName());
            params += "=";
            params += getQueryParameterValue(form);
        }

        return params;
    }

    protected final String getQueryParameterValue(Form form) {
        boolean hasData = false;
        final JsonObject jsonData = new JsonObject();

        form = this.clean(form);

        jsonData.addProperty(KEY_FORM_NAME, form.getName());

        Gson gson = new Gson();

        if (form.hasData()) {
            final JsonObject jsonForm = (JsonObject) gson.toJsonTree(form.getData());
            jsonData.add(KEY_FORM, jsonForm);
            hasData = true;
        }

        if (form.hasErrors()) {
            final JsonObject jsonError = (JsonObject) gson.toJsonTree(form.getErrors());
            jsonData.add(KEY_ERRORS, jsonError);
            hasData = true;
        }

        return hasData ? this.encode(jsonData.toString()) : "";

    }
}
