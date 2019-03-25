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

import org.apache.sling.api.resource.Resource;

/**
 * Service for synchronizing a remote asset's binaries a from remote server.
 */
public interface RemoteAssetsBinarySync {

    /**
     * Sync an asset's binaries from a remote server.
     * All binaries (original + renditions) are sync'd.
     * @param resource Resource representing a dam:AssetContent
     * @return boolean true if sync successful, else false
     */
    boolean syncAsset(Resource resource);
}
