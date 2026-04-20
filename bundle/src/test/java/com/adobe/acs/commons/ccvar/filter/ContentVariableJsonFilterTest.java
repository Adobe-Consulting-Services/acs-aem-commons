/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.ccvar.filter;

import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.adobe.acs.commons.ccvar.ContextualContentVariableTestUtil.defaultService;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class ContentVariableJsonFilterTest {
    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private FilterChain filterChain;

    private ContentVariableJsonFilter filter;

    @Before
    public void setup() throws IOException, ServletException {
        context.load().json(getClass().getResourceAsStream("ContentVariableJsonFilterContent.json"), "/content/we-retail/language-masters/en/experience");
        context.currentResource("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten/jcr:content/root/hero_image");
    }

    @Test
    public void testNonJsonResponse() throws IOException, ServletException {
        initServices(null);
        context.requestPathInfo().setExtension("html");

        String before = "<html><body>((page_properties.jcr:title))</body></html>";
        doAnswer(invocation -> {
            HttpServletResponse response =
                    (HttpServletResponse) invocation.getArguments()[1];
            response.getWriter().println(before);
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(context.request(), context.response(), filterChain);
        assertTrue(startsWith(before)
                .matches(context.response().getOutputAsString()));
    }

    @Test
    public void testModelJsonResponseWithSingleProp() throws IOException, ServletException {
        initServices(null);
        context.currentResource("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten/jcr:content/root/hero_image");
        context.request().setResource(context.currentResource());
        context.request().setPathInfo("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten/jcr:content/root/hero_image.model.json");
        context.requestPathInfo().setExtension("json");

        FilterChain mocked = mock(MockFilterChain.class);
        String before = "{\"jcr:primaryType\":\"nt:unstructured\",\"fileReference\":\"/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/surfer-wave-01.jpg\",\"useFullWidth\":\"true\",\"title\":\"((page_properties.jcr:title))\",\"sling:resourceType\":\"weretail/components/content/heroimage\"}";
        doAnswer(invocation -> {
            HttpServletResponse response =
                    (HttpServletResponse) invocation.getArguments()[1];
            response.setCharacterEncoding("utf-8");
            response.setContentType("application/json");
            response.getWriter().println(before);
            return null;
        }).when(mocked).doFilter(any(), any());

        String after = "{\"jcr:primaryType\":\"nt:unstructured\",\"fileReference\":\"/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/surfer-wave-01.jpg\",\"useFullWidth\":\"true\",\"title\":\"Arctic Surfing In Lofoten\",\"sling:resourceType\":\"weretail/components/content/heroimage\"}";
        filter.doFilter(context.request(), context.response(), mocked);
        assertTrue(startsWith(after)
                .matches(context.response().getOutputAsString()));
    }

    @Test
    public void testModelJsonResponseWithSinglePropAndUrlEncodeAction() throws IOException, ServletException {
        initServices(null);
        context.currentResource("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten/jcr:content/root/hero_image_1");
        context.request().setResource(context.currentResource());
        context.request().setPathInfo("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten/jcr:content/root/hero_image_1.model.json");
        context.requestPathInfo().setExtension("json");

        FilterChain mocked = mock(MockFilterChain.class);
        String before = "{\"jcr:primaryType\":\"nt:unstructured\",\"fileReference\":\"/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/surfer-wave-01.jpg\",\"useFullWidth\":\"true\",\"property\":\"((page_properties.property!url))\",\"sling:resourceType\":\"weretail/components/content/heroimage\"}";
        doAnswer(invocation -> {
            HttpServletResponse response =
                    (HttpServletResponse) invocation.getArguments()[1];
            response.setCharacterEncoding("utf-8");
            response.setContentType("application/json");
            response.getWriter().println(before);
            return null;
        }).when(mocked).doFilter(any(), any());

        String after = "{\"jcr:primaryType\":\"nt:unstructured\",\"fileReference\":\"/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/surfer-wave-01.jpg\",\"useFullWidth\":\"true\",\"property\":\"test+space\",\"sling:resourceType\":\"weretail/components/content/heroimage\"}";
        filter.doFilter(context.request(), context.response(), mocked);
        assertTrue(startsWith(after)
                .matches(context.response().getOutputAsString()));
    }

    @Test
    public void testModelJsonResponseWithSinglePropAndInvalidAction() throws IOException, ServletException {
        initServices(null);
        context.currentResource("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten/jcr:content/root/hero_image_2");
        context.request().setResource(context.currentResource());
        context.request().setPathInfo("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten/jcr:content/root/hero_image_2.model.json");
        context.requestPathInfo().setExtension("json");

        FilterChain mocked = mock(MockFilterChain.class);
        String before = "{\"jcr:primaryType\":\"nt:unstructured\",\"fileReference\":\"/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/surfer-wave-01.jpg\",\"useFullWidth\":\"true\",\"property\":\"((page_properties.property!fakeAction))\",\"sling:resourceType\":\"weretail/components/content/heroimage\"}";
        doAnswer(invocation -> {
            HttpServletResponse response =
                    (HttpServletResponse) invocation.getArguments()[1];
            response.setCharacterEncoding("utf-8");
            response.setContentType("application/json");
            response.getWriter().println(before);
            return null;
        }).when(mocked).doFilter(any(), any());

        String after = "{\"jcr:primaryType\":\"nt:unstructured\",\"fileReference\":\"/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/surfer-wave-01.jpg\",\"useFullWidth\":\"true\",\"property\":\"test space\",\"sling:resourceType\":\"weretail/components/content/heroimage\"}";
        filter.doFilter(context.request(), context.response(), mocked);
        assertTrue(startsWith(after)
                .matches(context.response().getOutputAsString()));
    }

    @Test
    public void testModelJsonResponseWithMultiProps() throws IOException, ServletException {
        initServices(null);
        context.currentResource("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten/jcr:content/root/hero_image");
        context.request().setResource(context.currentResource());
        context.request().setPathInfo("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten/jcr:content/root/hero_image.model.json");
        context.requestPathInfo().setExtension("json");

        FilterChain mocked = mock(MockFilterChain.class);
        String before = "{\"jcr:primaryType\":\"nt:unstructured\",\"fileReference\":\"/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/surfer-wave-01.jpg\",\"useFullWidth\":\"true\",\"stringArray\":[\"((page_properties.pageTitle))\",\"((page_properties.jcr:title))\"],\"objectArray\":[{\"pageTitle\":\"((page_properties.pageTitle))\"},{\"jcrTitle\":\"((page_properties.jcr:title))\"}],\"sling:resourceType\":\"weretail/components/content/heroimage\"}";
        doAnswer(invocation -> {
            HttpServletResponse response =
                    (HttpServletResponse) invocation.getArguments()[1];
            response.setCharacterEncoding("utf-8");
            response.setContentType("application/json");
            response.getWriter().println(before);
            return null;
        }).when(mocked).doFilter(any(), any());

        String after = "{\"jcr:primaryType\":\"nt:unstructured\",\"fileReference\":\"/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/surfer-wave-01.jpg\",\"useFullWidth\":\"true\",\"stringArray\":[\"Surfing In Arctic Lofoten\",\"Arctic Surfing In Lofoten\"],\"objectArray\":[{\"pageTitle\":\"Surfing In Arctic Lofoten\"},{\"jcrTitle\":\"Arctic Surfing In Lofoten\"}],\"sling:resourceType\":\"weretail/components/content/heroimage\"}";
        filter.doFilter(context.request(), context.response(), mocked);
        assertTrue(startsWith(after)
                .matches(context.response().getOutputAsString()));
    }

    @Test
    public void testInvalidJson() throws IOException, ServletException {
        initServices(null);
        context.currentResource("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten/jcr:content/root/hero_image");
        context.request().setResource(context.currentResource());
        context.request().setPathInfo("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten/jcr:content/root/hero_image.model.json");
        context.requestPathInfo().setExtension("json");

        FilterChain mocked = mock(MockFilterChain.class);
        String before = "<html><body>((page_properties.jcr:title))</body></html>";
        doAnswer(invocation -> {
            HttpServletResponse response =
                    (HttpServletResponse) invocation.getArguments()[1];
            response.setCharacterEncoding("utf-8");
            response.getWriter().println(before);
            return null;
        }).when(mocked).doFilter(any(), any());

        filter.doFilter(context.request(), context.response(), mocked);
        assertTrue(startsWith(before)
                .matches(context.response().getOutputAsString()));
    }

    @Test
    public void testExcludeConfig() throws IOException, ServletException {
        Map<String, Object> config = new HashMap<>();
        config.put("excludes", new String[]{".*\\.model\\..*"});
        initServices(config);
        context.currentResource("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten/jcr:content/root/hero_image");
        context.request().setResource(context.currentResource());
        context.request().setPathInfo("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten/jcr:content/root/hero_image.model.json");
        context.requestPathInfo().setExtension("json");

        FilterChain mocked = mock(MockFilterChain.class);
        String before = "{\"jcr:primaryType\":\"nt:unstructured\",\"fileReference\":\"/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/surfer-wave-01.jpg\",\"useFullWidth\":\"true\",\"title\":\"((page_properties.jcr:title))\",\"sling:resourceType\":\"weretail/components/content/heroimage\"}";
        doAnswer(invocation -> {
            HttpServletResponse response =
                    (HttpServletResponse) invocation.getArguments()[1];
            response.setCharacterEncoding("utf-8");
            response.setContentType("application/json");
            response.getWriter().println(before);
            return null;
        }).when(mocked).doFilter(any(), any());

        filter.doFilter(context.request(), context.response(), mocked);
        assertTrue(startsWith(before)
                .matches(context.response().getOutputAsString()));
    }

    @Test
    public void testEmptyConfig() throws IOException, ServletException {
        Map<String, Object> config = new HashMap<>();
        config.put("includes", new String[]{""});
        config.put("excludes", new String[]{""});
        initServices(config);
        context.currentResource("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten/jcr:content/root/hero_image");
        context.request().setResource(context.currentResource());
        context.request().setPathInfo("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten/jcr:content/root/hero_image.json");
        context.requestPathInfo().setExtension("json");

        FilterChain mocked = mock(MockFilterChain.class);
        String before = "{\"jcr:primaryType\":\"nt:unstructured\",\"fileReference\":\"/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/surfer-wave-01.jpg\",\"useFullWidth\":\"true\",\"title\":\"((page_properties.jcr:title))\",\"sling:resourceType\":\"weretail/components/content/heroimage\"}";
        doAnswer(invocation -> {
            HttpServletResponse response =
                    (HttpServletResponse) invocation.getArguments()[1];
            response.setCharacterEncoding("utf-8");
            response.setContentType("application/json");
            response.getWriter().println(before);
            return null;
        }).when(mocked).doFilter(any(), any());

        String after = "{\"jcr:primaryType\":\"nt:unstructured\",\"fileReference\":\"/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/surfer-wave-01.jpg\",\"useFullWidth\":\"true\",\"title\":\"Arctic Surfing In Lofoten\",\"sling:resourceType\":\"weretail/components/content/heroimage\"}";
        filter.doFilter(context.request(), context.response(), mocked);
        assertTrue(startsWith(after)
                .matches(context.response().getOutputAsString()));
    }

    @Test
    public void testInvalidConfig() throws IOException, ServletException {
        Map<String, Object> config = new HashMap<>();
        config.put("includes", new String[]{"*.model*"});
        config.put("excludes", new String[]{"*model*"});
        initServices(config);
        context.currentResource("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten/jcr:content/root/hero_image");
        context.request().setResource(context.currentResource());
        context.request().setPathInfo("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten/jcr:content/root/hero_image.json");
        context.requestPathInfo().setExtension("json");

        FilterChain mocked = mock(MockFilterChain.class);
        String before = "{\"jcr:primaryType\":\"nt:unstructured\",\"fileReference\":\"/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/surfer-wave-01.jpg\",\"useFullWidth\":\"true\",\"title\":\"((page_properties.jcr:title))\",\"sling:resourceType\":\"weretail/components/content/heroimage\"}";
        doAnswer(invocation -> {
            HttpServletResponse response =
                    (HttpServletResponse) invocation.getArguments()[1];
            response.setCharacterEncoding("utf-8");
            response.setContentType("application/json");
            response.getWriter().println(before);
            return null;
        }).when(mocked).doFilter(any(), any());

        String after = "{\"jcr:primaryType\":\"nt:unstructured\",\"fileReference\":\"/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/surfer-wave-01.jpg\",\"useFullWidth\":\"true\",\"title\":\"((page_properties.jcr:title))\",\"sling:resourceType\":\"weretail/components/content/heroimage\"}";
        filter.doFilter(context.request(), context.response(), mocked);
        assertTrue(startsWith(after)
                .matches(context.response().getOutputAsString()));
    }

    @Test
    public void testInit() throws IOException, ServletException {
        initServices(null);
        filter.init(null);
    }

    @Test
    public void testDestroy() throws IOException, ServletException {
        initServices(null);
        filter.destroy();
    }

    private void initServices(Map<String, Object> config) {
        defaultService(context);
        if (config != null) {
            filter = context.registerInjectActivateService(new ContentVariableJsonFilter(), config);
        } else {
            filter = context.registerInjectActivateService(new ContentVariableJsonFilter());
        }
    }

    static class MockFilterChain implements FilterChain {

        MockFilterChain() {
        }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
            return;
        }
    }

}
