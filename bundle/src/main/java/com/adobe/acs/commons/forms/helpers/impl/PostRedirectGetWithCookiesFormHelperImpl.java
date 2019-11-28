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
import com.adobe.acs.commons.forms.helpers.PostRedirectGetWithCookiesFormHelper;
import com.adobe.acs.commons.util.CookieUtil;
import com.day.cq.wcm.api.Page;
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

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * ACS AEM Commons - Forms - POST-Redirect-GET-With-Cookies Form Helper
 *
 */
@Component(inherit = true)
@Property(label = "Service Ranking",
        name = Constants.SERVICE_RANKING,
        intValue = FormHelper.SERVICE_RANKING_POST_REDIRECT_WITH_COOKIES_GET)
@Service(value = { FormHelper.class, PostRedirectGetWithCookiesFormHelper.class })
public class PostRedirectGetWithCookiesFormHelperImpl extends PostRedirectGetFormHelperImpl implements PostRedirectGetWithCookiesFormHelper {
    private static final Logger log = LoggerFactory.getLogger(PostRedirectGetWithCookiesFormHelperImpl.class);

    public static final int COOKIE_MAX_AGE = 10 * 60;
    public static final String ROOT_COOKIE_PATH = "/";

    @Override
    public final void sendRedirect(Form form, String path, String formSelector, SlingHttpServletResponse response)
            throws IOException {
        final String url = this.getRedirectPath(form, path, formSelector);
        addFlashCookie(response, form);
        response.sendRedirect(url);
    }

    @Override
    public final void sendRedirect(Form form, Page page, String formSelector, SlingHttpServletResponse response)
            throws IOException {
        final String url = this.getRedirectPath(form, page, formSelector);
        addFlashCookie(response, form);
        response.sendRedirect(url);
    }

    @Override
    public final void sendRedirect(Form form, Resource resource, String formSelector,
            SlingHttpServletResponse response) throws IOException {
        final String url = this.getRedirectPath(form, resource, formSelector);
        addFlashCookie(response, form);
        response.sendRedirect(url);
    }

    @Override
    protected final boolean doHandleGet(final String formName, final SlingHttpServletRequest request) {
        //noinspection SimplifiableIfStatement
        if (StringUtils.equalsIgnoreCase("GET", request.getMethod())) {
            return (CookieUtil.getCookie(request, this.getGetLookupKey(formName)) != null);
        } else {
            return false;
        }
    }

    @Override
    protected String getRawFormData(final String formName, final SlingHttpServletRequest request,
            final SlingHttpServletResponse response) {
        final String cookieName = getGetLookupKey(formName);
        final Cookie cookie = CookieUtil.getCookie(request, cookieName);

        String data = "";
        if (response != null && cookie != null) {
            CookieUtil.dropCookies(request, response, ROOT_COOKIE_PATH, cookieName);
            // Get the QP lookup for this form
            data = this.decode(cookie.getValue());
        } else {
            log.warn("SlingHttpServletResponse required for removing cookie. Please use formHelper.getForm({}, slingRequest, slingResponse);", formName);
        }
        return data;
    }

    @Override
    protected final String getRedirectPath(final Form form, final String path, final String formSelector) {
        String redirectPath = path;
        redirectPath += this.getSuffix();
        if (StringUtils.isNotBlank(formSelector)) {
            redirectPath += "/" + formSelector;
        }
        return redirectPath;
    }

    /**
     * Encodes URL data, escaping characters such as "+" and "="
     *
     * @param unencoded
     * @return
     */
    @Override
    protected final String encode(String unencoded) {
        try {
            return StringUtils.isBlank(unencoded) ? "" : URLEncoder.encode(unencoded, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            log.error("Cannot encode '{}' to UTF-8", unencoded, ex);
            return unencoded;
        }
    }

    /**
     * Decodes URL data.
     *
     * @param encoded
     * @return
     */
    @Override
    protected final String decode(String encoded) {
        try {
            return StringUtils.isBlank(encoded) ? "" : URLDecoder.decode(encoded, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            log.error("Cannot decode '{}'", ex);
            return encoded;
        }
    }

    /**
     * Adds a cookie containing the serialised contents of the form to a cookie named after the GET lookup key
     * @param response
     * @param form
     * @throws JSONException
     */
    protected void addFlashCookie(SlingHttpServletResponse response, Form form) {
        final String name = this.getGetLookupKey(form.getName());
        final String value = getQueryParameterValue(form);
        final Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(COOKIE_MAX_AGE);
        CookieUtil.addCookie(cookie, response);
    }

}