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
package com.adobe.acs.commons.httpcache.store.jcr.impl.visitor.mock;

import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.doCallRealMethod;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jcr.Binary;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.collections.IteratorUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.AbstractNode;
import org.apache.jackrabbit.value.BinaryImpl;
import org.apache.sling.commons.testing.jcr.MockNodeIterator;
import org.apache.sling.commons.testing.jcr.MockProperty;
import org.apache.sling.commons.testing.jcr.MockPropertyIterator;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.adobe.acs.commons.httpcache.store.jcr.impl.JCRHttpCacheStoreConstants;

public class RootNodeMockFactory
{
    public static final String ROOT_PATH = "/etc/acs-commons/httpcache/root";

    private static final String TEST_FILE_PATH = "cachecontent.html";

    private final Settings settings;

    private final Session session = mock(Session.class);

    public RootNodeMockFactory(final Settings settings){
        this.settings = settings;
    }

    public Node build() throws RepositoryException, IOException
    {
        final Node rootNode = mockStandardNode("rootnode");

        Node[] bucketNodeChain = generateBucketNodeChain(rootNode, false);

        Node parentNode = bucketNodeChain[bucketNodeChain.length - 1];
        final NodeIterator entryNodes = generateEntryNodes(parentNode);
        when(parentNode.getNodes()).thenReturn(entryNodes);


        Node[] level1nodes = new Node[1 + settings.emptyBucketNodeChainCount];
        level1nodes[0] = bucketNodeChain[0];

        for(int i =0;i<settings.emptyBucketNodeChainCount;i++){
            Node[] emptyBucketNodeChain = generateBucketNodeChain(rootNode, true);
            when(emptyBucketNodeChain[emptyBucketNodeChain.length - 1].getNodes()).thenReturn(new MockNodeIterator());
            level1nodes[i+1] = emptyBucketNodeChain[0];
        }

        when(rootNode.getNodes()).thenReturn(new MockNodeIterator(level1nodes));
        when(rootNode.getProperties()).thenReturn(new MockPropertyIterator(IteratorUtils.EMPTY_ITERATOR));

        return rootNode;
    }

    private Node[] generateBucketNodeChain(Node rootNode, boolean isEmpty) throws RepositoryException
    {
        final Node[] bucketNodeChain = new Node[settings.bucketDepth];
        Node currentParentNode = rootNode;
        for(int i=0;i<settings.bucketDepth;i++){
            final Node node = mockStandardNode("bucketnode" + (isEmpty ? "-empty" : "") + "-level-" + (i + 1));
            bucketNodeChain[i] = node;

            when(node.getParent()).thenReturn(currentParentNode);
            when(node.getProperties()).thenReturn(new MockPropertyIterator(IteratorUtils.EMPTY_ITERATOR));
            when(node.hasProperty(JCRHttpCacheStoreConstants.PN_ISCACHEENTRYNODE)).thenReturn(false);

            currentParentNode = node;
        }

        for(int i=0;i<settings.bucketDepth;i++){
            if(i < settings.bucketDepth){
                final Node node = bucketNodeChain[i];
                final Node childNode = bucketNodeChain[i];
                final AtomicInteger deleteCounter = new AtomicInteger();

                when(node.getParent().getNodes()).thenAnswer(new Answer<NodeIterator>(){
                    @Override public NodeIterator answer(InvocationOnMock invocationOnMock) throws Throwable
                    {
                        if(deleteCounter.get() > 0){
                            return new MockNodeIterator();
                        }else{
                            return new MockNodeIterator(new Node[]{ childNode });
                        }
                    }
                });

            }
        }


        return bucketNodeChain;
    }

    private NodeIterator generateEntryNodes(final Node parentNode) throws RepositoryException, IOException
    {
        int totalNodeCount = settings.entryNodeCount + settings.expiredEntryNodeCount;
        final Node[] nodes = new AbstractNode[totalNodeCount];

        int i=0;
        for(;i<settings.entryNodeCount;i++){
            final Node randomNode = mockEntryNode(parentNode, i, false);
            nodes[i] = randomNode;
        }

        for(;i<totalNodeCount;i++){
            final Node randomNode = mockEntryNode(parentNode, i, true);
            nodes[i] = randomNode;
        }

        return new MockNodeIterator(nodes);
    }

    private Node mockEntryNode(Node parentNode, int i, boolean isExpired) throws RepositoryException, IOException
    {

        final String nodeName = (isExpired) ? "expired-entrynode-" : "entrynode-";
        final Node entryNode = mockStandardNode(nodeName + (i + 1));

        if(settings.enableCacheEntryBinaryContent){
            mockEntryContentNode(entryNode);
        }

        when(entryNode.hasProperty(JCRHttpCacheStoreConstants.PN_ISCACHEENTRYNODE)).thenReturn(true);

        final MockProperty expiresMockProperty = new MockProperty(JCRHttpCacheStoreConstants.PN_EXPIRES_ON);

        int seconds;
        if(isExpired){
            seconds = -9000;
        }else{
            seconds = 9000;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND,  seconds);
        expiresMockProperty.setValue(calendar);

        return entryNode;
    }

    private void mockEntryContentNode(Node entryNode) throws RepositoryException, IOException
    {
        final Node contentNode = mock(Node.class);
        final Node jcrContentNode = mock(Node.class);
        final Property dataProperty = mock(Property.class);

        InputStream cacheTestStream = getClass().getResourceAsStream(TEST_FILE_PATH);

        final Binary binary = new BinaryImpl(cacheTestStream);

        when(dataProperty.getBinary()).thenReturn(binary);

        when(jcrContentNode.getProperty(JcrConstants.JCR_DATA)).thenReturn(dataProperty);
        when(jcrContentNode.hasProperty(JcrConstants.JCR_DATA)).thenReturn(true);
        when(contentNode.getNode(JcrConstants.JCR_CONTENT)).thenReturn(jcrContentNode);
        when(contentNode.hasNode(JcrConstants.JCR_CONTENT)).thenReturn(true);
        when(entryNode.getNode(JCRHttpCacheStoreConstants.PATH_CONTENTS)).thenReturn(contentNode);
        when(entryNode.hasNode(JCRHttpCacheStoreConstants.PATH_CONTENTS)).thenReturn(true);
    }

    private Node mockStandardNode(String name) throws RepositoryException
    {
        final Node node = mock(AbstractNode.class);
        when(node.getSession()).thenReturn(session);
        doCallRealMethod().when(node).accept(any(ItemVisitor.class));

        return node;
    }

    public static class Settings{
        private int entryNodeCount            = 10;
        private int bucketDepth               = 10;
        private int expiredEntryNodeCount     = 0;
        private int emptyBucketNodeChainCount = 0;
        private boolean enableCacheEntryBinaryContent = false;

        public void setEntryNodeCount(int entryNodeCount)
        {
            this.entryNodeCount = entryNodeCount;
        }

        public void setBucketDepth(int bucketDepth)
        {
            this.bucketDepth = bucketDepth;
        }

        public void setExpiredEntryNodeCount(int expiredEntryNodeCount)
        {
            this.expiredEntryNodeCount = expiredEntryNodeCount;
        }

        public void setEmptyBucketNodeChainCount(int emptyBucketNodeChainCount)
        {
            this.emptyBucketNodeChainCount = emptyBucketNodeChainCount;
        }

        public void setEnableCacheEntryBinaryContent(boolean enableCacheEntryBinaryContent)
        {
            this.enableCacheEntryBinaryContent = enableCacheEntryBinaryContent;
        }
    }
}
