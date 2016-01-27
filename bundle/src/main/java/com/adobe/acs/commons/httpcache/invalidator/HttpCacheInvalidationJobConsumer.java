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
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ACS AEM Commons - HTTP Cache - Cache invalidation job consumer
 * Consumes job for invalidating the http cache.
 *
 * Sling job consumer consuming the job created for invalidating cache. For creating an invalidation job for this
 * consumer, make use of the topic and associated constants defined at {@link CacheInvalidationJobConstants}
 */
@Component(immediate = true)
@Service
@Property(name = JobConsumer.PROPERTY_TOPICS,
          value = CacheInvalidationJobConstants.TOPIC_HTTP_CACHE_INVALIDATION_JOB)
public class HttpCacheInvalidationJobConsumer implements JobConsumer {
    private static final Logger log = LoggerFactory.getLogger(HttpCacheInvalidationJobConsumer.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY,
               policy = ReferencePolicy.DYNAMIC)
    private HttpCacheEngine httpCacheEngine;

    @Override
    public JobResult process(final Job job) {

        // Validate the given job.
        String path = (String) job.getProperty(CacheInvalidationJobConstants.PAYLOAD_KEY_DATA_CHANGE_PATH);
        if (StringUtils.isEmpty(path)) {
            log.error("Invalidation job doesn't have path information.");
            return JobResult.CANCEL;
        }

        // Check if the path in the job is applicable for the set cache configs.
        if (httpCacheEngine.isPathPotentialToInvalidate(path)) {
            // Invalidate the cache.
            try{
                httpCacheEngine.invalidateCache(path);
            } catch (HttpCacheException e){
                log.debug("Job with the payload path - {} has invalidated the cache", path);
            }
        }
        log.trace("Invalidation job for the path processed.", path);
        return JobResult.OK;
    }
}
