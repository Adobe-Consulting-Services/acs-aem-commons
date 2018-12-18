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

/**
 * Invalidation module of http cache implementation has a sling job with a defined topic, a job consumer and a set of
 * invalidation events. Invalidation events create invalidation jobs which would be consumed by the job consumer and
 * invalidates the cache. For a typical implementation, invalidation event could be custom supplied based on the cache
 * config invalidation requirements. A sample implementation based on sling eventing is provided.
 */
@aQute.bnd.annotation.Version("1.1.0")
package com.adobe.acs.commons.httpcache.invalidator;


