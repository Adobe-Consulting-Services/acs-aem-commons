package com.adobe.acs.commons.httpcache.store.jcr.impl.mock;

import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.doCallRealMethod;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;

import com.adobe.acs.commons.functions.Consumer;
import com.adobe.acs.commons.functions.Function;
import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.jcr.impl.CacheKeyMock;
import com.adobe.acs.commons.httpcache.store.jcr.impl.JCRHttpCacheStoreImpl;
import com.adobe.acs.commons.httpcache.store.jcr.impl.handler.BucketNodeHandler;
import com.adobe.acs.commons.httpcache.store.jcr.impl.writer.BucketNodeFactory;
import com.adobe.acs.commons.httpcache.store.jcr.impl.writer.EntryNodeWriter;
import com.adobe.acs.commons.httpcache.store.mem.impl.MemTempSinkImpl;

public class JCRHttpCacheStoreMocks
{
    private final Arguments arguments;
    private final JCRHttpCacheStoreImpl store = mock(JCRHttpCacheStoreImpl.class);
    private final DynamicClassLoaderManager dclm = mock(DynamicClassLoaderManager.class);
    private final Session session = mock(Session.class);
    private final ResourceResolverFactory resourceResolverFactory = mock(ResourceResolverFactory.class);
    private final ResourceResolver resourceResolver = mock(ResourceResolver.class);

    private final Node rootNode = mock(Node.class);
    private final Node bucketNode = mock(Node.class);
    private final Node entryNode = mock(Node.class);

    private final CacheKey cacheKey;
    private final CacheContent cacheContent = mock(CacheContent.class);

    private static final Logger log = mock(Logger.class);
    private EntryNodeWriter entryNodeWriter = mock(EntryNodeWriter.class);
    private BucketNodeHandler bucketNodeHandler = mock(BucketNodeHandler.class);
    private BucketNodeFactory factory = mock(BucketNodeFactory.class);

    private final AtomicBoolean resourceResolverOpen = new AtomicBoolean(true);

    public static class Arguments{
        InputStream contents;
        Map<String,List<String>> headers;

        String cacheKeyUri = "https://adobe-consulting-services.github.io/acs-aem-commons/";
        String cacheKeyString = "some/random/string";
        String cacheKeyHierarchyResourcePath = "/content/some/resource/path";
        String cacheContentCharEncoding = "utf-8";
        String cacheContentType = "text/html";

        int cacheKeyHashCode = 1234567890;
        int cacheContentStatus = 200;
    }

    public JCRHttpCacheStoreMocks(final Arguments arguments) throws Exception{
        this.arguments = arguments;
        generateCacheContent();
        mockRepository();
        mockBucketNodeHandler();
        mockBucketNodeFactory();
        mockEntryNodeWriter();
        mockNodeNames();

        cacheKey = generateCacheKey(arguments);
        mockStore();
        mockLogger();
    }

    private void mockNodeNames()
    {
        when(rootNode.toString()).thenReturn("rootnode");
        when(bucketNode.toString()).thenReturn("bucketnode");
    }

    private void mockLogger()
    {
        Whitebox.setInternalState(JCRHttpCacheStoreImpl.class, "log", log);
    }

    private void mockStore() throws Exception
    {
        Whitebox.setInternalState(store,  "cacheRootPath", JCRHttpCacheStoreImpl.DEFAULT_ROOTPATH);
        Whitebox.setInternalState(store,  "bucketTreeDepth", JCRHttpCacheStoreImpl.DEFAULT_BUCKETDEPTH);
        Whitebox.setInternalState(store,  "deltaSaveThreshold", JCRHttpCacheStoreImpl.DEFAULT_SAVEDELTA);
        Whitebox.setInternalState(store,  "expireTimeInSeconds", JCRHttpCacheStoreImpl.DEFAULT_EXPIRETIMEINSECONDS);

        doCallRealMethod().when(store).put(cacheKey, cacheContent);
        doCallRealMethod().when(store).contains(cacheKey);
        doCallRealMethod().when(store).invalidate(cacheKey);
        doCallRealMethod().when(store).clearCache();
        doCallRealMethod().when(store).getCacheEntry(any(String.class));
        doCallRealMethod().when(store).withSession(any(Consumer.class));
        doCallRealMethod().when(store).withSession(any(Consumer.class), any(Consumer.class));
        doCallRealMethod().when(store).withSession(any(Function.class));
        doCallRealMethod().when(store).withSession(any(Function.class), any(Consumer.class));
    }

    private CacheKeyMock generateCacheKey(Arguments arguments)
    {
        return new CacheKeyMock(
                arguments.cacheKeyUri,
                arguments.cacheKeyHierarchyResourcePath,
                arguments.cacheKeyHashCode,
                arguments.cacheKeyString
        );
    }

    private void mockEntryNodeWriter() throws Exception
    {
        whenNew(EntryNodeWriter.class)
                .withParameterTypes(Session.class, Node.class, CacheKey.class, CacheContent.class, Integer.class)
                .withArguments(any(Session.class), any(Node.class), any(CacheKey.class), any(CacheContent.class), any(Integer.class))
                .thenReturn(entryNodeWriter);
    }

    private void mockBucketNodeHandler() throws Exception{
        when(bucketNodeHandler.createOrRetrieveEntryNode(any(CacheKey.class)))
                .thenReturn(entryNode);
        whenNew(BucketNodeHandler.class)
                .withParameterTypes(Node.class, DynamicClassLoaderManager.class)
                .withArguments(any(Node.class), any(DynamicClassLoaderManager.class))
                .thenReturn(bucketNodeHandler);

    }

    private void mockBucketNodeFactory() throws Exception{
        when(factory.getBucketNode()).thenReturn(bucketNode);
        whenNew(BucketNodeFactory.class)
                .withParameterTypes(Session.class, String.class, CacheKey.class, Integer.class)
                .withArguments(any(Session.class), any(String.class), any(CacheKey.class), any(Integer.class))
                .thenReturn(factory);
    }

    private void generateCacheContent(){
        when(cacheContent.getCharEncoding()).thenReturn(arguments.cacheContentCharEncoding);
        when(cacheContent.getContentType()).thenReturn(arguments.cacheContentType);
        when(cacheContent.getInputDataStream()).thenReturn(arguments.contents);
        when(cacheContent.getStatus()).thenReturn(arguments.cacheContentStatus);
        when(cacheContent.getHeaders()).thenReturn(arguments.headers);
        when(cacheContent.getTempSink()).thenReturn(new MemTempSinkImpl());
    }


    private JCRHttpCacheStoreImpl mockRepository() throws Exception
    {
        when(resourceResolver.isLive()).thenAnswer(new Answer<Boolean>()
        {
            @Override public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                return resourceResolverOpen.get();
            }
        });

        when(resourceResolver, "close").then(new Answer<Void>()
        {
            @Override public Void answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                resourceResolverOpen.set(false);
                return null;
            }
        });

        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        Whitebox.setInternalState(store, "resourceResolverFactory", resourceResolverFactory);
        Whitebox.setInternalState(store, "dclm", dclm);
        Whitebox.setInternalState(store, "cacheRootPath", JCRHttpCacheStoreImpl.DEFAULT_ROOTPATH);
        when(resourceResolverFactory.getAdministrativeResourceResolver(null)).thenReturn(resourceResolver);
        when(session.getNode(JCRHttpCacheStoreImpl.DEFAULT_ROOTPATH)).thenReturn(rootNode);
        when(session.nodeExists(JCRHttpCacheStoreImpl.DEFAULT_ROOTPATH)).thenReturn(true);

        return store;
    }

    public JCRHttpCacheStoreImpl getStore()
    {
        return store;
    }

    public Session getSession()
    {
        return session;
    }

    public ResourceResolver getResourceResolver()
    {
        return resourceResolver;
    }

    public CacheKey getCacheKey()
    {
        return cacheKey;
    }

    public CacheContent getCacheContent()
    {
        return cacheContent;
    }

    public static Logger getLog()
    {
        return log;
    }

    public EntryNodeWriter getEntryNodeWriter()
    {
        return entryNodeWriter;
    }
}
