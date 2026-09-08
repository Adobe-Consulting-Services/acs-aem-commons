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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.sling.api.resource.AbstractResourceVisitor;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.adobe.acs.commons.contentsync.io.JobLogWriter.DATA_PROPERTY;

import io.wcm.testing.mock.aem.junit.AemContext;


public class TestJobLogWriter {
    @Rule
    public AemContext context = new AemContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    private ResourceResolverFactory resourceResolverFactory;

    @Before
    public void setUp() throws Exception {
        resourceResolverFactory = mock(ResourceResolverFactory.class);
        when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenReturn(context.resourceResolver());
    }


    @Test
    public void testWriteAndFlush() throws Exception {
        JobLogWriter writer = new JobLogWriter(resourceResolverFactory, "/var/logs", 2, -1, 2);

        writer.write("msg1");
        // Should not flush yet (bucket size 2)
        verify(resourceResolverFactory, never()).getServiceResourceResolver(anyMap());

        writer.write("msg2");
        // Should flush now
        verify(resourceResolverFactory, times(1)).getServiceResourceResolver(anyMap());
    }

    @Test
    public void testFlushOnClose() throws Exception {
        JobLogWriter writer = new JobLogWriter(resourceResolverFactory, "/var/logs", 10, 1000, 2);

        writer.write("msg1");
        writer.write("msg2");
        writer.close();

        verify(resourceResolverFactory, atLeastOnce()).getServiceResourceResolver(anyMap());
    }

    @Test
    public void testFlushIfNeededByTimeout() throws Exception {
        JobLogWriter writer = new JobLogWriter(resourceResolverFactory, "/var/logs", 10, 10, 2);

        writer.write("msg1");
        Thread.sleep(20); // Wait for flush interval to pass
        writer.write("msg2");

        verify(resourceResolverFactory, atLeastOnce()).getServiceResourceResolver(anyMap());
    }

    @Test
    public void testFlushDoesNothingIfBufferEmpty() throws Exception {
        JobLogWriter writer = new JobLogWriter(resourceResolverFactory, "/var/logs", 2, 1000, 2);

        writer.flush(); // Should not call resolverFactory
        verify(resourceResolverFactory, never()).getServiceResourceResolver(anyMap());
    }

    @Test
    public void testLoginExceptionIsWrapped() throws Exception {
        when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenThrow(new LoginException("fail"));
        JobLogWriter writer = new JobLogWriter(resourceResolverFactory, "/var/logs", 2, 1000, 2);

        assertThrows(IOException.class, () -> {
            writer.write("msg1");
            writer.write("msg2"); // triggers flush
        });
    }

    @Test
    public void testReadWriteConsistency() throws IOException {
        String jobPath = "/var/log";
        int bucketSize = 2;
        int shardWidth = 2;
        int numMessages = 21;
        try (JobLogWriter writer = new JobLogWriter(resourceResolverFactory, jobPath, bucketSize, -1, shardWidth)) {
            for(int i = 0; i < numMessages; i++){
                String msg = "hello-" + i;
                writer.write(msg);
            }
        }

        Resource logNode = context.resourceResolver().getResource(jobPath);
        List<Resource> shards = new ArrayList<>();
        new AbstractResourceVisitor(){
            @Override
            public void visit(Resource res){
                if(res.getValueMap().containsKey(DATA_PROPERTY)) {
                    shards.add(res);
                }
            }
        }.accept(logNode);
        // 21 messages are distributed in 11 buckets, 10x2 messages and 1 bucked with 1 message
        assertEquals(11, shards.size());

        JobLogIterator it = new JobLogIterator(logNode, shardWidth);
        List<String> readMessages = new ArrayList<>();
        while(it.hasNext()){
            readMessages.addAll(Arrays.asList(it.next()));
        }

        // assert read and write operations match
        for(int i = 0; i < numMessages; i++){
            String msg = "hello-" + i;
            assertEquals(msg, readMessages.get(i));
        }
    }

}