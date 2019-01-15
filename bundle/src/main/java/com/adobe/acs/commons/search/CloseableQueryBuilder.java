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
package com.adobe.acs.commons.search;

import java.io.IOException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.annotation.versioning.ProviderType;

/**
 * A drop-in replacement for the CQ QueryBuilder service that wraps {@link Query} instances to produce
 * {@link CloseableQuery} instances, whose {@link CloseableQuery#close()} method cleans up the internal shallow resource
 * resolver.
 */
@ProviderType
public interface CloseableQueryBuilder {

    /**
     * Create a closeable query around the provided predicate group.
     *
     * @param predicates       the root group of predicates
     * @param resourceResolver the session to use
     * @return a closeable query
     */
    CloseableQuery createQuery(final PredicateGroup predicates, final ResourceResolver resourceResolver);

    /**
     * Create a closeable query.
     *
     * @param resourceResolver the session to use
     * @return a closeable query
     */
    CloseableQuery createQuery(final ResourceResolver resourceResolver);

    /**
     * Create a closeable query around the provided predicate group.
     *
     * @param predicates the root group of predicates
     * @param session    the session to use
     * @return a closeable query
     */
    CloseableQuery createQuery(final PredicateGroup predicates, final Session session);

    /**
     * Create a closeable query.
     *
     * @param session the session to use
     * @return a closeable query
     */
    CloseableQuery createQuery(final Session session);

    /**
     * Load a CQ Search query from a properties file located at the specified path. If the specified path does not exist,
     * returns null.
     *
     * @param path             a repository path to a properties file
     * @param resourceResolver the resource resolver providing access to the repository
     * @return a query ready for execution
     * @throws RepositoryException on JCR failure
     * @throws IOException         on failure to read properties file
     */
    CloseableQuery loadQuery(final String path, final ResourceResolver resourceResolver) throws RepositoryException, IOException;

    /**
     * Load a CQ Search query from a properties file located at the specified path. If the specified path does not exist,
     * returns null.
     *
     * @param path    a repository path to a properties file
     * @param session the session providing access to the repository
     * @return a query ready for execution
     * @throws RepositoryException on JCR failure
     * @throws IOException         on failure to read properties file
     */
    CloseableQuery loadQuery(final String path, final Session session) throws RepositoryException, IOException;

    /**
     * Saves the query predicates to a properties file in the repository at the specified path.
     *
     * @param query            the query whose predicates should be stored
     * @param path             the path of the properties file
     * @param createFile       true to create the file if it doesn't exist already
     * @param resourceResolver the session to use
     * @throws RepositoryException on JCR failure
     * @throws IOException         on failure save properties file
     */
    void storeQuery(final Query query,
                    final String path,
                    final boolean createFile,
                    final ResourceResolver resourceResolver) throws RepositoryException, IOException;

    /**
     * Saves the query predicates to a properties file in the repository at the specified path.
     *
     * @param query      the query whose predicates should be stored
     * @param path       the path of the properties file
     * @param createFile true to create the file if it doesn't exist already
     * @param session    the session to use
     * @throws RepositoryException on JCR failure
     * @throws IOException         on failure save properties file
     */
    void storeQuery(final Query query,
                    final String path,
                    final boolean createFile,
                    final Session session) throws RepositoryException, IOException;

    /**
     * Clears the QueryBuilder facet cache, in case you need to do that.
     */
    void clearFacetCache();
}
