package com.adobe.acs.commons.httpcache.store.jcr.impl;

import static com.adobe.acs.commons.httpcache.store.jcr.impl.writer.BucketNodeFactory.HASHCODE_LENGTH;
import static org.junit.Assert.assertEquals;
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
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.jcr.impl.exceptions.BucketNodeFactoryException;
import com.adobe.acs.commons.httpcache.store.jcr.impl.writer.BucketNodeFactory;

@RunWith(MockitoJUnitRunner.class)
public class BucketNodeFactoryTest
{

    @Mock Session session;
    @Mock Node cacheRootNode;

    @Before
    public void setup() throws Exception{
        when(session.isLive()).thenReturn(true);
        when(cacheRootNode.getPath()).thenReturn(MockSettings.VALID_ROOT_PATH);
    }

    @Test(expected = BucketNodeFactoryException.class)
    public void testInvalidPath() throws BucketNodeFactoryException, RepositoryException
    {
        final MockSettings settings = new MockSettings();
        settings.cacheRootPath = "/some/non/existing/path";
        settings.cacheKeyHashCode = 1002021887;
        settings.cacheSplitDepth = 3;
        final BucketNodeFactory factory = buildNodeFactoryWithMocks(settings);

        factory.getBucketNode();
    }

    @Test
    public void test3() throws RepositoryException, BucketNodeFactoryException
    {
        final MockSettings settings = new MockSettings();
        settings.cacheKeyHashCode = 1002021887;
        settings.cacheSplitDepth = 3;
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
        settings.cacheSplitDepth = 4;
        settings.cacheRootPath = MockSettings.VALID_ROOT_PATH;
        final BucketNodeFactory factory = buildNodeFactoryWithMocks(settings);

        final Node bucketNode = factory.getBucketNode();
        assertEquals("00007", bucketNode.getName());
        assertEquals("00188", bucketNode.getParent().getName());
        assertEquals("00202", bucketNode.getParent().getParent().getName());
        assertEquals("00100", bucketNode.getParent().getParent().getParent().getName());
    }

    private BucketNodeFactory buildNodeFactoryWithMocks(MockSettings settings)
            throws RepositoryException, BucketNodeFactoryException
    {
        final CacheKey cacheKey = mockCacheKey(settings);

        when(session.nodeExists(MockSettings.VALID_ROOT_PATH)).thenReturn(true);
        when(session.getNode(MockSettings.VALID_ROOT_PATH)).thenReturn(cacheRootNode);

        final BucketNodeFactory bucketNodeFactory = new BucketNodeFactory(session, settings.cacheRootPath, cacheKey, settings.cacheSplitDepth);

        final int hashCode = cacheKey.hashCode();

        if(hashCode > 0){
            final String hashString = StringUtils.leftPad(String.valueOf(hashCode), (int)HASHCODE_LENGTH, "0");
            final int increment = (int) Math.ceil(HASHCODE_LENGTH / settings.cacheSplitDepth);
            final String[] pathArray = new String[settings.cacheSplitDepth];

            for(int position = 0, i = 0; i < settings.cacheSplitDepth; position += increment, i++){
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
            @Override public String getUri()
            {
                return mockSettings.cacheKeyURI;
            }

            @Override public String getHierarchyResourcePath()
            {
                return mockSettings.cacheKeyHierarchyResourcePath;
            }

            @Override public boolean isInvalidatedBy(CacheKey cacheKey)
            {
                return false;
            }

            @Override  public int hashCode(){
                return mockSettings.cacheKeyHashCode;
            }
        };
    }

    private static class MockSettings{

        final static String VALID_ROOT_PATH = "/etc/acs-commons/jcr-cache";
        int cacheSplitDepth,cacheKeyHashCode;
        String cacheRootPath, cacheKeyURI, cacheKeyHierarchyResourcePath;


    }

}
