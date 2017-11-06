/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
package com.adobe.acs.commons.httpcache.store.jcr.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.jcr.Binary;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import com.adobe.acs.commons.httpcache.store.TempSink;

import org.apache.commons.lang.NotImplementedException;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;


/**
 * ACS AEM Commons - HTTP Cache - JCR based cache store implementation.
 */

// TODO - Placeholder component. To be implemented.

@Component
@Service
@Property(name = HttpCacheStore.KEY_CACHE_STORE_TYPE,
          value = HttpCacheStore.VALUE_JCR_CACHE_STORE_TYPE,
          propertyPrivate = true)
public class JCRHttpCacheStoreImpl implements HttpCacheStore {

    @Reference private Repository repository;

    @Property private String cacheRootPath;

    @Property  private int bucketDimensionDepth;

    private Session session;

    private Node cacheRoot;

    @Activate protected void activate() throws RepositoryException
    {
        Credentials credentials = null;
        session = repository.login(credentials);
        cacheRoot = session.getNode(cacheRootPath);
    }

    @Deactivate protected void deactivate(){
        if(session != null && session.isLive())
            session.logout();
    }

    @Override
    public void put(CacheKey key, CacheContent content) throws HttpCacheDataStreamException {

        try {
            final Node bucketNode = BucketNodeHandler.getBucketNode(cacheRoot, key, bucketDimensionDepth);

            final Node entryNode = createOrRetrieveEntryNode(bucketNode, key, content);

            new EntryNodeWriter(session, entryNode, content).write();


        } catch (Exception e) {
            throw new HttpCacheDataStreamException(e);
        }
    }


    private Node createOrRetrieveEntryNode(Node bucketNode, CacheKey key, CacheContent content)
            throws RepositoryException, IOException, ClassNotFoundException
    {
        final Node entryNode = getEntryIfExists(bucketNode, key);

        if(null != entryNode){
            return entryNode;
        }else{

        }

        return null;
    }

    private Node getEntryIfExists(Node bucketNode, CacheKey key)
            throws RepositoryException, IOException, ClassNotFoundException
    {
        NodeIterator entryNodeIterator  = bucketNode.getNodes();

        while(entryNodeIterator.hasNext()){
            Node entryNode = entryNodeIterator.nextNode();
            CacheKey entryKey = getCacheKeyFromEntryNode(entryNode);
            if(key.equals(entryKey))
                return entryNode;
        }

        return null;
    }

    private CacheKey getCacheKeyFromEntryNode(Node entryNode)
            throws RepositoryException, IOException, ClassNotFoundException
    {
        final javax.jcr.Property cacheKeyProperty = entryNode.getProperty("cacheKeySerialized");

        ObjectInputStream objectInputStream = new ObjectInputStream(cacheKeyProperty.getBinary().getStream());
        return (CacheKey) objectInputStream.readObject();
    }

    private void saveCacheKeyToEntryNode(Node entryNode, CacheKey key) throws RepositoryException, IOException
    {
        final PipedInputStream pis = new PipedInputStream();
        final PipedOutputStream pos = new PipedOutputStream(pis);

        ObjectOutputStream objectOutputStream = new ObjectOutputStream(pos);
        objectOutputStream.writeObject(key);
        objectOutputStream.close();

        Binary binary = session.getValueFactory().createBinary(pis);
        entryNode.setProperty("cacheKeySerialized", binary);
        session.save();
    }


    @Override
    public boolean contains(CacheKey key) {
        throw new NotImplementedException();
    }

    @Override
    public CacheContent getIfPresent(CacheKey key) {
        throw new NotImplementedException();
    }

    @Override
    public long size() {
        throw new NotImplementedException();
    }

    @Override
    public void invalidate(CacheKey key) {
        throw new NotImplementedException();
    }

    @Override
    public void invalidateAll() {
        throw new NotImplementedException();
    }

    @Override
    public void invalidate(HttpCacheConfig cacheConfig) {
        throw new NotImplementedException();
    }

    @Override
    public TempSink createTempSink() {
        throw new NotImplementedException();
    }
}
