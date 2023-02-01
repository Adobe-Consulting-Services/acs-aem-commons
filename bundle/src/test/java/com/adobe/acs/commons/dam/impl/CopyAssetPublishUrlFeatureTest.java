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

package com.adobe.acs.commons.dam.impl;

import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.featureflags.Feature;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class CopyAssetPublishUrlFeatureTest {

    @Rule
    public final AemContext ctx = new AemContext();

    @Test
    public void getName() {
        ctx.registerInjectActivateService(new CopyAssetPublishUrlFeature());

        Feature feature = ctx.getService(Feature.class);
        assertEquals("com.adobe.acs.commons.dam.impl.copyassetpublishurlfeature.feature.flag", feature.getName());
    }

    @Test
    public void getDescription() {
        ctx.registerInjectActivateService(new CopyAssetPublishUrlFeature());

        Feature feature = ctx.getService(Feature.class);
        assertTrue(StringUtils.isNoneBlank(feature.getDescription()));
    }

    @Test
    public void isEnabled_Enabled() {
        ctx.registerInjectActivateService(new CopyAssetPublishUrlFeature(),
                "feature.flag.active.status", "true");

        Feature feature = ctx.getService(Feature.class);
        assertTrue(feature.isEnabled(null));
    }

    @Test
    public void isEnabled_Disabled() {
        ctx.registerInjectActivateService(new CopyAssetPublishUrlFeature(),
                "feature.flag.active.status", "false");

        Feature feature = ctx.getService(Feature.class);
        assertFalse(feature.isEnabled(null));
    }

    @Test
    public void isEnabled_Default() {
        ctx.registerInjectActivateService(new CopyAssetPublishUrlFeature());

        Feature feature = ctx.getService(Feature.class);
        assertFalse(feature.isEnabled(null));
    }
}