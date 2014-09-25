/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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
package com.adobe.acs.commons.genericlists.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.acs.commons.genericlists.GenericList;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

@RunWith(MockitoJUnitRunner.class)
public class GenericListJsonResourceProviderTest {

    private GenericListJsonResourceProvider provider = new GenericListJsonResourceProvider();

    @Mock
    private PageManager pageManager;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Page validPage;

    @Mock
    private Page invalidPage;

    @Mock
    private GenericList list;

    private String goodMntPath = GenericListJsonResourceProvider.ROOT + "/good";

    private String goodPagePath = GenericListJsonResourceProvider.LIST_ROOT + "/good";

    private String badMntPath = GenericListJsonResourceProvider.ROOT + "/bad";

    private String badPagePath = GenericListJsonResourceProvider.LIST_ROOT + "/bad";

    private String nonExisting = GenericListJsonResourceProvider.ROOT + "/non-existing";

    @Before
    public void setup() {
        when(resourceResolver.adaptTo(PageManager.class)).thenReturn(pageManager);
        when(pageManager.getPage(goodPagePath)).thenReturn(validPage);
        when(pageManager.getPage(badPagePath)).thenReturn(invalidPage);
        when(validPage.adaptTo(GenericList.class)).thenReturn(list);
    }

    @Test
    public void testRootResource() {
        assertNull(provider.getResource(null, GenericListJsonResourceProvider.ROOT));
    }

    @Test
    public void testPageWhichDoesntExist() {
        assertNull(provider.getResource(resourceResolver, nonExisting));
    }

    @Test
    public void testPageWhichDoesntAdapt() {
        assertNull(provider.getResource(resourceResolver, badMntPath));
    }

    @Test
    public void testPageWhichDoesAdapt() {
        Resource r = provider.getResource(resourceResolver, goodMntPath);
        assertNotNull(r);
    }

    @Test
    public void testPageWhichDoesAdaptWithExtension() {
        Resource r = provider.getResource(resourceResolver, goodMntPath + ".json");
        assertNotNull(r);
    }

}
