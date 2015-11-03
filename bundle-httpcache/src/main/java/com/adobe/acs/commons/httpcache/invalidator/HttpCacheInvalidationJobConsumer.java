package com.adobe.acs.commons.httpcache.invalidator;

import com.adobe.acs.commons.httpcache.engine.HttpCacheEngine;
import com.adobe.acs.commons.httpcache.invalidator.CacheInvalidationJobConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling job consumer consuming the job created for invalidating cache. For creating an invalidation job for this
 * consumer, make use of the topic and associated constants defined at {@link CacheInvalidationJobConstants}
 */
@Component(label = "ACS AEM Commons - HTTP Cache - Cache invalidation job consumer",
           description = "Consumes job for invalidating the http cache.",
           immediate = true)
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
            if (httpCacheEngine.invalidateCache(path)) {
                log.debug("Job with the payload path - {} has invalidated the cache", path);
            }
        }
        log.trace("Invalidation job for the path processed.", path);
        return JobResult.OK;
    }
}
