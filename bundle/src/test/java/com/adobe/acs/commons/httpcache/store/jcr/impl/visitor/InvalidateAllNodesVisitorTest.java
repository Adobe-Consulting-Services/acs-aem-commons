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
public class InvalidateAllNodesVisitorTest
{
    @Test public void test() throws Exception
    {
        final RootNodeMockFactory.Settings settings = new RootNodeMockFactory.Settings();
        settings.setEntryNodeCount(10);
        settings.setExpiredEntryNodeCount(20);

        final Node rootNode = new RootNodeMockFactory(settings).build();
        final InvalidateAllNodesVisitor visitor = getMockedExpiredNodesVisitor(8);
        visitor.visit(rootNode);
        visitor.close();

        //validate 40 evictions. 10 entries, 20 expired entries, 10 bucket nodes should be removed.
        assertEquals(40, visitor.getEvictionCount());

        //validate 5 saves. 40 divided by 8 equals 5.
        Mockito.verify(rootNode.getSession(), Mockito.times(5)).save();
    }

    @Test public void testWithEmptyBuckets() throws Exception
    {
        final RootNodeMockFactory.Settings settings = new RootNodeMockFactory.Settings();
        settings.setEntryNodeCount(10);
        settings.setExpiredEntryNodeCount(20);
        settings.setEmptyBucketNodeChainCount(2);
        final Node rootNode = new RootNodeMockFactory(settings).build();
        final InvalidateAllNodesVisitor visitor = getMockedExpiredNodesVisitor(4);
        visitor.visit(rootNode);
        visitor.close();

        //validate 60 evictions. 10 entries, 20 expired entries, 30 bucket nodes should be removed.
        assertEquals(60, visitor.getEvictionCount());

        Mockito.verify(rootNode.getSession(), Mockito.times(12)).save();
    }

    public InvalidateAllNodesVisitor getMockedExpiredNodesVisitor(int deltaSaveThreshold)
    {
        final InvalidateAllNodesVisitor visitor = new InvalidateAllNodesVisitor(11, deltaSaveThreshold);
        return visitor;
    }
}
