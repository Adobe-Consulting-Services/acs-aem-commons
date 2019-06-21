/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.httpcache.store.jcr.impl.visitor;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.jcr.impl.visitor.mock.RootNodeMockFactory;

@RunWith(MockitoJUnitRunner.class)
public final class EntryNodeByStringKeyVisitorTest {

    @Test
    public void testPresent() throws RepositoryException, IOException, ClassNotFoundException {
        final RootNodeMockFactory.Settings settings = new RootNodeMockFactory.Settings();
        settings.setEntryNodeCount(10);
        final Node rootNode = new RootNodeMockFactory(settings).build();
        final EntryNodeByStringKeyVisitor visitor = getMockedEntryNodeByStringKeyVisitor("[resourcePath: /content/some/path]", true);

        visitor.visit(rootNode);

        assertNotNull(visitor.getCacheContentIfPresent());
    }

    @Test
    public void testNotPresent() throws RepositoryException, IOException, ClassNotFoundException {
        final RootNodeMockFactory.Settings settings = new RootNodeMockFactory.Settings();
        settings.setEntryNodeCount(10);
        final Node rootNode = new RootNodeMockFactory(settings).build();
        final EntryNodeByStringKeyVisitor visitor = getMockedEntryNodeByStringKeyVisitor("[resourcePath: /content/some/path]", false);

        visitor.visit(rootNode);

        assertNull(visitor.getCacheContentIfPresent());
    }

    private EntryNodeByStringKeyVisitor getMockedEntryNodeByStringKeyVisitor(String cacheKeyStr, boolean match) throws ClassNotFoundException, RepositoryException, IOException {
        final DynamicClassLoaderManager dclm = mock(DynamicClassLoaderManager.class);
        final EntryNodeByStringKeyVisitor visitor = spy(new EntryNodeByStringKeyVisitor(11, dclm, cacheKeyStr));
        final CacheKey cacheKey = mock(CacheKey.class);

        if (match) {
            when(cacheKey.toString()).thenReturn(cacheKeyStr);
        } else {
            when(cacheKey.toString()).thenReturn(RandomStringUtils.random(10000));
        }

        when(visitor.getCacheKey(any(Node.class))).thenReturn(cacheKey);

        return visitor;
    }

}
