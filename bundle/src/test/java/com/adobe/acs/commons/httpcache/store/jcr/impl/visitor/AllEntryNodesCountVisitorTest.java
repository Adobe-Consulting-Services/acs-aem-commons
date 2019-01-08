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
