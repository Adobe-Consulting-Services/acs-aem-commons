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
public class RequestHeaderHttpCacheConfigExtensionTest {

    @Rule
    public AemContext ctx = new AemContext();

    private final RequestHeaderHttpCacheConfigExtension extension = new RequestHeaderHttpCacheConfigExtension();

    RequestHeaderHttpCacheConfigExtension.Config configWithRequestHeader = new RequestHeaderHttpCacheConfigExtension.Config(){
        @Override
        public Class<? extends Annotation> annotationType() {
            return null;
        }

        @Override
        public String config_name() {
            return "config-with-request-header";
        }

        @Override
        public String httpcache_config_extension_requestheader() {
            return "myHeader";
        }

        @Override
        public String[] httpcache_config_extension_requestheader_values() {
            return new String[0];
        }

        @Override
        public String webconsole_configurationFactory_nameHint() {
            return null;
        }
    };

    RequestHeaderHttpCacheConfigExtension.Config configWithRequestHeaderValues = new RequestHeaderHttpCacheConfigExtension.Config(){
        @Override
        public Class<? extends Annotation> annotationType() {
            return null;
        }

        @Override
        public String config_name() {
            return "config-with-request-header-values";
        }

        @Override
        public String httpcache_config_extension_requestheader() {
            return "myHeader";
        }

        @Override
        public String[] httpcache_config_extension_requestheader_values() {
            return new String[]{"zip", "zap", "foo"};
        }

        @Override
        public String webconsole_configurationFactory_nameHint() {
            return null;
        }
    };


    @Before
    public void setUp(){

    }

    @Test
    public void test_WithOnlyNameMatch() {
        ctx.request().addHeader("myHeader", "");

        extension.activate(configWithRequestHeader);

        boolean actual = extension.accepts(ctx.request(), null, extension.getAllowedKeyValues());

        assertTrue(actual);
    }

    @Test
    public void test_WithOnlyNameMismatch() {
        ctx.request().addHeader("randomHeader", null);

        extension.activate(configWithRequestHeader);

        boolean actual = extension.accepts(ctx.request(), null, extension.getAllowedKeyValues());

        assertFalse(actual);
    }


    @Test
    public void test_WithValueMatch() {
        ctx.request().addHeader("myHeader", "foo");

        extension.activate(configWithRequestHeaderValues);

        boolean actual = extension.accepts(ctx.request(), null, extension.getAllowedKeyValues());

        assertTrue(actual);
    }

    @Test
    public void test_WithValueMismatch() {
        ctx.request().addHeader("myHeader", "bar");

        extension.activate(configWithRequestHeaderValues);

        boolean actual = extension.accepts(ctx.request(), null, extension.getAllowedKeyValues());

        assertFalse(actual);
    }

}
