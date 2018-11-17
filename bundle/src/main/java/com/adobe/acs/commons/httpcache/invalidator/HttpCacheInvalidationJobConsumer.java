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
package com.adobe.acs.commons.httpcache.invalidator;

import com.adobe.acs.commons.httpcache.engine.HttpCacheEngine;
import com.adobe.acs.commons.httpcache.exception.HttpCacheException;
import com.day.cq.wcm.commons.ReferenceSearch;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

/**
 * ACS AEM Commons - HTTP Cache - Cache invalidation job consumer
 * Consumes job for invalidating the http cache.
 *
 * Sling job consumer consuming the job created for invalidating cache. For creating an invalidation job for this
 * consumer, make use of the topic and associated constants defined at {@link CacheInvalidationJobConstants}
 */
@Component(label = "ACS AEM Commons - HTTP Cache - Cache invalidation job consumer",
           description = "Consumes job for invalidating the http cache",
           immediate = true,
           metatype = true)
@Service
@Property(name = JobConsumer.PROPERTY_TOPICS,
          value = CacheInvalidationJobConstants.TOPIC_HTTP_CACHE_INVALIDATION_JOB,
          propertyPrivate = true
)
public class HttpCacheInvalidationJobConsumer implements JobConsumer {
    private static final Logger log = LoggerFactory.getLogger(HttpCacheInvalidationJobConsumer.class);

    @Property(label = "Invalidate references",
            description = "Whether to search for references and invalidate them in the cache.",
            boolValue = HttpCacheInvalidationJobConsumer.DEFAULT_REFERENCES)
    private static final String PROP_REFERENCES = "httpcache.config.invalidation.references";
    private static final boolean DEFAULT_REFERENCES = false;
    private boolean invalidateRefs;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY,
            policy = ReferencePolicy.DYNAMIC)
    private volatile HttpCacheEngine httpCacheEngine;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Activate
    protected void activate(Map<String, Object> configs) {
        invalidateRefs = PropertiesUtil.toBoolean(configs.get(PROP_REFERENCES), DEFAULT_REFERENCES);
    }

    @Override
    public JobResult process(final Job job) {

        // Validate the given job.
        String path = (String) job.getProperty(CacheInvalidationJobConstants.PAYLOAD_KEY_DATA_CHANGE_PATH);
        if (StringUtils.isEmpty(path)) {
            log.error("Invalidation job doesn't have path information.");
            return JobResult.CANCEL;
        }

        invalidate(path);

        if(invalidateRefs) {
            invalidateReferences(path);
        }

        log.trace("Invalidation job for the path processed.", path);
        return JobResult.OK;
    }

    /**
     * Invalidate the cache for the given path
     *
     * @param path the resource to invalidate
     */
    void invalidate(String path){
        // Check if the path in the job is applicable for the set cache configs.
        if (httpCacheEngine.isPathPotentialToInvalidate(path)) {
            // Invalidate the cache.
            try{
                log.debug("invalidating {}", path);
                httpCacheEngine.invalidateCache(path);
            } catch (HttpCacheException e){
                log.debug("Job with the payload path - {} has invalidated the cache", path);
            }
        }

    }

    /**
     * Searches for references to the given path and invalidates them in the cache
     *
     * @param path the path to search for
     */
    void invalidateReferences(String path) {
        try (ResourceResolver adminResolver = resolverFactory.getServiceResourceResolver(null)){
            Collection<ReferenceSearch.Info> refs = new ReferenceSearch()
                    .search(adminResolver, path).values();
            for (ReferenceSearch.Info info : refs) {
                String refPath = info.getPage().getPath();
                invalidate(refPath);

            }
        } catch (Exception e){
            log.debug("failed to invalidate references of {}", path);
        }
    }
}
