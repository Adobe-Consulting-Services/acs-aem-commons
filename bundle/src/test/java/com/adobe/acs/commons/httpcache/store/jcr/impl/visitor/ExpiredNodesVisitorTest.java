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
public class ExpiredNodesVisitorTest
{
    @Test public void test() throws Exception
    {
        final RootNodeMockFactory.Settings settings = new RootNodeMockFactory.Settings();
        settings.setEntryNodeCount(10);
        settings.setExpiredEntryNodeCount(20);

        final Node rootNode = new RootNodeMockFactory(settings).build();
        final ExpiredNodesVisitor visitor = getMockedExpiredNodesVisitor(8);
        visitor.visit(rootNode);
        visitor.close();
        assertEquals(20, visitor.getEvictionCount());

        Mockito.verify(rootNode.getSession(), Mockito.times(3)).save();
    }

    @Test public void testEmptyBucketNodes() throws Exception
    {
        final RootNodeMockFactory.Settings settings = new RootNodeMockFactory.Settings();
        settings.setEntryNodeCount(10);
        settings.setExpiredEntryNodeCount(20);
        settings.setEmptyBucketNodeChainCount(1);

        final Node rootNode = new RootNodeMockFactory(settings).build();
        final ExpiredNodesVisitor visitor = getMockedExpiredNodesVisitor(8);
        visitor.visit(rootNode);
        visitor.close();
        assertEquals(30, visitor.getEvictionCount());

        Mockito.verify(rootNode.getSession(), Mockito.times(4)).save();
    }

    public ExpiredNodesVisitor getMockedExpiredNodesVisitor(int deltaSaveThreshold)
    {
        final ExpiredNodesVisitor visitor = new ExpiredNodesVisitor(11, deltaSaveThreshold);

        return visitor;
    }
}
