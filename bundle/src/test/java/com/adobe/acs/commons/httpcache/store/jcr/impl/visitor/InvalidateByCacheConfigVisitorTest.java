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

import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

import javax.jcr.Node;

import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.jcr.impl.visitor.mock.RootNodeMockFactory;

@PrepareForTest({EntryNodeMapVisitor.class,InvalidateByCacheConfigVisitor.class})
@RunWith(PowerMockRunner.class)
public class InvalidateByCacheConfigVisitorTest
{
    @Test
    public void test() throws Exception
    {
        final InvalidateByCacheConfigVisitor visitor = getInvalidateByCacheConfigVisitor(5, true);
        final RootNodeMockFactory.Settings settings = new RootNodeMockFactory.Settings();
        settings.setEntryNodeCount(10);

        final Node rootNode = new RootNodeMockFactory(settings).build();

        visitor.visit(rootNode);
        visitor.close();
        Mockito.verify(rootNode.getSession(), Mockito.times(2)).save();
    }

    private InvalidateByCacheConfigVisitor getInvalidateByCacheConfigVisitor(long delta, boolean knows) throws Exception
    {

        final DynamicClassLoaderManager dclm = mock(DynamicClassLoaderManager.class);

        final CacheKey cacheKey = mockCacheKey();
        final HttpCacheConfig cacheConfig;

        if(knows){
            cacheConfig = mockCacheStore(cacheKey);
        }else{
            cacheConfig = mockCacheStore();
        }

        final InvalidateByCacheConfigVisitor visitor = new InvalidateByCacheConfigVisitor(11, delta, cacheConfig, dclm);
        final InvalidateByCacheConfigVisitor spy = spy(visitor);

        when(spy, "getCacheKey", any(Node.class)).thenReturn(mockCacheKey());

        return spy;
    }

    private HttpCacheConfig mockCacheStore() throws HttpCacheKeyCreationException
    {
        final HttpCacheConfig config = mock(HttpCacheConfig.class);

        when(config.knows(any(CacheKey.class))).thenReturn(false);

        return config;
    }

    private HttpCacheConfig mockCacheStore(final CacheKey key) throws HttpCacheKeyCreationException
    {
        final HttpCacheConfig config = mock(HttpCacheConfig.class);

        when(config.knows(any(CacheKey.class))).thenReturn(true);

        return config;
    }

    private CacheKey mockCacheKey(){
        final CacheKey key = mock(CacheKey.class);
        return key;
    }
}
