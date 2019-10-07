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
package com.adobe.acs.commons.wrap.cqsearch;

import javax.annotation.Nonnull;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.eval.PredicateEvaluator;
import com.day.cq.search.facets.Bucket;
import com.day.cq.search.result.SearchResult;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Interface for wrappers of CQ Search Query instances.
 */
@ProviderType
public interface QueryIWrap extends Query {

    /**
     * Return the underlying query. This must be implemented by concrete classes.
     *
     * @return the underlying query
     */
    Query unwrapQuery();

    default @Nonnull
    Query wrapQuery(final @Nonnull Query other) {
        return other;
    }

    @Override
    default SearchResult getResult() {
        return unwrapQuery().getResult();
    }

    @Override
    default PredicateGroup getPredicates() {
        return unwrapQuery().getPredicates();
    }

    @Override
    default void registerPredicateEvaluator(final String type, final PredicateEvaluator evaluator) {
        unwrapQuery().registerPredicateEvaluator(type, evaluator);
    }

    @Override
    default Query refine(final Bucket bucket) {
        return wrapQuery(unwrapQuery().refine(bucket));
    }

    @Override
    default void setExcerpt(final boolean excerpt) {
        unwrapQuery().setExcerpt(excerpt);
    }

    @Override
    default boolean getExcerpt() {
        return unwrapQuery().getExcerpt();
    }

    @Override
    default void setStart(final long start) {
        unwrapQuery().setStart(start);
    }

    @Override
    default long getStart() {
        return unwrapQuery().getStart();
    }

    @Override
    default void setHitsPerPage(final long hitsPerPage) {
        unwrapQuery().setHitsPerPage(hitsPerPage);
    }

    @Override
    default long getHitsPerPage() {
        return unwrapQuery().getHitsPerPage();
    }
}
