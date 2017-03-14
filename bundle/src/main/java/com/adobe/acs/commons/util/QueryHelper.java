/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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

import aQute.bnd.annotation.ProviderType;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Map;

@ProviderType
public interface QueryHelper {

    /**
     * @param resourceResolver the resourceResolver providing access into the JCR
     * @param language querybuilder, list, xpath, JCR-SQL, JCR-SQL2
     * @param statement the query statement
     * @param relPath the relative path to apply to the query result resources
     * @return a list of Resource objects
     * @throws RepositoryException
     */
    List<Resource> findResources(ResourceResolver resourceResolver,
                                 String language,
                                 String statement,
                                 String relPath) throws RepositoryException;

    /**
     * Determines if the provided query will traverse.
     * @param resourceResolver the resourceResolver providing access into the JCR
     * @param language the query language (xpath, JCR-SQL, JCR-SQL2)
     * @param statement the query statement
     * @return true if the query will traverse, false if it will not
     */
    boolean isTraversal(ResourceResolver resourceResolver,
                       String language,
                       String statement) throws RepositoryException;

    /**
     * Determines if the provided query will traverse.
     * @param resourceResolver the resourceResolver providing access into the JCR
     * @param queryBuilderParams map of query builder params
     * @return true if the query will traverse, false if it will not
     */
    boolean isTraversal(ResourceResolver resourceResolver,
                       Map<String, String> queryBuilderParams) throws RepositoryException;
}
