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

import io.wcm.testing.mock.aem.junit.AemContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.annotation.Annotation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class RequestParameterHttpCacheConfigExtensionTest {
    @Rule
    public AemContext ctx = new AemContext();

    RequestParameterHttpCacheConfigExtension.Config configWithRequestParameter = new RequestParameterHttpCacheConfigExtension.Config(){
        @Override
        public Class<? extends Annotation> annotationType() {
            return null;
        }

        @Override
        public String config_name() {
            return "config";
        }

        @Override
        public String httpcache_config_extension_requestparameter() {
            return "myParameter";
        }

        @Override
        public String[] httpcache_config_extension_requestparameter_values() {
            return new String[0];
        }

        @Override
        public String webconsole_configurationFactory_nameHint() {
            return null;
        }
    };

    RequestParameterHttpCacheConfigExtension.Config configWithRequestParameterValues = new RequestParameterHttpCacheConfigExtension.Config(){
        @Override
        public Class<? extends Annotation> annotationType() {
            return null;
        }

        @Override
        public String config_name() {
            return "config-with-values";
        }

        @Override
        public String httpcache_config_extension_requestparameter() {
            return "myParameter";
        }

        @Override
        public String[] httpcache_config_extension_requestparameter_values() {
            return new String[]{"zip", "zap", "foo"};
        }

        @Override
        public String webconsole_configurationFactory_nameHint() {
            return null;
        }
    };

    private final RequestParameterHttpCacheConfigExtension extension = new RequestParameterHttpCacheConfigExtension();


    @Before
    public void setUp(){ }

    @Test
    public void test_WithOnlyNameMatch() {
        ctx.request().setQueryString("myParameter");

        extension.activate(configWithRequestParameter);

        boolean actual = extension.accepts(ctx.request(), null, extension.getAllowedKeyValues());

        assertTrue(actual);
    }

    @Test
    public void test_WithOnlyNameMismatch() {
        ctx.request().setQueryString("randomParameter");

        extension.activate(configWithRequestParameter);

        boolean actual = extension.accepts(ctx.request(), null, extension.getAllowedKeyValues());

        assertFalse(actual);
    }


    @Test
    public void test_WithValueMatch() {
        ctx.request().setQueryString("myParameter=foo");

        extension.activate(configWithRequestParameterValues);

        boolean actual = extension.accepts(ctx.request(), null, extension.getAllowedKeyValues());

        assertTrue(actual);
    }

    @Test
    public void test_WithValueMismatch() {
        ctx.request().setQueryString("myParameter=bar");

        extension.activate(configWithRequestParameterValues);

        boolean actual = extension.accepts(ctx.request(), null, extension.getAllowedKeyValues());

        assertFalse(actual);
    }
}
