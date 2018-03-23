package com.adobe.acs.commons.httpcache.store.jcr.impl.visitor;

import static org.junit.Assert.assertEquals;

import javax.jcr.Node;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.adobe.acs.commons.httpcache.store.jcr.impl.visitor.mock.RootNodeMockFactory;

@PrepareForTest({EntryNodeMapVisitor.class})
@RunWith(PowerMockRunner.class)
public class TotalCacheSizeVisitorTest
{
    private static final long TEST_FILE_SIZE = 65;

    @Test public void test() throws Exception
    {
        final RootNodeMockFactory.Settings settings = new RootNodeMockFactory.Settings();
        settings.setEntryNodeCount(10);
        settings.setExpiredEntryNodeCount(20);
        settings.setEnableCacheEntryBinaryContent(true);

        final Node rootNode = new RootNodeMockFactory(settings).build();
        final TotalCacheSizeVisitor visitor = getMockedExpiredNodesVisitor();
        visitor.visit(rootNode);

        assertEquals(TEST_FILE_SIZE * 30, visitor.getBytes());
    }

    public TotalCacheSizeVisitor getMockedExpiredNodesVisitor()
    {
        final TotalCacheSizeVisitor visitor = new TotalCacheSizeVisitor();
        return visitor;
    }
}
