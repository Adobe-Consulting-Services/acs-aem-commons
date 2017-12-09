package com.adobe.acs.commons.httpcache.store.jcr.impl.visitor.mock;

import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.doCallRealMethod;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.concurrent.atomic.AtomicInteger;

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.collections.IteratorUtils;
import org.apache.jackrabbit.commons.AbstractNode;
import org.apache.sling.commons.testing.jcr.MockNodeIterator;
import org.apache.sling.commons.testing.jcr.MockProperty;
import org.apache.sling.commons.testing.jcr.MockPropertyIterator;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.adobe.acs.commons.httpcache.store.jcr.impl.JCRHttpCacheStoreConstants;

public class RootNodeMockFactory
{
    public static final String ROOT_PATH = "/etc/acs-commons/httpcache/root";
    private final Settings settings;

    private final Session session = mock(Session.class);

    public RootNodeMockFactory(final Settings settings){
        this.settings = settings;
    }
    public Node build() throws RepositoryException
    {
        final Node rootNode = mockStandardNode("rootnode");

        when(rootNode.getPath()).thenReturn(ROOT_PATH);
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

                doAnswer(new Answer<Object>()
                {
                    @Override public Object answer(InvocationOnMock invocationOnMock) throws Throwable
                    {
                        deleteCounter.getAndIncrement();
                        return null;
                    }
                }).when(node).remove();

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

    private NodeIterator generateEntryNodes(final Node parentNode) throws RepositoryException
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

    private Node mockEntryNode(Node parentNode, int i, boolean isExpired) throws RepositoryException
    {

        final String nodeName = (isExpired) ? "expired-entrynode-" : "entrynode-";
        final Node entryNode = mockStandardNode(nodeName + (i + 1));

        when(entryNode.hasProperty(JCRHttpCacheStoreConstants.PN_ISCACHEENTRYNODE)).thenReturn(true);
        when(entryNode.hasProperty(JCRHttpCacheStoreConstants.PN_EXPIRES_ON)).thenReturn(true);
        when(entryNode.getNodes()).thenReturn(new MockNodeIterator());
        when(entryNode.getProperties()).thenReturn(new MockPropertyIterator(IteratorUtils.EMPTY_ITERATOR));
        when(entryNode.getParent()).thenReturn(parentNode);

        final MockProperty expiresMockProperty = new MockProperty(JCRHttpCacheStoreConstants.PN_EXPIRES_ON);
        if(isExpired){
            expiresMockProperty.setValue(System.currentTimeMillis() - 90000);
        }else{
            expiresMockProperty.setValue(System.currentTimeMillis() + 90000);
        }

        when(entryNode.getProperty(JCRHttpCacheStoreConstants.PN_EXPIRES_ON)).thenReturn(expiresMockProperty);
        return entryNode;
    }

    private Node mockStandardNode(String name) throws RepositoryException
    {
        final Node node = mock(AbstractNode.class);
        when(node.getSession()).thenReturn(session);
        when(node.toString()).thenReturn(name);
        doCallRealMethod().when(node).accept(any(ItemVisitor.class));

        when(node.hasNodes()).thenAnswer(new Answer<Boolean>()
        {
            @Override public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                return node.getNodes().getSize() > 0;
            }
        });

        return node;
    }

    public static class Settings{
        private int entryNodeCount            = 10;
        private int bucketDepth               = 10;
        private int expiredEntryNodeCount     = 0;
        private int emptyBucketNodeChainCount = 0;

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
    }
}
