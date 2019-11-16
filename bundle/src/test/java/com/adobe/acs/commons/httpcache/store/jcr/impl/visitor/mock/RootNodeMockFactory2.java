package com.adobe.acs.commons.httpcache.store.jcr.impl.visitor.mock;

import java.util.HashMap;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.resourcebuilder.api.ResourceBuilder;
import org.apache.sling.testing.mock.sling.builder.ContentBuilder;
import org.apache.sling.testing.mock.sling.junit.SlingContext;

public class RootNodeMockFactory2 {
	
	private static final String CACHE_ROOT="/etc/acs-commons/httpcache/root";
	
	Settings settings;
	
	public RootNodeMockFactory2 (Settings settings) {
		this.settings = settings;
	}
	
	public Node build() {
		ContentBuilder cb = settings.context.create();
		
		Resource rootResource = cb.resource(CACHE_ROOT);

		
		
		return rootResource.adaptTo(Node.class);
	}
	
	
    public static class Settings{
        private int entryNodeCount            = 10;
        private int bucketDepth               = 10;
        private int expiredEntryNodeCount     = 0;
        private int emptyBucketNodeChainCount = 0;
        private boolean enableCacheEntryBinaryContent = false;
        private SlingContext context;

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
        
        public void SetSession(SlingContext context) {
        	this.context = context;
        }
    }

}
