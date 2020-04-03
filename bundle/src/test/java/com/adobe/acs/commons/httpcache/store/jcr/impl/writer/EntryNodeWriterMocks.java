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

import static com.adobe.acs.commons.httpcache.store.jcr.impl.JCRHttpCacheStoreConstants.OAK_UNSTRUCTURED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.value.BinaryImpl;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.engine.HttpCacheServletResponseWrapper;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.jcr.impl.CacheKeyMock;
import com.adobe.acs.commons.httpcache.store.jcr.impl.JCRHttpCacheStoreConstants;
import com.adobe.acs.commons.httpcache.store.mem.impl.MemTempSinkImpl;
import com.day.cq.commons.jcr.JcrConstants;

public final class EntryNodeWriterMocks
{

    private Clock clock = Clock.systemUTC();
    private final Session session = mock(Session.class);

    private CacheKey cacheKey;

    private CacheContent cacheContent;

    private final EntryNodeWriter entryNodeWriter;

    private Node entryNode;

    private final Node contentNode = mock(Node.class);

    private final Node jcrContentNode = mock(Node.class);

    private final Node headersNode = mock(Node.class);

    private final MockArguments arguments;

    public static final class MockArguments {
        Node entryNode;
        int cacheKeyHashCode;
        int status;

        String cacheKeyUri;
        String cacheKeyString;
        String cacheKeyHierarchyResourcePath;
        String cacheContentCharEncoding;
        String cacheContentType;
        Map<String, List<String>> cacheContentHeaders = new HashMap<String, List<String>>();
        InputStream cacheContent;
    }

    public EntryNodeWriterMocks(final MockArguments arguments, final long expireTimeInMilliSeconds) throws RepositoryException
    {
        this.arguments = arguments;
        entryNode = arguments.entryNode;
        mockSession();
        mockCacheKey();
        mockCacheContent();

        final EntryNodeWriter writer = new EntryNodeWriter(session, entryNode, cacheKey,  cacheContent, expireTimeInMilliSeconds, clock);
        entryNodeWriter = spy(writer);

        mockJCRUtil();
    }

    public EntryNodeWriterMocks(final MockArguments arguments) throws RepositoryException
    {
        this(arguments, 1000L);
    }

    public EntryNodeWriter getEntryNodeWriter()
    {
        return entryNodeWriter;
    }

    public Node getEntryNode()
    {
        return entryNode;
    }

    public Node getJcrContentNode()
    {
        return jcrContentNode;
    }

    private void mockJCRUtil() throws RepositoryException
    {
        doReturn(contentNode).when(entryNodeWriter).getOrCreateByPath(arguments.entryNode, JCRHttpCacheStoreConstants.PATH_CONTENTS, JcrConstants.NT_FILE, JcrConstants.NT_FILE);
        doReturn(jcrContentNode).when(entryNodeWriter).getOrCreateByPath(contentNode, JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE, JcrConstants.NT_RESOURCE);
        doReturn(headersNode).when(entryNodeWriter).getOrCreateByPath(entryNode, JCRHttpCacheStoreConstants.PATH_HEADERS, OAK_UNSTRUCTURED, OAK_UNSTRUCTURED);
    }

    private void mockSession() throws RepositoryException
    {
        final ValueFactory valueFactory = mock(ValueFactory.class);
        when(valueFactory.createBinary(any(InputStream.class))).thenAnswer(new Answer<Binary>()
        {
            @Override public Binary answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                InputStream inputStream = ( InputStream)invocationOnMock.getArguments()[0];
                if(inputStream != null){
                    BinaryImpl binary = new BinaryImpl(inputStream);
                    return binary;
                }
                return null;
            }
        });

        when(session.getValueFactory()).thenReturn(valueFactory);
    }

    private void mockCacheKey()
    {
        cacheKey = new CacheKeyMock(
                arguments.cacheKeyUri,
                arguments.cacheKeyHierarchyResourcePath,
                arguments.cacheKeyHashCode,
                arguments.cacheKeyString
        );
    }

    private void mockCacheContent()
    {
        cacheContent = mock(CacheContent.class);
        when(cacheContent.getCharEncoding()).thenReturn(arguments.cacheContentCharEncoding);
        when(cacheContent.getContentType()).thenReturn(arguments.cacheContentType);
        when(cacheContent.getInputDataStream()).thenReturn(arguments.cacheContent);
        when(cacheContent.getStatus()).thenReturn(arguments.status);
        when(cacheContent.getHeaders()).thenReturn(arguments.cacheContentHeaders);
        when(cacheContent.getWriteMethod()).thenReturn(HttpCacheServletResponseWrapper.ResponseWriteMethod.PRINTWRITER);
    }
}
