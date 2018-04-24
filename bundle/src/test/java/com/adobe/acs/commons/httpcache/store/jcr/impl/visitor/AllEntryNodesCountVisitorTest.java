package com.adobe.acs.commons.httpcache.store.jcr.impl.visitor;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.adobe.acs.commons.httpcache.store.jcr.impl.visitor.mock.RootNodeMockFactory;

@PrepareForTest({EntryNodeMapVisitor.class})
@RunWith(PowerMockRunner.class)
public class AllEntryNodesCountVisitorTest
{
    @Test
    public void test() throws IOException, RepositoryException
    {
        final AllEntryNodesCountVisitor visitor = new AllEntryNodesCountVisitor(11);

        final RootNodeMockFactory.Settings settings = new RootNodeMockFactory.Settings();
        settings.setEntryNodeCount(10);
        final Node rootNode = new RootNodeMockFactory(settings).build();

        visitor.visit(rootNode);

        assertEquals(10, visitor.getTotalEntryNodeCount());
    }

    @Test
    public void testWithExpiredEntries() throws IOException, RepositoryException
    {
        final AllEntryNodesCountVisitor visitor = new AllEntryNodesCountVisitor(11);

        final RootNodeMockFactory.Settings settings = new RootNodeMockFactory.Settings();
        settings.setEntryNodeCount(20);
        settings.setExpiredEntryNodeCount(10);
        final Node rootNode = new RootNodeMockFactory(settings).build();

        visitor.visit(rootNode);

        assertEquals(30, visitor.getTotalEntryNodeCount());
    }

    @Test
    public void testEmpty() throws IOException, RepositoryException
    {
        final AllEntryNodesCountVisitor visitor = new AllEntryNodesCountVisitor(11);

        final RootNodeMockFactory.Settings settings = new RootNodeMockFactory.Settings();
        settings.setEntryNodeCount(0);
        settings.setExpiredEntryNodeCount(0);
        final Node rootNode = new RootNodeMockFactory(settings).build();

        visitor.visit(rootNode);

        assertEquals(0, visitor.getTotalEntryNodeCount());
    }



}
