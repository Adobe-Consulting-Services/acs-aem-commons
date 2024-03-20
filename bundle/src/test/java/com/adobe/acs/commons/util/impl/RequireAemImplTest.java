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
package com.adobe.acs.commons.util.impl;

import com.adobe.acs.commons.util.RequireAem;
import com.adobe.granite.license.ProductInfo;
import com.adobe.granite.license.ProductInfoProvider;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.Version;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class RequireAemImplTest {

    @Mock
    ProductInfoProvider productInfoProvider;

    @Mock
    ProductInfo productInfo;

    @Rule
    public final AemContext ctx = new AemContext();

    private void setUpAsCloudReady() {
        doReturn(new Version("2020.2.2239.20200214T010959Z")).when(productInfo).getVersion();
        doReturn("2020.2.2239.20200214T010959Z").when(productInfo).getShortVersion();

        doReturn(productInfo).when(productInfoProvider).getProductInfo();

        ctx.registerService(ProductInfoProvider.class, productInfoProvider);
        ctx.registerInjectActivateService(new RequireAemImpl());
    }

    private void setUpAsNotCloudReady() {
        doReturn(new Version("6.5.4")).when(productInfo).getVersion();
        doReturn("6.5.4").when(productInfo).getShortVersion();
        doReturn(productInfo).when(productInfoProvider).getProductInfo();

        ctx.registerService(ProductInfoProvider.class, productInfoProvider);
        ctx.registerInjectActivateService(new RequireAemImpl());
    }
    
    @Test
    public void isCloudReady_True() {
        setUpAsCloudReady();

        RequireAem requireAem = ctx.getService(RequireAem.class);

        assertEquals(RequireAem.Distribution.CLOUD_READY, requireAem.getDistribution());
    }

    @Test
    public void isCloudReady_False() {
        setUpAsNotCloudReady();

        RequireAem requireAem = ctx.getService(RequireAem.class);

        assertEquals(RequireAem.Distribution.CLASSIC, requireAem.getDistribution());
    }

    @Test
    public void referenceFilter_CloudReady_True_Satisfied() {
        setUpAsCloudReady();

        RequireAem[] requireAems = ctx.getServices(RequireAem.class, "(distribution=cloud-ready)");

        assertEquals(1, requireAems.length);
        assertEquals(RequireAem.Distribution.CLOUD_READY, requireAems[0].getDistribution());
    }

    @Test
    public void referenceFilter_CloudReady_True_Unsatisfied() {
        setUpAsNotCloudReady();

        RequireAem[] requireAems = ctx.getServices(RequireAem.class, "(distribution=cloud-ready)");

        assertEquals(0, requireAems.length);
    }

    @Test
    public void referenceFilter_CloudReady_False_Satisfied() {
        setUpAsNotCloudReady();

        RequireAem[] requireAems = ctx.getServices(RequireAem.class, "(distribution=classic)");

        assertEquals(1, requireAems.length);
        assertEquals(RequireAem.Distribution.CLASSIC, requireAems[0].getDistribution());
    }

    @Test
    public void referenceFilter_CloudReady_False_Unsatisfied() {
        setUpAsCloudReady();

        RequireAem[] requireAems = ctx.getServices(RequireAem.class, "(distribution=classic)");

        assertEquals(0, requireAems.length);
    }
}