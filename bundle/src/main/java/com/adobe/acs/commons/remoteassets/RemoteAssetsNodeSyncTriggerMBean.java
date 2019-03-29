/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.remoteassets;

import com.adobe.granite.jmx.annotation.Description;

/**
 * MBean interface for interacting with the Remote Asset Node Sync.
 */
@Description("Services for managing the Remote Asset nodes sync.")
public interface RemoteAssetsNodeSyncTriggerMBean {

    /**
     * Method to run when triggering the syncAssetNodes() located in {@link RemoteAssetsNodeSync}.
     */
    @Description("Executes remote asset node sync based on configured paths.")
    void syncAssetNodes();
}
