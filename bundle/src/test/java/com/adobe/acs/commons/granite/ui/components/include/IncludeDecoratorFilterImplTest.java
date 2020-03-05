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
package com.adobe.acs.commons.granite.ui.components.include;

import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;

import static com.adobe.acs.commons.granite.ui.components.include.IncludeDecoratorFilterImpl.NAMESPACE;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;

@RunWith(MockitoJUnitRunner.class)
public class IncludeDecoratorFilterImplTest {

    @Mock
    FilterChain filterChain;

    IncludeDecoratorFilterImpl systemUnderTest;

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);


    @Before
    public void setUp() throws Exception {

        InputStream inputStream = getClass().getResourceAsStream("filter-test.json");
        context.load().json(inputStream, "/apps/tab");
        context.currentResource("/apps/tab/items/column/items/include");

        systemUnderTest = new IncludeDecoratorFilterImpl();


    }

    @Test
    public void test() throws IOException, ServletException {

        Mockito.doAnswer(invocationOnMock -> {

            SlingHttpServletRequest captured = invocationOnMock.getArgumentAt(0, SlingHttpServletRequest.class);
            assertEquals("block1", captured.getAttribute(NAMESPACE));
            return null;

        }).when(filterChain).doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));

        systemUnderTest.doFilter(context.request(), context.response(), filterChain);

        assertTrue("namespace is removed after the filter is performed", context.request().getAttribute(NAMESPACE) == null);

    }

    @Test
    public void test_nested_include() throws IOException, ServletException {

        Mockito.doAnswer(invocationOnMock -> {

            SlingHttpServletRequest captured = invocationOnMock.getArgumentAt(0, SlingHttpServletRequest.class);
            assertEquals("nested/block1", captured.getAttribute(NAMESPACE));
            return null;

        }).when(filterChain).doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));

        context.request().setAttribute(NAMESPACE, "nested");
        systemUnderTest.doFilter(context.request(), context.response(), filterChain);

        assertEquals("nested", context.request().getAttribute(NAMESPACE));

    }
}