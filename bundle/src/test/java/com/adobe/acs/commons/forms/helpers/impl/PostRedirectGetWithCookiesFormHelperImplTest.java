/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
import com.adobe.acs.commons.forms.impl.FormsRouterImpl;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.xss.XSSAPI;
import com.google.common.collect.ImmutableMap;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.Cookie;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PostRedirectGetWithCookiesFormHelperImplTest {

    public static final String RESOURCE_PATH = "/test";

    @Rule
    public SlingContext slingContext = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);
    @Mock
    private XSSAPI xss;

    private MockSlingHttpServletRequest request;
    private MockSlingHttpServletResponse response;
    private Resource requestResource;
    private FormHelper formHelper;

    private ArgumentCaptor<Cookie> cookieCaptor;

    @Before
    public void setup() throws LoginException, PersistenceException {
        // force resource resolver creation
        slingContext.resourceResolver();

        slingContext.registerService(XSSAPI.class, xss);
        slingContext.registerService(FormsRouter.class, new FormsRouterImpl());

        formHelper = slingContext.registerInjectActivateService(new PostRedirectGetWithCookiesFormHelperImpl());

        request = slingContext.request();
        response = slingContext.response();
        requestResource = slingContext.create().resource(RESOURCE_PATH, Collections.<String, Object>emptyMap());
        request.setResource(requestResource);
        cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
    }

    @Test
    public void shouldSerialiseFormInRedirectUrlParameter() throws Exception {
        request.setMethod("POST");
        request.setParameterMap(ImmutableMap.<String, Object>of(":form", "x", "hello", "world"));
        final SlingHttpServletResponse spiedResponse = spy(response);

        final Form form = formHelper.getForm("x", request, response);
        form.setError("hello", "Error1");

        formHelper.renderForm(form, requestResource, request, spiedResponse);

        verify(spiedResponse).addCookie(cookieCaptor.capture());
        final Cookie formCookie = cookieCaptor.getValue();

        final String formString = decode(formCookie.getValue());
        final JSONObject formObject = toJSONObject(formString);

        // form name
        assertThat(formObject.getString(PostRedirectGetFormHelperImpl.KEY_FORM_NAME), is(equalTo("x")));

        // errors
        final JSONObject formErrors = formObject.getJSONObject(PostRedirectGetFormHelperImpl.KEY_ERRORS);
        assertThat(formErrors.getString("hello"), is(equalTo("Error1")));

        // form data
        final JSONObject formData = formObject.getJSONObject(PostRedirectGetFormHelperImpl.KEY_FORM);
        assertThat(formData.getString("hello"), is(equalTo("world")));
    }


    @Test
    public void shouldInvalidateCookieOnGetRequest() throws Exception {
        final SlingHttpServletResponse redirectResponse = spy(slingContext.response());
        request.setMethod("GET");
        request.addCookie(new Cookie("f_x", encode("{\"n\":\"x\", \"f\": {\"hello\": \"world\"}}")));

        formHelper.getForm("x", request, redirectResponse);

        // check we a cookie was set
        verify(redirectResponse).addCookie(cookieCaptor.capture());
        // and that is
        assertThat(cookieCaptor.getValue().getMaxAge(), is(0));
    }

    @Test
    public void shouldRemoveCookieDataOnGetRequest() throws Exception {
        final SlingHttpServletResponse redirectResponse = spy(slingContext.response());
        request.setMethod("GET");
        request.addCookie(new Cookie("f_x", encode("{\"n\":\"x\", \"f\": {\"hello\": \"world\"}}")));

        formHelper.getForm("x", request, redirectResponse);

        // check we a cookie was set
        verify(redirectResponse).addCookie(cookieCaptor.capture());
        // and that is
        assertThat(cookieCaptor.getValue().getMaxAge(), is(0));
    }

    private JSONObject toJSONObject(String form) throws JSONException {
        return new JSONObject(form);
    }

    private String decode(final String encodedForm) {
        return org.apache.sling.commons.json.http.Cookie.unescape(encodedForm);
    }

    private String encode(final String unencoded) {
        return org.apache.sling.commons.json.http.Cookie.escape(unencoded);
    }
}