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
package com.adobe.acs.commons.httpcache.store.jcr.impl.writer;

import static com.adobe.acs.commons.httpcache.store.jcr.impl.writer.BucketNodeFactory.HASHCODE_LENGTH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.jcr.impl.exceptions.BucketNodeFactoryException;

@RunWith(MockitoJUnitRunner.class)
public class BucketNodeFactoryTest
{

    @Mock Session session;
    @Mock Node cacheRootNode;

    @Test(expected = BucketNodeFactoryException.class)
    public void testInvalidPath() throws BucketNodeFactoryException, RepositoryException
    {
        final MockSettings settings = new MockSettings();
        settings.cacheRootPath = "/some/non/existing/path";
        settings.cacheKeyHashCode = 1002021887;
        settings.bucketNodeDepth = 3;
        final BucketNodeFactory factory = buildNodeFactoryWithMocks(settings);

        factory.getBucketNode();
    }

    @Test
    public void test3() throws RepositoryException, BucketNodeFactoryException
    {
        final MockSettings settings = new MockSettings();
        settings.cacheKeyHashCode = 1002021887;
        settings.bucketNodeDepth = 3;
        settings.cacheRootPath = MockSettings.VALID_ROOT_PATH;
        final BucketNodeFactory factory = buildNodeFactoryWithMocks(settings);

        final Node bucketNode = factory.getBucketNode();
        assertEquals("00087", bucketNode.getName());
        assertEquals("00218", bucketNode.getParent().getName());
        assertEquals("01002", bucketNode.getParent().getParent().getName());
    }

    @Test
    public void test4() throws RepositoryException, BucketNodeFactoryException
    {
        final MockSettings settings = new MockSettings();
        settings.cacheKeyHashCode = 1002021887;
        settings.bucketNodeDepth = 4;
        settings.cacheRootPath = MockSettings.VALID_ROOT_PATH;
        final BucketNodeFactory factory = buildNodeFactoryWithMocks(settings);

        final Node bucketNode = factory.getBucketNode();
        assertEquals("00007", bucketNode.getName());
        assertEquals("00188", bucketNode.getParent().getName());
        assertEquals("00202", bucketNode.getParent().getParent().getName());
        assertEquals("00100", bucketNode.getParent().getParent().getParent().getName());
    }


    @Test
    public void test10() throws RepositoryException, BucketNodeFactoryException
    {
        final MockSettings settings = new MockSettings();
        settings.cacheKeyHashCode = 1002021887;
        settings.bucketNodeDepth = 10;
        settings.cacheRootPath = MockSettings.VALID_ROOT_PATH;
        final BucketNodeFactory factory = buildNodeFactoryWithMocks(settings);

        final Node bucketNode = factory.getBucketNode();
        assertEquals("00007", bucketNode.getName());
        assertEquals("00008", bucketNode.getParent().getName());
        assertEquals("00008", bucketNode.getParent().getParent().getName());
        assertEquals("00001", bucketNode.getParent().getParent().getParent().getName());
        assertEquals("00002", bucketNode.getParent().getParent().getParent().getParent().getName());
        assertEquals("00000", bucketNode.getParent().getParent().getParent().getParent().getParent().getName());
        assertEquals("00002", bucketNode.getParent().getParent().getParent().getParent().getParent().getParent().getName());
        assertEquals("00000", bucketNode.getParent().getParent().getParent().getParent().getParent().getParent().getParent().getName());
        assertEquals("00000", bucketNode.getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().getName());
        assertEquals("00001", bucketNode.getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().getName());
    }

    private BucketNodeFactory buildNodeFactoryWithMocks(MockSettings settings)
            throws RepositoryException, BucketNodeFactoryException
    {
        final CacheKey cacheKey = mockCacheKey(settings);

        when(session.nodeExists(MockSettings.VALID_ROOT_PATH)).thenReturn(true);
        when(session.getNode(MockSettings.VALID_ROOT_PATH)).thenReturn(cacheRootNode);

        final BucketNodeFactory bucketNodeFactory = new BucketNodeFactory(session, settings.cacheRootPath, cacheKey, settings.bucketNodeDepth);

        final int hashCode = cacheKey.hashCode();

        if(hashCode > 0){
            final String hashString = StringUtils.leftPad(String.valueOf(hashCode), (int)HASHCODE_LENGTH, "0");
            final int increment = (int) Math.ceil(HASHCODE_LENGTH / settings.bucketNodeDepth);
            final String[] pathArray = new String[settings.bucketNodeDepth];

            for(int position = 0, i = 0; i < settings.bucketNodeDepth; position += increment, i++){
                int endIndex = (position + increment > hashString.length()) ? hashString.length() : position + increment;
                String nodeName =  StringUtils.leftPad(hashString.substring(position, endIndex), 5, "0");
                pathArray[i] = nodeName;
            }

            Node targetNode = cacheRootNode;

            for(String nodeName : pathArray){
                Node childNode = mock(Node.class);
                when(childNode.getName()).thenReturn(nodeName);
                when(targetNode.hasNode(nodeName)).thenReturn(true);
                when(targetNode.getNode(nodeName)).thenReturn(childNode);
                when(childNode.getParent()).thenReturn(targetNode);


                targetNode = childNode;
            }
        }

        return bucketNodeFactory;
    }

    private CacheKey mockCacheKey(final MockSettings mockSettings){
        return new CacheKey()

        {
            @Override
            public String getUri()
            {
                return mockSettings.cacheKeyURI;
            }

            @Override
            public String getHierarchyResourcePath()
            {
                return mockSettings.cacheKeyHierarchyResourcePath;
            }

            @Override
            public long getExpiryForCreation() {
                return -1;
            }

            @Override
            public long getExpiryForAccess() {
                return -1;
            }

            @Override
            public long getExpiryForUpdate() {
                return -1;
            }

            @Override
            public boolean isInvalidatedBy(CacheKey cacheKey)
            {
                return false;
            }

            @Override
            public int hashCode(){
                return mockSettings.cacheKeyHashCode;
            }

            @Override
            public boolean equals(Object obj) {
                return super.equals(obj);
            }
        };
    }

    private static class MockSettings{

        static final String VALID_ROOT_PATH = "/etc/acs-commons/jcr-cache";
        int bucketNodeDepth;
        int cacheKeyHashCode;
        String cacheRootPath;
        String cacheKeyURI;
        String cacheKeyHierarchyResourcePath;


    }

}
