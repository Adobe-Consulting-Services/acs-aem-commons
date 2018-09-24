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
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.adobe.acs.commons.httpcache.store.jcr.impl.visitor.mock.RootNodeMockFactory;

@PrepareForTest({EntryNodeMapVisitor.class})
@RunWith(PowerMockRunner.class)
public class TotalCacheSizeVisitorTest
{
    private static final long TEST_FILE_SIZE_WIN = 63 + 4;
    private static final long TEST_FILE_SIZE_POSIX = 63 + 2;

    @Test public void test() throws Exception
    {
        final RootNodeMockFactory.Settings settings = new RootNodeMockFactory.Settings();
        settings.setEntryNodeCount(10);
        settings.setExpiredEntryNodeCount(20);
        settings.setEnableCacheEntryBinaryContent(true);

        final Node rootNode = new RootNodeMockFactory(settings).build();
        final TotalCacheSizeVisitor visitor = getMockedExpiredNodesVisitor();
        visitor.visit(rootNode);

        long winSize = TEST_FILE_SIZE_WIN * 30;
        long posixSize = TEST_FILE_SIZE_POSIX * 30;
        long actualSize = visitor.getBytes();
        if (actualSize != winSize && actualSize != posixSize) {
            throw new Exception("File size was not the expected size, expected " + posixSize + " but was " + actualSize);
        }
    }

    public TotalCacheSizeVisitor getMockedExpiredNodesVisitor()
    {
        final TotalCacheSizeVisitor visitor = new TotalCacheSizeVisitor();
        return visitor;
    }
}
