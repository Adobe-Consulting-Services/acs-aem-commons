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

package com.adobe.acs.commons.workflow.bulk.execution;

import aQute.bnd.annotation.ProviderType;
import com.adobe.acs.commons.workflow.bulk.execution.model.Config;
import org.apache.sling.api.resource.PersistenceException;

import javax.jcr.RepositoryException;

@ProviderType
@SuppressWarnings("squid:S1214")
public interface BulkWorkflowEngine {
    String SLING_RESOURCE_TYPE = "acs-commons/components/utilities/bulk-workflow-manager";

    /**
     * Initialize the Bulk Workflow Manager jcr:content node and build out the batch structure.
     *
     * @param config bulk workflow manager config obj
     * @throws PersistenceException
     * @throws RepositoryException
     */
    void initialize(Config config) throws PersistenceException,
            RepositoryException;

    /**
     * Start bulk workflow process.
     *
     * @param config bulk workflow manager config obj
     */
    void start(Config config) throws PersistenceException;

    /**
     * Stop bulk workflow process.
     *
     * @param config bulk workflow manager config obj
     * @throws PersistenceException
     */
    void stop(Config config) throws PersistenceException;

    /**
     * Stopping bulk workflow process.
     *
     * @param config bulk workflow manager config obj
     * @throws PersistenceException
     */
    void stopping(Config config) throws PersistenceException;

    /**
     * Resume as stopped bulk workflow process.
     *
     * @param config bulk workflow manager config obj
     * @throws PersistenceException
     */
    void resume(Config config) throws PersistenceException;

}