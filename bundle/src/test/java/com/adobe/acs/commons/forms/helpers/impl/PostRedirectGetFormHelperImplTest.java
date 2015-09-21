package com.adobe.acs.commons.forms.helpers.impl;

import com.adobe.acs.commons.forms.Form;
import com.adobe.acs.commons.forms.FormsRouter;
import com.adobe.acs.commons.forms.helpers.FormHelper;
import com.adobe.acs.commons.forms.impl.FormsRouterImpl;
import com.adobe.granite.xss.XSSAPI;
import com.google.common.collect.ImmutableMap;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Hashtable;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PostRedirectGetFormHelperImplTest {

    @Rule
    public SlingContext slingContext = new SlingContext();
    @Mock
    private XSSAPI xss;

    private MockSlingHttpServletRequest request;
    private MockSlingHttpServletResponse response;
    private Resource requestResource;
    private FormHelper formHelper;

    @Before
    public void setup() throws LoginException, PersistenceException {
        slingContext.registerService(XSSAPI.class, xss, new Hashtable<String, Object>());
        slingContext.registerService(FormsRouter.class, new FormsRouterImpl());
        formHelper = slingContext.registerInjectActivateService(new PostRedirectGetFormHelperImpl());

        request = slingContext.request();
        response = slingContext.response();
        requestResource = slingContext.create().resource("/test", Collections.<String, Object>emptyMap());
        request.setResource(requestResource);
        request.setMethod("POST");
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
    public void shouldRedirectOnRenderForm() throws Exception {
        request.setParameterMap(ImmutableMap.<String, Object>of(":form", "x", "hello", new String[]{"world", "mundo"}));
        final SlingHttpServletResponse spiedResponse = spy(response);

        final Form form = formHelper.getForm("x", request, response);
        form.setError("hello", "Error1");
        formHelper.renderForm(form, requestResource, request, spiedResponse);

        verify(spiedResponse).sendRedirect(anyString());
    }

}