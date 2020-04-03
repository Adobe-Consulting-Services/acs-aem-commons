/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
package com.adobe.acs.commons.httpcache.config.impl;

import com.day.cq.commons.PathInfo;
import org.apache.sling.api.SlingHttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.annotation.Annotation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class RequestPathHttpCacheConfigExtensionTest {

    @Mock
    private SlingHttpServletRequest request;

    @InjectMocks
    private RequestPathHttpCacheConfigExtension extension;

    RequestPathHttpCacheConfigExtension.Config config = new RequestPathHttpCacheConfigExtension.Config(){

        @Override
        public Class<? extends Annotation> annotationType() {
            return null;
        }

        @Override
        public String config_name() {
            return "someConfig";
        }

        @Override
        public String[] httpcache_config_extension_paths_allowed() {
            return new String[]{
                    "/content/acs-commons/(.*)"
            };
        }

        @Override
        public String[] httpcache_config_extension_selectors_allowed() {
            return new String[]{
                "content"
            };
        }

        @Override
        public String[] httpcache_config_extension_extensions_allowed() {
            return new String[]{
                    "html"
            };
        }

        @Override
        public String webconsole_configurationFactory_nameHint() {
            return null;
        }
    };



    @Before
    public void setUp(){
        extension.activate(config);
    }

    @Test
    public void test_match() {

        prepRequestPathInfo("/content/acs-commons/en/homepage/jcr:content/component.content.html");

        boolean actual = extension.accepts(request, null);

        assertTrue(actual);
    }

    @Test
    public void test_no_path_match() {

        prepRequestPathInfo("/content/chuck-norris/en/homepage/jcr:content/component.content.html");

        boolean actual = extension.accepts(request, null);

        assertFalse(actual);
    }

    @Test
    public void test_no_selector_match() {

        prepRequestPathInfo("/content/acs-commons/en/homepage/jcr:content/component.body.html");

        boolean actual = extension.accepts(request, null);

        assertFalse(actual);
    }

    @Test
    public void test_no_extension_match() {

        prepRequestPathInfo("/content/acs-commons/en/homepage/jcr:content/component.body.json");

        boolean actual = extension.accepts(request, null);

        assertFalse(actual);
    }

    private void prepRequestPathInfo(String requestPathInfo){
        PathInfo pathInfo = new PathInfo(requestPathInfo);
        when(request.getRequestPathInfo()).thenReturn(pathInfo);
    }


}
