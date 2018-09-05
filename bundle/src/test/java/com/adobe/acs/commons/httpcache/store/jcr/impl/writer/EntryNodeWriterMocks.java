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
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.value.BinaryImpl;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.jcr.impl.CacheKeyMock;
import com.adobe.acs.commons.httpcache.store.jcr.impl.JCRHttpCacheStoreConstants;
import com.adobe.acs.commons.httpcache.store.mem.impl.MemTempSinkImpl;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;

public class EntryNodeWriterMocks
{

    private Session session;

    private CacheKey cacheKey;

    private CacheContent cacheContent;

    private final EntryNodeWriter entryNodeWriter;

    private Node entryNode;

    private Node contentNode;

    private Node jcrContentNode;

    private Node headersNode;

    private final MockArguments arguments;

    public static class MockArguments{
        Node entryNode;
        int expiryTime;
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

    public EntryNodeWriterMocks(MockArguments arguments) throws RepositoryException
    {
        this.arguments = arguments;
        entryNode = arguments.entryNode;
        mockContentNode();
        mockHeadersNode();
        mockSession();
        mockCacheKey();
        mockCacheContent();

        mockStatic(JcrUtil.class);
        mockJCRUtil();

        final EntryNodeWriter writer = new EntryNodeWriter(session, entryNode, cacheKey,  cacheContent, arguments.expiryTime);
        entryNodeWriter = spy(writer);
    }

    private void mockHeadersNode()
    {
        headersNode = mock(Node.class);
    }

    private void mockContentNode()
    {
        contentNode = mock(Node.class);
        jcrContentNode = mock(Node.class);
    }

    public Session getSession()
    {
        return session;
    }

    public CacheKey getCacheKey()
    {
        return cacheKey;
    }

    public CacheContent getCacheContent()
    {
        return cacheContent;
    }

    public EntryNodeWriter getEntryNodeWriter()
    {
        return entryNodeWriter;
    }

    public Node getEntryNode()
    {
        return entryNode;
    }

    public Node getContentNode()
    {
        return contentNode;
    }

    public Node getJcrContentNode()
    {
        return jcrContentNode;
    }

    public Node getHeadersNode()
    {
        return headersNode;
    }

    private void mockJCRUtil() throws RepositoryException
    {
        when(
                JcrUtils.getOrCreateByPath(arguments.entryNode, JCRHttpCacheStoreConstants.PATH_CONTENTS, false, JcrConstants.NT_FILE, JcrConstants.NT_FILE, false))
                .thenAnswer(
                        new Answer<Node>(){
                            @Override public Node answer(InvocationOnMock invocationOnMock) throws Throwable {
                                return contentNode;
                            }
                        }
        );

        when(
                JcrUtils.getOrCreateByPath(contentNode, JcrConstants.JCR_CONTENT, false, JcrConstants.NT_RESOURCE, JcrConstants.NT_RESOURCE, false))
                .thenAnswer(new Answer<Node>()
                {
                    @Override public Node answer(InvocationOnMock invocationOnMock) throws Throwable
                    {
                        return jcrContentNode;
                    }
                }
        );

        when(
                JcrUtils.getOrCreateByPath(entryNode, JCRHttpCacheStoreConstants.PATH_HEADERS, false, OAK_UNSTRUCTURED, OAK_UNSTRUCTURED, false))
                .thenAnswer(new Answer<Node>()
                            {
                                @Override public Node answer(InvocationOnMock invocationOnMock) throws Throwable
                                {
                                    return headersNode;
                                }
                            }
                );

    }

    private void mockSession() throws RepositoryException
    {
        session = mock(Session.class);
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
        when(cacheContent.getTempSink()).thenReturn(new MemTempSinkImpl());
    }
}
