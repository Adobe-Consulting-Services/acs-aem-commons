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
package com.adobe.acs.commons.redirects.models;

import com.adobe.acs.commons.redirects.filter.RedirectFilterMBean;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.resourcebuilder.api.ResourceBuilder;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Calendar;

import static com.adobe.acs.commons.redirects.filter.RedirectFilter.REDIRECT_RULE_RESOURCE_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UpgradeLegacyRedirectsTest {
    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    @Before
    public void setUp(){
        context.addModelsForClasses(UpgradeLegacyRedirects.class);
        RedirectFilterMBean mbean = mock(RedirectFilterMBean.class);
        when(mbean.getBucket()).thenReturn("settings");
        when(mbean.getConfigName()).thenReturn("redirects");
        context.registerService(RedirectFilterMBean.class, mbean);

    }

    @Test
    public void ignoreUpgrade(){

        UpgradeLegacyRedirects model =
                context.getService(ModelFactory.class).createModel(context.request(), UpgradeLegacyRedirects.class);
        assertFalse(model.isMoved());
    }

    @Test
    public void upgrade(){
        ResourceBuilder rb = context.build()
                .resource("/conf/global/settings/redirects")
                .resource("/conf/acs-commons/redirects");
        rb.resource("/conf/acs-commons/redirects/redirect-1",
                "sling:resourceType", REDIRECT_RULE_RESOURCE_TYPE,
                RedirectRule.SOURCE_PROPERTY_NAME, "/1",
                RedirectRule.TARGET_PROPERTY_NAME, "/2",
                RedirectRule.STATUS_CODE_PROPERTY_NAME, "301",
                RedirectRule.UNTIL_DATE_PROPERTY_NAME, "16 February 2021");

        UpgradeLegacyRedirects model =
                context.getService(ModelFactory.class).createModel(context.request(), UpgradeLegacyRedirects.class);
        assertTrue(model.isMoved());
        assertNull("/conf/acs-commons/redirects/redirect-1 should be moved to \"/conf/global/settings/redirects/redirect-1\"",
                context.resourceResolver().getResource("/conf/acs-commons/redirects/redirect-1"));
        Resource movedRule = context.resourceResolver().getResource("/conf/global/settings/redirects/redirect-1");
        assertNotNull("/conf/acs-commons/redirects/redirect-1 should be moved to \"/conf/global/settings/redirects/redirect-1\"",
                movedRule);
        // string date should  be converted to Java Calendar
        Calendar untilDate = movedRule.getValueMap().get(RedirectRule.UNTIL_DATE_PROPERTY_NAME, Calendar.class);
        assertEquals(2021, untilDate.get(Calendar.YEAR));
        assertEquals(1, untilDate.get(Calendar.MONTH)); // 0-based
        assertEquals(16, untilDate.get(Calendar.DATE));
        assertTrue(context.resourceResolver().getResource("/conf/acs-commons/redirects").getValueMap().get("moved", false));
    }
}
