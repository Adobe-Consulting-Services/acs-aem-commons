package com.adobe.acs.commons.forms.helpers.impl;

import com.adobe.acs.commons.forms.Form;
import com.adobe.acs.commons.forms.helpers.FormHelper;
import com.google.common.collect.ImmutableMap;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class PostRedirectGetFormHelperImplTest {

    private MockSlingHttpServletRequest request;
    private MockSlingHttpServletResponse response;
    private Resource requestResource;

    private FormHelper formHelper;

    @Before
    public void setup() throws LoginException, PersistenceException {
        final ResourceResolverFactory resourceResolverFactory = new MockResourceResolverFactory();
        final ResourceResolver resourceResolver = resourceResolverFactory.getResourceResolver(null);
        final Resource root = resourceResolver.getResource("/");

        request = new MockSlingHttpServletRequest(resourceResolver);
        response = new MockSlingHttpServletResponse();

        formHelper = new PostRedirectGetWithCookiesFormHelperImpl();
        requestResource = resourceResolver.create(root, "/test", Collections.<String, Object>emptyMap());
        request.setResource(requestResource);
    }

    @Test
    public void shouldMapFormName() throws Exception {
        request.setMethod("POST");
        request.setParameterMap(ImmutableMap.<String, Object>of(":form", "demo"));

        final Form form = formHelper.getForm("demo", request, response);

        assertThat(form.getName(), is(equalTo("demo")));
    }

    @Test
    public void shouldMapSingleParam() throws Exception {
        request.setMethod("POST");
        request.setResource(requestResource);
        request.setParameterMap(ImmutableMap.<String, Object>of(":form", "x", "hello", "world"));

        final Form form = formHelper.getForm("x", request, response);

        assertThat(form.get("hello"), is(equalTo("world")));
    }

    @Test
    public void shouldMapFirstValueOfMultiParams() throws Exception {
        request.setMethod("POST");
        request.setResource(requestResource);
        request.setParameterMap(ImmutableMap.<String, Object>of(":form", "x", "hello", new String[]{"world", "mundo"}));

        final Form form = formHelper.getForm("x", request, response);

        assertThat(form.get("hello"), is(equalTo("world")));
    }

}