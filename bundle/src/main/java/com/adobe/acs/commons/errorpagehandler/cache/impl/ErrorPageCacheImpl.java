/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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

package com.adobe.acs.commons.errorpagehandler.cache.impl;

import com.adobe.acs.commons.errorpagehandler.cache.ErrorPageCache;
import com.adobe.acs.commons.errorpagehandler.cache.ErrorPageCacheMBean;
import com.adobe.acs.commons.util.ResourceDataUtil;
import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.DynamicMBean;
import javax.management.NotCompliantMBeanException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component(
        label = "ACS AEM Commons - Error Page Handler Cache",
        metatype = true,
        immediate = true
)
@Properties({
        @Property(
                label = "MBean Name",
                name = "jmx.objectname",
                value = "com.adobe.acs.commons:type=error-page-handler",
                propertyPrivate = true
        )
})
@Service(value = { DynamicMBean.class, ErrorPageCache.class })
public class ErrorPageCacheImpl extends AnnotatedStandardMBean implements ErrorPageCache, ErrorPageCacheMBean {
    private static final Logger log = LoggerFactory.getLogger(ErrorPageCacheImpl.class);

    private static int ttl = 60;

    private ConcurrentHashMap<String, CacheEntry> cache;

    public ErrorPageCacheImpl() throws NotCompliantMBeanException {
        super(ErrorPageCacheMBean.class);
    }


    @Override
    public String get(final String path,
                    final SlingHttpServletRequest request, final SlingHttpServletResponse response) {

        CacheEntry cacheEntry = cache.get(path);

        /*if(!StringUtils.equals("anonymous", request.getUserPrincipal().getName())) {
            log.debug("Not Anonymous: {}", path);

            final String data = ResourceDataUtil.getIncludeAsString(path, request, response);

            log.debug(data);

            return data;

        } else */

        if(cacheEntry == null || cacheEntry.isExpired(new Date())) {
            // MISS
            if(cacheEntry == null) {
                cacheEntry = new CacheEntry(ttl);
            }

            log.debug("Cache miss: {}", path);

            final String data = ResourceDataUtil.getIncludeAsString(path, request, response);

            log.debug(data);

            cacheEntry.setData(data);
            cacheEntry.resetExpiresAt(ttl);
            cacheEntry.incrementMisses();

            // Add entry to cache
            cache.put(path, cacheEntry);

            log.debug("cache size; {}", this.cache.size());

            return data;
        } else {
            // HIT
            log.debug("Cache hit: {}", path);

            final String data = cacheEntry.getData();
            cacheEntry.incrementHits();

            cache.put(path, cacheEntry);

            log.debug(data);

            return data;
        }
    }

    @Activate
    protected  void activate(Map<String, String> config) {
        ttl = PropertiesUtil.toInteger(config.get("ttl"), 60);
        cache = new ConcurrentHashMap<String, CacheEntry>();
    }

    @Deactivate
    protected void deactivate(Map<String, String> config) {
        cache = null;
    }

    /* MBean Methods */

    @Override
    public int getHits() {
        int hits = 0;

        for(final Map.Entry<String, CacheEntry> entry : this.cache.entrySet()) {
            hits = hits + entry.getValue().getHits();
        }

        return hits;
    }

    @Override
    public int getCacheCount() {
        if(this.cache == null) {
            return 0;
        } else {
            return this.cache.size();
        }
    }

    @Override
    public int getMisses() {
        int misses = 0;

        for(final Map.Entry<String, CacheEntry> entry : this.cache.entrySet()) {
            misses = misses + entry.getValue().getMisses();
        }

        return misses;
    }

    @Override
    public int getTotalCacheRequests() {
        return this.getHits() + getMisses();
    }

    @Override
    public int getTotal() {
        return this.getHits() + getMisses();
    }

    @Override
    public long getCacheSizeInBytes() {
        long bytes = 0;

        for(final Map.Entry<String, CacheEntry> entry : this.cache.entrySet()) {
            bytes = bytes + entry.getValue().getData().getBytes().length;
        }

        return bytes;
    }
}
