/**
 * Invalidation module of http cache implementation has a sling job with a defined topic, a job consumer and a set of
 * invalidation events. Invalidation events create invalidation jobs which would be consumed by the job consumer and
 * invalidates the cache. For a typical implementation, invalidation event could be custom supplied based on the cache
 * config invalidation requirements. A sample implementation based on sling eventing is provided.
 */
package com.adobe.acs.commons.httpcache.invalidator;


