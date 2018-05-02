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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.Map;

import javax.jcr.Node;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.jcr.impl.visitor.mock.RootNodeMockFactory;

@PrepareForTest({EntryNodeMapVisitor.class})
@RunWith(PowerMockRunner.class)
public class EntryNodeMapVisitorTest
{

    @Test
    public void test10entries() throws Exception
    {
        final RootNodeMockFactory.Settings settings = new RootNodeMockFactory.Settings();
        settings.setEntryNodeCount(10);
        final Node rootNode = new RootNodeMockFactory(settings).build();
        final EntryNodeMapVisitor visitor = getMockedNodeMapVisitor();

        visitor.visit(rootNode);

        final Map<CacheKey, CacheContent> cache = visitor.getCache();

        assertEquals(10, cache.size());
    }

    @Test
    public void testNoEntries() throws Exception
    {
        final RootNodeMockFactory.Settings settings = new RootNodeMockFactory.Settings();
        settings.setEntryNodeCount(0);
        final Node rootNode = new RootNodeMockFactory(settings).build();
        final EntryNodeMapVisitor visitor = getMockedNodeMapVisitor();

        visitor.visit(rootNode);

        final Map<CacheKey, CacheContent> cache = visitor.getCache();

        assertTrue(cache.isEmpty());
    }

    @Test
    public void test5BucketDepth() throws Exception
    {
        final RootNodeMockFactory.Settings settings = new RootNodeMockFactory.Settings();
        settings.setEntryNodeCount(10);
        settings.setBucketDepth(5);
        final Node rootNode = new RootNodeMockFactory(settings).build();
        final EntryNodeMapVisitor visitor = getMockedNodeMapVisitor();

        visitor.visit(rootNode);

        final Map<CacheKey, CacheContent> cache = visitor.getCache();

        assertEquals(10, cache.size());
    }

    @Test
    public void test5entries() throws Exception
    {
        final RootNodeMockFactory.Settings settings = new RootNodeMockFactory.Settings();
        settings.setEntryNodeCount(5);
        final Node rootNode = new RootNodeMockFactory(settings).build();
        final EntryNodeMapVisitor visitor = getMockedNodeMapVisitor();

        visitor.visit(rootNode);

        final Map<CacheKey, CacheContent> cache = visitor.getCache();

        assertEquals(5, cache.size());
    }



    private EntryNodeMapVisitor getMockedNodeMapVisitor() throws Exception
    {
        final DynamicClassLoaderManager dclm = mock(DynamicClassLoaderManager.class);


        final EntryNodeMapVisitor visitor = new EntryNodeMapVisitor(11, dclm);
        final EntryNodeMapVisitor spy = spy(visitor);

        when(spy, "getCacheContent", any(Node.class)).thenAnswer(new Answer<Object>()
        {
            @Override public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                return generateRandomCacheContent();
            }
        });

        when(spy, "getCacheKey", any(Node.class)).thenAnswer(new Answer<Object>()
        {
            @Override public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                return generateRandomCacheKey();
            }
        });

        return spy;
    }

    public static CacheKey generateRandomCacheKey()
    {
        final String randomString = RandomStringUtils.random(10);
        return new CacheKey()
        {
            @Override public String getUri()
            {
                return randomString;
            }

            @Override public String getHierarchyResourcePath()
            {
                return randomString;
            }

            @Override public boolean isInvalidatedBy(CacheKey cacheKey)
            {
                return false;
            }

            public int hashCode(){
                return randomString.hashCode();
            }

            public String toString(){
                return randomString;
            }

            public boolean equals(Object o){
                return false;
            }
        };
    }

    private CacheContent generateRandomCacheContent()
    {
        return new CacheContent();
    }

}
