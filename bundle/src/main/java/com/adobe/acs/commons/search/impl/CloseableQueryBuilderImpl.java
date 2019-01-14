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
package com.adobe.acs.commons.search.impl;

import java.io.IOException;
import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.adobe.acs.commons.search.CloseableQuery;
import com.adobe.acs.commons.search.CloseableQueryBuilder;
import com.adobe.acs.commons.wrap.cqsearch.QueryIWrap;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Service implementing {@link CloseableQueryBuilder}, which is intended to be a drop-in replacement for the
 * {@link QueryBuilder} service, except for the use of {@link ResourceResolver} arguments in place of {@link Session}s.
 * The use of {@link ResourceResolver}s is both more convenient for developers, who are working primarily in a Sling API
 * context, and more convenient for injecting a logout-guarded session wrapper to allow for shallow-closing the
 * encapsulated ResourceResolver without terminating request Sessions.
 */
@Component
public final class CloseableQueryBuilderImpl implements CloseableQueryBuilder {

    @Reference
    private QueryBuilder queryBuilder;

    @Override
    public CloseableQuery createQuery(final PredicateGroup rootPredicateGroup,
                                      final ResourceResolver resourceResolver) {
        return createQuery(rootPredicateGroup, resourceResolver.adaptTo(Session.class));
    }

    @Override
    public CloseableQuery createQuery(final ResourceResolver resourceResolver) {
        return createQuery(resourceResolver.adaptTo(Session.class));
    }

    @Override
    public CloseableQuery createQuery(final PredicateGroup rootPredicateGroup, final Session session) {
        return new CloseableQueryImpl(queryBuilder.createQuery(rootPredicateGroup,
                SessionLogoutGuardFactory.useBestWrapper(session)));
    }

    @Override
    public CloseableQuery createQuery(final Session session) {
        return new CloseableQueryImpl(queryBuilder.createQuery(SessionLogoutGuardFactory
                .useBestWrapper(session)));
    }

    @Override
    public CloseableQuery loadQuery(final String path,
                                    final ResourceResolver resourceResolver) throws RepositoryException, IOException {
        return loadQuery(path, resourceResolver.adaptTo(Session.class));
    }

    @Override
    public CloseableQuery loadQuery(final String path, final Session session) throws RepositoryException, IOException {
        final Query query = queryBuilder.loadQuery(path, SessionLogoutGuardFactory.useBestWrapper(session));
        return query != null ? new CloseableQueryImpl(query) : null;
    }

    @Override
    public void storeQuery(final Query query,
                           final String path,
                           final boolean createFile,
                           final ResourceResolver resourceResolver) throws RepositoryException, IOException {
        storeQuery(query, path, createFile, resourceResolver.adaptTo(Session.class));
    }

    @Override
    public void storeQuery(final Query query, final String path, final boolean createFile, final Session session)
            throws RepositoryException, IOException {
        queryBuilder.storeQuery(query, path, createFile, session);
    }

    @Override
    public void clearFacetCache() {
        queryBuilder.clearFacetCache();
    }

    static class CloseableQueryImpl implements QueryIWrap, CloseableQuery {

        final Query wrapped;

        CloseableQueryImpl(final Query wrapped) {
            this.wrapped = wrapped;
        }

        @Nonnull
        @Override
        public Query wrapQuery(@Nonnull final Query other) {
            if (other instanceof CloseableQueryImpl) {
                return other;
            }
            return new CloseableQueryImpl(other);
        }

        @Nonnull
        @Override
        public Query unwrapQuery() {
            return wrapped;
        }
    }
}
