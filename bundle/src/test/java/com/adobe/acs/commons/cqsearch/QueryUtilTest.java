/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2022 Adobe
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
package com.adobe.acs.commons.cqsearch;

import com.day.cq.search.Query;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

public class QueryUtilTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    public static class ResourceResolverMonster {
        private ResourceResolver resourceResolver;

        public ResourceResolver getResourceResolver() {
            return resourceResolver;
        }
    }

    @Test
    public void testInternalSetResourceResolverOn() throws Exception {
        final ResourceResolverMonster monster = new ResourceResolverMonster();
        assertNull("expect null resourceResolver to start with", monster.getResourceResolver());
        QueryUtil.internalSetResourceResolverOn(context.resourceResolver(), monster);
        assertSame("expect same resourceResolver as aem context",
                context.resourceResolver(), monster.getResourceResolver());
    }

    @Test(expected = NoSuchFieldException.class)
    public void testInternalSetResourceResolverOnMockQuery() throws Exception {
        final Query query = mock(Query.class);
        QueryUtil.internalSetResourceResolverOn(context.resourceResolver(), query);
    }

    @Test
    public void testSetResourceResolverOnMock() {
        final Query query = mock(Query.class);
        QueryUtil.setResourceResolverOn(context.resourceResolver(), query);
        // expect no changes.
    }

    @Test
    public void testSetResourceResolverOnNull() {
        QueryUtil.setResourceResolverOn(context.resourceResolver(), null);
        // expect no changes.
    }
}