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
package com.adobe.acs.commons.httpcache.store.jcr.impl.writer;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

import com.adobe.acs.commons.httpcache.store.jcr.impl.JCRHttpCacheStoreConstants;
import com.day.cq.commons.jcr.JcrConstants;

@RunWith(MockitoJUnitRunner.class)
public final class EntryNodeWriterTest {

    private static final String CACHE_CONTENT_LOCATION = "cachecontent.html";
    
    
    Clock clock = Clock.systemUTC();

    private EntryNodeWriterMocks.MockArguments arguments;
    private final InputStream inputStream = getClass().getResourceAsStream(CACHE_CONTENT_LOCATION);

    @Before
    public void setUp() {
        arguments = new EntryNodeWriterMocks.MockArguments();
        arguments.cacheContentCharEncoding = "UTF-8";
        arguments.cacheContentType = "text/html";
        arguments.entryNode = mock(Node.class);
        arguments.cacheContent = inputStream;

        final List<String> header1Value = Arrays.asList("header-value");
        final List<String> header2Value = Arrays.asList("another-header-value");
        arguments.cacheContentHeaders.put("some-header", header1Value);
        arguments.cacheContentHeaders.put("another-header", header2Value);
    }

    @Test
    public void testValid() throws IOException, RepositoryException {
        final EntryNodeWriterMocks mocks = new EntryNodeWriterMocks(arguments);
        mocks.getEntryNodeWriter().write();

        verify(mocks.getEntryNode(), times(1))
                .setProperty(startsWith(JCRHttpCacheStoreConstants.PN_CACHEKEY), any(Binary.class));

        final Node jcrContentNode = mocks.getJcrContentNode();
        final ArgumentCaptor<Binary> argumentCaptor = ArgumentCaptor.forClass(Binary.class);
        verify(jcrContentNode, times(1))
               .setProperty(startsWith(JcrConstants.JCR_DATA), argumentCaptor.capture());
        verify(jcrContentNode, times(1))
                .setProperty(JcrConstants.JCR_MIMETYPE, arguments.cacheContentType);
    }

    @Test
    public void skip_setExpireTime_populateCacheKey() throws IOException, RepositoryException {
        when(arguments.entryNode.hasProperty(JCRHttpCacheStoreConstants.PN_CACHEKEY)).thenReturn(true);
        final EntryNodeWriterMocks mocks = new EntryNodeWriterMocks(arguments, 0);
        mocks.getEntryNodeWriter().write();

        verify(mocks.getEntryNode(), times(0))
                .setProperty(anyString(), any(Binary.class));
    }

    @Test
    public void testGetOrCreateByPath() throws RepositoryException {
        final EntryNodeWriter writer = new EntryNodeWriter(null, null, null, null, 0, clock);
        final String path = "/some/path";
        final Node baseNode = mock(Node.class);
        when(baseNode.hasNode(path)).thenReturn(true);
        final Node childNode = mock(Node.class);
        when(baseNode.getNode(path)).thenReturn(childNode);
        assertEquals(childNode, writer.getOrCreateByPath(baseNode, path, null, null));
    }

}
