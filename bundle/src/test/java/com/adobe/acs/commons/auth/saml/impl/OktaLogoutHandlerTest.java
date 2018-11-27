/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.auth.saml.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.wcm.testing.mock.aem.junit.AemContext;

@RunWith(MockitoJUnitRunner.class)
public class OktaLogoutHandlerTest {

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    @Mock
    private OktaLogoutHandler.Config config;
    
    
    private OktaLogoutHandler underTest = new OktaLogoutHandler();

    @Test
    public void testExtractCredentialsReturnsNull() {
        assertNull(underTest.extractCredentials(context.request(), context.response()));
    }

    @Test
    public void testRequestCredentialsReturnsFalse() throws Exception {
        assertFalse(underTest.requestCredentials(context.request(), context.response()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void activateWithoutHostThrowsIllegalArgumentException() {
        underTest.activate(config);
    }

    @Test
    public void testDropCredentials() throws Exception {
           when(config.okta_host_name()).thenReturn("www.okta.com");
        underTest.dropCredentials(context.request(), context.response());

        assertRedirect("https://www.okta.com/login/signout", context.response());
    }

    @Test
    public void testDropCredentialsWithFromUri() throws Exception {
       when(config.okta_host_name()).thenReturn("www.okta.com");
       when(config.from_uri()).thenReturn("www.myco.com");

        underTest.activate(config);
        underTest.dropCredentials(context.request(), context.response());

        assertRedirect("https://www.okta.com/login/signout?fromURI=www.myco.com", context.response());
    }

    private void assertRedirect(String expected, MockSlingHttpServletResponse response) {
        assertEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, response.getStatus());
        assertEquals(expected, response.getHeader("Location"));
    }
}