/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
package com.adobe.acs.commons.contentfinder.querybuilder.impl.viewhandler;

import com.adobe.acs.commons.contentfinder.querybuilder.impl.ContentFinderHitBuilder;
import com.day.cq.search.Query;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.core.contentfinder.ViewQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class QueryBuilderViewQuery implements ViewQuery {
    private static final Logger log = LoggerFactory.getLogger(QueryBuilderViewQuery.class);

    private final Query query;
    private List<com.day.cq.wcm.core.contentfinder.Hit> hits = null;

    public QueryBuilderViewQuery(final Query query) {
        this.query = query;
    }

    @Override
    public Collection<com.day.cq.wcm.core.contentfinder.Hit> execute() {
        if (hits == null) {
            hits = new ArrayList<>();

            if (this.query == null) {
                return Collections.unmodifiableList(hits);
            }

            final SearchResult result = this.query.getResult();

            // iterating over the results
            for (Hit hit : result.getHits()) {
                try {
                    hits.add(createHit(hit));
                } catch (RepositoryException e) {
                    log.error("Could not return required information for Content Finder result: {}", hit);
                }
            }

        }
        return Collections.unmodifiableList(hits);
    }

    /**
     * ViewQuery integration.
     *
     * @param hit
     * @return
     * @throws javax.jcr.RepositoryException
     */
    private com.day.cq.wcm.core.contentfinder.Hit createHit(final Hit hit) throws RepositoryException {
        final Map<String, Object> map = ContentFinderHitBuilder.buildGenericResult(hit);
        final com.day.cq.wcm.core.contentfinder.Hit cfHit = new com.day.cq.wcm.core.contentfinder.Hit();

        for (final Map.Entry<String, Object> entry : map.entrySet()) {
            cfHit.set(entry.getKey(), entry.getValue());
        }

        return cfHit;
    }
}
