/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2022 Adobe
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
package com.adobe.acs.commons.cqsearch;

import com.day.cq.search.Query;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * Simple utility to use as an alternative to deprecated CloseableQuery in older versions of AEM.
 */
public final class QueryUtil {
    private static final Logger log = LoggerFactory.getLogger(QueryUtil.class);

    /**
     * No constructor.
     */
    private QueryUtil() {
        // private constructor
    }

    /**
     * Uses reflection to forcibly set the Query object's ResourceResolver to the provided.
     */
    public static void setResourceResolverOn(ResourceResolver resolver, Query query) {
        Class<? extends Query> clazz = query.getClass();
        try {
            Field resourceResolverField = clazz.getDeclaredField("resourceResolver");
            resourceResolverField.setAccessible(true);
            resourceResolverField.set(query, resolver);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.debug("Could not set ResourceResolver on provided Query: {} => {}",
                    e.getClass().getName(), e.getMessage());
        }
    }
}
