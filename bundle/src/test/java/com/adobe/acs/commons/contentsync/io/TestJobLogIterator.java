/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
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
package com.adobe.acs.commons.contentsync.io;

import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.Resource;

import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestJobLogIterator {
    @Rule
    public AemContext context = new AemContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    private Resource rootResource;

    @Before
    public void setUp() {
        String logPath = "/var/log";
        context.build()
                .resource(logPath)
                .resource(logPath + "/0", JobLogWriter.DATA_PROPERTY, new String[]{"msg1", "msg2"})
                .resource(logPath + "/1", JobLogWriter.DATA_PROPERTY, new String[]{"msg3"})
        ;
        rootResource = context.resourceResolver().getResource(logPath);
    }

    @Test
    public void testIteratesOverShards() {
        JobLogIterator it = new JobLogIterator(rootResource, 2);

        assertTrue(it.hasNext());
        String[] first = it.next();
        assertArrayEquals(new String[]{"msg1", "msg2"}, first);

        assertTrue(it.hasNext());
        String[] second = it.next();
        assertArrayEquals(new String[]{"msg3"}, second);

        assertFalse(it.hasNext());
    }

    @Test
    public void testNextWithoutHasNextThrows() {
        JobLogIterator it = new JobLogIterator(rootResource, 2);
        // hasNext() not called, so shardNode is null
        assertThrows(IllegalStateException.class, it::next);
    }

    @Test
    public void testMultipleHasNextSkipsShards() {
        JobLogIterator it = new JobLogIterator(rootResource, 2);

        assertTrue(it.hasNext());
        assertTrue(it.hasNext()); // advances again, skipping a shard
        // Now next() returns the second shard, not the first
        String[] result = it.next();
        assertArrayEquals(new String[]{"msg3"}, result);
    }

    @Test
    public void testNoSuchElementIfNoMoreShards() {
        JobLogIterator it = new JobLogIterator(rootResource, 2);

        assertTrue(it.hasNext());
        it.next();
        assertTrue(it.hasNext());
        it.next();
        assertFalse(it.hasNext());
        assertThrows(IllegalStateException.class, it::next);
    }
}