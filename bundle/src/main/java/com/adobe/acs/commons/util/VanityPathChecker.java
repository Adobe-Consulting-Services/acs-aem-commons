/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.util;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.NameConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;

public class VanityPathChecker {
    private static final Logger log = LoggerFactory.getLogger(VanityPathChecker.class);

    private VanityPathChecker() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean ValidateVanityPath(final ResourceResolver resourceResolver, final String vanityPath, final String currentPath){
        final QueryBuilder queryBuilder = resourceResolver.adaptTo(QueryBuilder.class);
        final Session session = resourceResolver.adaptTo(Session.class);
        final Map<String, String> queryMap = new ConcurrentSkipListMap<String, String>();
        queryMap.put("type", "cq:PageContent");
        queryMap.put("property", NameConstants.PN_SLING_VANITY_PATH);
        queryMap.put("property.value", vanityPath);
        if(queryBuilder != null && session != null){
            final Query query = queryBuilder.createQuery(PredicateGroup.create(queryMap), session);
            log.debug("Performing search: {}", queryMap.toString());
            final SearchResult result = query.getResult();
            List<Hit> hits = result.getHits();
            long count = hits.stream()
                    .map(hit -> {
                        try {
                            return hit.getPath();
                        } catch (RepositoryException e) {
                            log.warn("Problem while querying vanity path");
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .filter(hitPath -> (StringUtils.isBlank(currentPath)) || !hitPath.contains(currentPath))
                    .count();
            log.debug("duplicates found: {}", count);
            return count == 0;
        }
        return false;
    }

}
