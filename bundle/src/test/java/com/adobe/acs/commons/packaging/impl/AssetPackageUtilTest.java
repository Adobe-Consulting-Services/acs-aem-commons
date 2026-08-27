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
package com.adobe.acs.commons.packaging.impl;

import java.util.List;

import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.adobe.acs.commons.packaging.util.AssetPackageUtil;

import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class AssetPackageUtilTest {

    private static final String PACKAGER_CONTENT_PATH = "/etc/acs-commons/packagers/assets";

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

    @Before
    public void setup() {
        context.load().json(getClass().getResourceAsStream("AssetPackageUtilContent.json"),
            "/content/we-retail/language-masters/en/experience");
        context.load().json(getClass().getResourceAsStream("AssetPackagerServletDamContent.json"),
            "/content/dam/we-retail/en");
    }

    @Test
    public void getPackageFilterPaths() {
        context.load().json(getClass().getResourceAsStream("AssetPackagerServletConfiguration.json"),
            PACKAGER_CONTENT_PATH);

        final ResourceResolver resourceResolver = context.resourceResolver();
        final ValueMap properties = getAssetPackagerConfigurationProperties();

        final AssetPackageUtil assetPackageUtil = new AssetPackageUtil(properties, resourceResolver);

        final List<PathFilterSet> packageFilterPaths = assetPackageUtil.getPackageFilterPaths();

        assertEquals(4, packageFilterPaths.size());
    }

    /**
     * @return asset packager configuration properties
     */
    private ValueMap getAssetPackagerConfigurationProperties() {
        final Resource assetPackagerResource = context.resourceResolver().getResource(PACKAGER_CONTENT_PATH);

        return assetPackagerResource.getChild("jcr:content/configuration").getValueMap();
    }
}
