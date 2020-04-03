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
public class ResourcePropertiesHttpCacheConfigExtensionTest {

    @Rule
    public AemContext ctx = new AemContext();

    private final ResourcePropertiesHttpCacheConfigExtension extension = new ResourcePropertiesHttpCacheConfigExtension();

    ResourcePropertiesHttpCacheConfigExtension.Config configWithoutValues = new ResourcePropertiesHttpCacheConfigExtension.Config(){
        @Override
        public Class<? extends Annotation> annotationType() {
            return null;
        }

        @Override
        public String config_name() {
            return "config-without-values";
        }

        @Override
        public String httpcache_config_extension_property() {
            return "cacheProperty";
        }

        @Override
        public String[] httpcache_config_extension_property_values() {
            return new String[0];
        }

        @Override
        public String webconsole_configurationFactory_nameHint() {
            return null;
        }
    };

    ResourcePropertiesHttpCacheConfigExtension.Config configWithValues = new ResourcePropertiesHttpCacheConfigExtension.Config(){
        @Override
        public Class<? extends Annotation> annotationType() {
            return null;
        }

        @Override
        public String config_name() {
            return "config-with-values";
        }

        @Override
        public String httpcache_config_extension_property() {
            return "cacheProperty";
        }

        @Override
        public String[] httpcache_config_extension_property_values() {
            return new String[]{"zip", "zap", "foo"};
        }

        @Override
        public String webconsole_configurationFactory_nameHint() {
            return null;
        }
    };

    @Before
    public void setUp(){
        ctx.build().resource("/content/cache/property", "cacheProperty", null).commit();
        ctx.build().resource("/content/cache/values", "cacheProperty", "foo").commit();

        ctx.build().resource("/content/no-cache/property", "noCacheProperty", null).commit();
        ctx.build().resource("/content/no-cache/values", "noCacheProperty", "bar").commit();
    }


    @Test
    public void test_WithOnlyPropertyNameMatch() {
        ctx.currentResource(ctx.resourceResolver().getResource("/content/cache/property"));

        extension.activate(configWithoutValues);

        boolean actual = extension.accepts(ctx.request(), null, extension.getAllowedKeyValues());

        assertTrue(actual);
    }

    @Test
    public void test_WithOnlyPropertyNameMismatch() {
        ctx.currentResource(ctx.resourceResolver().getResource("/content/no-cache/property"));

        extension.activate(configWithoutValues);

        boolean actual = extension.accepts(ctx.request(), null, extension.getAllowedKeyValues());

        assertFalse(actual);
    }


    @Test
    public void test_WithValueMatch() {
        ctx.currentResource(ctx.resourceResolver().getResource("/content/cache/values"));

        extension.activate(configWithValues);

        boolean actual = extension.accepts(ctx.request(), null, extension.getAllowedKeyValues());

        assertTrue(actual);
    }

    @Test
    public void test_WithValueMismatch() {
        ctx.currentResource(ctx.resourceResolver().getResource("/content/no-cache/values"));

        extension.activate(configWithValues);

        boolean actual = extension.accepts(ctx.request(), null, extension.getAllowedKeyValues());

        assertFalse(actual);
    }

}
