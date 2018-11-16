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
package com.adobe.acs.commons.forms.helpers;

import aQute.bnd.annotation.ProviderType;

import com.adobe.acs.commons.forms.Form;
import com.day.cq.wcm.api.Page;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;

import javax.servlet.ServletException;

import java.io.IOException;

@ProviderType
@SuppressWarnings("squid:S1214")
public interface FormHelper {
    String EXTENSION = ".html";

    String DEFAULT_FORM_SELECTOR = "post";
    String FORM_NAME_INPUT = ":form";
    String FORM_RESOURCE_INPUT = ":formResource";

    int SERVICE_RANKING_FORWARD_AS_GET = 1000;
    int SERVICE_RANKING_POST_REDIRECT_GET = 500;
    int SERVICE_RANKING_POST_REDIRECT_WITH_COOKIES_GET = 400;
    int SERVICE_RANKING_BASE = Integer.MIN_VALUE;

    /**
     * Gets the From from either the POST Requests parameters or the GET
     * request's (synthetic) attributes.
     *
     * @param formName
     * @param request
     * @return
     */
    Form getForm(String formName, SlingHttpServletRequest request);

    /**
     * Gets the From from either the POST Requests parameters or the GET
     * request's (synthetic) attributes.
     *
     * @param formName
     * @param request
     * @param response
     * @return
     */
    Form getForm(final String formName, final SlingHttpServletRequest request, final SlingHttpServletResponse response);

    /**
     * Returns a series of hidden fields used to persist multi-page form data
     * between forms.
     *
     * @param form
     * @param keys
     * @return
     * @throws java.io.IOException
     */
    String getFormInputsHTML(Form form, String... keys);

    /**
     * Gets the Form Selector for the form POST request.
     *
     * @param slingRequest
     * @return
     */
    String getFormSelector(final SlingHttpServletRequest slingRequest);

    /**
     * Builds the form's action URI based on the provided resource's path
     * <p>
     * Appends ".post.html" to the resource's path.
     *
     * @param resource
     * @return
     */
    String getAction(final Resource resource);

    /**
     * Builds the form's action URI based on the provided resource's path
     * <p>
     * Appends ".html/<suffix>" to the resource's path.
     *
     * @param resource
     * @param formSelector
     * @return
     */
    String getAction(final Resource resource, String formSelector);

    /**
     * Builds the form's action URI based on the provided page's path
     * <p>
     * Appends ".html/<suffix>/<formSelector>" to the page's path.
     *
     * @param page
     * @return
     */
    String getAction(final Page page);

    /**
     * Builds the form's action URI based on the provided page's path
     * <p>
     * Appends ".html/<suffix>/<formSelector>" to the page's path.
     *
     * @param page
     * @param formSelector
     * @return
     */
    String getAction(final Page page, String formSelector);

    /**
     * Builds the form's action URI based on the provided path
     * <p>
     * Appends ".html/<suffix>" to the path.
     *
     * @param path
     * @return
     */
    String getAction(final String path);

    /**
     * Builds the form's action URI based on the provided path
     * <p>
     * Appends ".html/<suffix>/<formSelector>" to the path.
     *
     * @param path
     * @param formSelector
     * @return
     */
    String getAction(final String path, final String formSelector);

    /**
     * Wrapped method to create a interface from FormHelper to normalize APIs that are commonly used.
     * <p>
     * - Wraps implementing FormHelper's `render(..)` method (sendRedirect or forwardAsGet)
     * in the implementing FormHelper
     *
     * @param form
     * @param path
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     * @throws JSONException
     */
    void renderForm(Form form, String path, SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException, ServletException;

    /**
     * Wrapped method to create a interface from FormHelper to normalize APIs that are commonly used.
     * <p>
     * - Wraps implementing FormHelper's `render(..)` method (sendRedirect or forwardAsGet)
     * in the implementing FormHelper
     *
     * @param form
     * @param page
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     * @throws JSONException
     */
    void renderForm(Form form, Page page, SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException, ServletException;

    /**
     * Wrapped method to create a interface from FormHelper to normalize APIs that are commonly used.
     * <p>
     * - Wraps implementing `.renderForm(..)` method (sendRedirect or forwardAsGet) in the implementing FormHelper
     *
     * @param form
     * @param resource
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     * @throws JSONException
     */
    void renderForm(Form form, Resource resource, SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException, ServletException;

    /**
     * Wrapped method to create a interface from FormHelper to normalize APIs that are commonly used.
     * <p>
     * - Wraps implementing FormHelper's `render(..)` method (sendRedirect or forwardAsGet)
     * in the implementing FormHelper
     *
     * @param form
     * @param path
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     * @throws JSONException
     */
    void renderOtherForm(Form form, String path, String selectors, SlingHttpServletRequest request,
                         SlingHttpServletResponse response)
            throws IOException, ServletException;

    /**
     * Wrapped method to create a interface from FormHelper to normalize APIs that are commonly used.
     * <p>
     * - Wraps implementing FormHelper's `render(..)` method (sendRedirect or forwardAsGet)
     * in the implementing FormHelper
     *
     * @param form
     * @param page
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     * @throws JSONException
     */
    void renderOtherForm(Form form, Page page, String selectors, SlingHttpServletRequest request,
                         SlingHttpServletResponse response)
            throws IOException, ServletException;


    /**
     * Wrapped method to create a interface from FormHelper to normalize APIs that are commonly used.
     * <p>
     * - Wraps implementing FormHelper's `render(..)` method (sendRedirect or forwardAsGet)
     * in the implementing FormHelper
     *
     * @param form
     * @param resource
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    void renderOtherForm(Form form, Resource resource, String selectors, SlingHttpServletRequest request,
                         SlingHttpServletResponse response)
            throws IOException, ServletException;

}