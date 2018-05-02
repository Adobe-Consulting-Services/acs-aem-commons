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
