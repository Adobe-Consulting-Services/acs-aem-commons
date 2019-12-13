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
import com.day.cq.commons.PathInfo;
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

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PostRedirectGetFormHelperImplTest {

    public static final String RESOURCE_PATH = "/test";
    private static final String SUFFIX = "/submit/form";
    private static final Map<String, Object> ROUTER_PROPS = ImmutableMap.<String, Object>of("suffix", SUFFIX);

    @Rule
    public SlingContext slingContext = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);
    @Mock
    private XSSAPI xss;

    private MockSlingHttpServletRequest request;
    private MockSlingHttpServletResponse response;
    private Resource requestResource;
    private FormHelper formHelper;

    private ArgumentCaptor<String> redirectCaptor;

    @Before
    public void setup() throws LoginException, PersistenceException {
        // force resource resolver creation
        slingContext.resourceResolver();
        slingContext.registerService(XSSAPI.class, xss, new Hashtable<String, Object>());
        slingContext.registerService(FormsRouter.class, new FormsRouterImpl(), ROUTER_PROPS);
        formHelper = slingContext.registerInjectActivateService(new PostRedirectGetFormHelperImpl());

        request = slingContext.request();
        response = slingContext.response();
        requestResource = slingContext.create().resource(RESOURCE_PATH, Collections.<String, Object>emptyMap());
        request.setResource(requestResource);
        request.setMethod("POST");

        redirectCaptor = ArgumentCaptor.forClass(String.class);
    }

    @Test
    public void shouldMapFormName() throws Exception {
        request.setParameterMap(ImmutableMap.<String, Object>of(":form", "demo"));

        final Form form = formHelper.getForm("demo", request, response);

        assertThat(form.getName(), is(equalTo("demo")));
    }

    @Test
    public void shouldMapSingleParam() throws Exception {
        request.setParameterMap(ImmutableMap.<String, Object>of(":form", "x", "hello", "world"));

        final Form form = formHelper.getForm("x", request, response);

        assertThat(form.get("hello"), is(equalTo("world")));
    }

    @Test
    public void shouldMapFirstValueOfMultiParams() throws Exception {
        request.setParameterMap(ImmutableMap.<String, Object>of(":form", "x", "hello", new String[]{"world", "mundo"}));

        final Form form = formHelper.getForm("x", request, response);

        assertThat(form.get("hello"), is(equalTo("world")));
    }

    @Test
    public void shouldRedirectForRenderForm() throws Exception {
        request.setParameterMap(ImmutableMap.<String, Object>of(":form", "x"));
        final SlingHttpServletResponse spiedResponse = spy(response);

        final Form form = formHelper.getForm("x", request, response);
        formHelper.renderForm(form, requestResource, request, spiedResponse);

        // check a redirect was requested
        verify(spiedResponse).sendRedirect(redirectCaptor.capture());
        PathInfo redirectPathInfo = new PathInfo(redirectCaptor.getValue());

        // check where we are being redirected to
        assertThat(redirectPathInfo.getExtension(), is(equalTo("html")));
        assertThat(redirectPathInfo.getResourcePath(), is(equalTo("/test")));
        assertThat(redirectPathInfo.getSuffix(), startsWith(SUFFIX));
    }

    @Test
    public void shouldSerialiseFormInRedirectUrlParameter() throws Exception {
        request.setParameterMap(ImmutableMap.<String, Object>of(":form", "x", "hello", "world"));
        final SlingHttpServletResponse spiedResponse = spy(response);

        final Form form = formHelper.getForm("x", request, response);
        form.setError("hello", "Error1");
        formHelper.renderForm(form, requestResource, request, spiedResponse);

        verify(spiedResponse).sendRedirect(redirectCaptor.capture());
        String redirectLocation = redirectCaptor.getValue();

        final String formString = decode(getFormParameter(redirectLocation));
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

    private JSONObject toJSONObject(String form) throws JSONException {
        return new JSONObject(form);
    }

    private String decode(final String encodedForm) throws UnsupportedEncodingException {
        return java.net.URLDecoder.decode(encodedForm, "UTF-8");
    }

    private String getFormParameter(String redirectLocation) {
        final Pattern p = Pattern.compile(".*[\\?|&]f_x=([^=&]*)");
        final Matcher matcher = p.matcher(redirectLocation);
        return matcher.matches() ? matcher.group(1) : null;
    }

}