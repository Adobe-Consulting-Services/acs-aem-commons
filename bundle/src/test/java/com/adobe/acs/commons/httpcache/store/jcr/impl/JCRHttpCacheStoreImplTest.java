package com.adobe.acs.commons.httpcache.store.jcr.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.functions.Consumer;
import com.adobe.acs.commons.functions.Function;
import com.adobe.acs.commons.httpcache.store.jcr.impl.mock.JCRHttpCacheStoreMocks;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = {
        "com.adobe.acs.commons.httpcache.store.jcr.impl.*",
        "com.adobe.acs.commons.util.impl.*"
})
public class JCRHttpCacheStoreImplTest
{
    @Test
    public void testPutIntoCache() throws Exception
    {
        JCRHttpCacheStoreMocks.Arguments arguments = new JCRHttpCacheStoreMocks.Arguments();
        final JCRHttpCacheStoreMocks mocks = new JCRHttpCacheStoreMocks(arguments);
        final JCRHttpCacheStoreImpl store = mocks.getStore();
        store.put(mocks.getCacheKey(), mocks.getCacheContent());
        verify(mocks.getLog(), never()).error(anyString(), any(Exception.class));
        verify(mocks.getResourceResolver(), times(1)).close();
        verify(mocks.getSession(), times(1)).save();
        verify(mocks.getEntryNodeWriter(), times(1)).write();
    }

    @Test
    public void testContains() throws Exception{
        JCRHttpCacheStoreMocks.Arguments arguments = new JCRHttpCacheStoreMocks.Arguments();
        final JCRHttpCacheStoreMocks mocks = new JCRHttpCacheStoreMocks(arguments);
        final JCRHttpCacheStoreImpl store = mocks.getStore();
        store.contains(mocks.getCacheKey());
    }

    @Test
    public void testInvalidate() throws Exception{
        JCRHttpCacheStoreMocks.Arguments arguments = new JCRHttpCacheStoreMocks.Arguments();
        final JCRHttpCacheStoreMocks mocks = new JCRHttpCacheStoreMocks(arguments);
        final JCRHttpCacheStoreImpl store = mocks.getStore();
        store.invalidate(mocks.getCacheKey());
    }


}
