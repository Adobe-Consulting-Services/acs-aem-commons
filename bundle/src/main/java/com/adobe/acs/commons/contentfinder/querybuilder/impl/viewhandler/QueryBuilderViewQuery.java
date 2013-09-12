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
import java.util.List;
import java.util.Map;

public class QueryBuilderViewQuery implements ViewQuery {
    private static final Logger log = LoggerFactory.getLogger(QueryBuilderViewQuery.class);

    private final Query query;

    public QueryBuilderViewQuery(final Query query) {
        this.query = query;
    }

    @Override
    public Collection<com.day.cq.wcm.core.contentfinder.Hit> execute() {
        final List<com.day.cq.wcm.core.contentfinder.Hit> hits = new ArrayList<com.day.cq.wcm.core.contentfinder.Hit>();

        if (this.query == null) {
            return hits;
        }

        final SearchResult result = this.query.getResult();

        // iterating over the results
        for (Hit hit : result.getHits()) {
            try {
                hits.add(createHit(hit));
            } catch (RepositoryException e) {
                log.error("Could not return required information for Content Finder result: {}", hit.toString());
            }
        }

        return hits;
    }

    /**
     * ViewQuery integration
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
