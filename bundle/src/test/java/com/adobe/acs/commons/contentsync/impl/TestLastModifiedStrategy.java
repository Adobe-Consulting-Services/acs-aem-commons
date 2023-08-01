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
package com.adobe.acs.commons.contentsync.impl;

import com.adobe.acs.commons.contentsync.CatalogItem;
import com.adobe.acs.commons.contentsync.UpdateStrategy;
import com.day.cq.wcm.api.Page;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestLastModifiedStrategy {

    @Rule
    public AemContext context = new AemContext(ResourceResolverType.RESOURCEPROVIDER_MOCK);

    private UpdateStrategy updateStrategy;

    @Before
    public void setUp() {
        updateStrategy = new LastModifiedStrategy();
    }

    /**
     * isModified() returns false if cq:lastModified/jcr:lastModified is not set
     */
    @Test
    public void testLastModifiedNA() {
        String pagePath = "/content/wknd/page";
        JsonObject catalogItem = Json.createObjectBuilder()
                .add("path", pagePath)
                .add("jcr:primaryType", "cq:Page")
                .build();

        Page page = context.create().page(pagePath);
        Resource pageResource = page.adaptTo(Resource.class);
        assertFalse(updateStrategy.isModified(new CatalogItem(catalogItem), pageResource));
    }

    @Test
    public void testPageModified() {
        String pagePath = "/content/wknd/page";
        ZonedDateTime remoteTimestamp = ZonedDateTime.now().minusDays(1);
        ZonedDateTime localTimestamp = ZonedDateTime.now().minusDays(2);

        JsonObject catalogItem = Json.createObjectBuilder()
                .add("path", pagePath)
                .add("jcr:primaryType", "cq:Page")
                .add("lastModified", remoteTimestamp.toInstant().toEpochMilli())
                .build();

        Page page = context.create().page(pagePath, null, Collections.singletonMap("cq:lastModified", GregorianCalendar.from(localTimestamp)));
        Resource pageResource = page.adaptTo(Resource.class);

        assertTrue(updateStrategy.isModified(new CatalogItem(catalogItem), pageResource));
    }

    @Test
    public void testPageNotModified() {
        String pagePath = "/content/wknd/page";
        ZonedDateTime localTimestamp = ZonedDateTime.now().minusDays(1);
        ZonedDateTime remoteTimestamp = ZonedDateTime.now().minusDays(2);

        JsonObject catalogItem = Json.createObjectBuilder()
                .add("path", pagePath)
                .add("jcr:primaryType", "cq:Page")
                .add("lastModified", remoteTimestamp.toInstant().toEpochMilli())
                .build();

        Page page = context.create().page(pagePath, null, Collections.singletonMap("cq:lastModified", GregorianCalendar.from(localTimestamp)));
        Resource pageResource = page.adaptTo(Resource.class);

        assertFalse(updateStrategy.isModified(new CatalogItem(catalogItem), pageResource));
    }

    @Test
    public void testAccepts() {
        String pagePath = "/content/wknd/page";

        Page page = context.create().page(pagePath);
        Resource pageResource = page.adaptTo(Resource.class);
        assertTrue(updateStrategy.accepts(pageResource));
        assertFalse(updateStrategy.accepts(pageResource.getChild("jcr:content")));
    }
}
