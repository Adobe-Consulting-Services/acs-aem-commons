package com.adobe.acs.commons.httpcache.store.jcr.impl.visitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.jcr.impl.visitor.mock.RootNodeMockFactory;

@PrepareForTest({EntryNodeByStringKeyVisitor.class})
@RunWith(PowerMockRunner.class)
public class EntryNodeByStringKeyVisitorTest
{
    @Test
    public void testPresent() throws Exception
    {
        final RootNodeMockFactory.Settings settings = new RootNodeMockFactory.Settings();
        settings.setEntryNodeCount(10);
        final Node rootNode = new RootNodeMockFactory(settings).build();
        final EntryNodeByStringKeyVisitor visitor = getMockedEntryNodeByStringKeyVisitor("[resourcePath: /content/some/path]", true);

        visitor.visit(rootNode);

        assertNotNull(visitor.getCacheContentIfPresent());
    }

    @Test
    public void testNotPresent() throws Exception
    {
        final RootNodeMockFactory.Settings settings = new RootNodeMockFactory.Settings();
        settings.setEntryNodeCount(10);
        final Node rootNode = new RootNodeMockFactory(settings).build();
        final EntryNodeByStringKeyVisitor visitor = getMockedEntryNodeByStringKeyVisitor("[resourcePath: /content/some/path]", false);

        visitor.visit(rootNode);

        assertNull(visitor.getCacheContentIfPresent());
    }

    private EntryNodeByStringKeyVisitor getMockedEntryNodeByStringKeyVisitor(String cacheKeyStr, boolean match) throws Exception
    {
        final DynamicClassLoaderManager dclm = mock(DynamicClassLoaderManager.class);
        final EntryNodeByStringKeyVisitor visitor = new EntryNodeByStringKeyVisitor(11, dclm, cacheKeyStr);

        final EntryNodeByStringKeyVisitor spy = spy(visitor);
        final CacheKey cacheKey = mock(CacheKey.class);

        if(match){
            when(cacheKey.toString()).thenReturn(cacheKeyStr);
        }else{
            when(cacheKey.toString()).thenReturn(RandomStringUtils.random(10000));
        }

        when(spy, "getCacheKey", any(Node.class)).thenReturn(cacheKey);

        return spy;
    }

}
