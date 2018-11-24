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

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;

import java.io.IOException;

@ProviderType
@SuppressWarnings("squid:S1214")
public interface PostRedirectGetFormHelper extends FormHelper {
    String KEY_FORM_NAME = "n";
    String KEY_FORM = "f";
    String KEY_ERRORS = "e";
    String KEY_PREFIX_FORM_NAME = "f_";
    String QUERY_PARAM_FORM_SELECTOR = "f_selector";

    /**
     * Issues a 302 redirect with the form serialized into a JSON object that can be
     * read out by the PostRedirectGetFormHelper on the "other side".
     *
     * Allows 302 redirect to target the specified path.
     *
     * @param form
     * @param path
     * @param response
     * @throws IOException
     */
    void sendRedirect(Form form, String path, SlingHttpServletResponse response) throws IOException;

    /**
     * Issues a 302 redirect with the form serialized into a JSON object that can be
     * read out by the PostRedirectGetFormHelper on the "other side".
     *
     * Allows 302 redirect to target the specified CQ Page.
     *
     * @param form
     * @param page
     * @param response
     * @throws IOException
     */
    void sendRedirect(Form form, Page page, SlingHttpServletResponse response) throws IOException;

    /**
     /**
     * Issues a 302 redirect with the form serialized into a JSON object that can be
     * read out by the PostRedirectGetFormHelper on the "other side".
     *
     * Allows 302 redirect to target the specified resource with provided .html extension.
     *
     * @param form
     * @param resource
     * @param response
     * @throws IOException
     */
    void sendRedirect(Form form, Resource resource, SlingHttpServletResponse response) throws IOException;


    /**
     * Same as:
     *
     *      sendRedirect(Form form, String path, SlingHttpServletResponse response)
     *
     * but adds the Form selector query parameter to redirect request.
     *
     * @param form
     * @param path
     * @param formSelector
     * @param response
     * @throws IOException
     */
    void sendRedirect(Form form, String path, String formSelector, SlingHttpServletResponse response) throws IOException;

    /**
     * Same as:
     *
     *      sendRedirect(Form form, Page page, SlingHttpServletResponse response)
     *
     * but adds the Form selector query parameter to redirect request.
     *
     * @param form
     * @param page
     * @param formSelector
     * @param response
     * @throws IOException
     */
    void sendRedirect(Form form, Page page, String formSelector, SlingHttpServletResponse response) throws IOException;

    /**
     * Same as:
     *
     *      sendRedirect(Form form, Resource resource, SlingHttpServletResponse response)
     *
     * but adds the Form selector query parameter to redirect request.
     *
     * @param form
     * @param resource
     * @param formSelector
     * @param response
     * @throws IOException
     */
    void sendRedirect(Form form, Resource resource, String formSelector, SlingHttpServletResponse response) throws IOException;
}