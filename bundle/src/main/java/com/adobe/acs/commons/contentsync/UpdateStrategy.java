/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
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
package com.adobe.acs.commons.contentsync;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

import java.util.List;

public interface UpdateStrategy {

    /**
     * This method is called on the remote instance when Content Sync requests the list of resources to sync
     *
     * @param request   the request from the Content Sync UI
     * @return  the list of resources to sync
     */
    List<CatalogItem> getItems(SlingHttpServletRequest request);


    /**
     * Compare local and remote resources and decided whether the resource was modified and need to be sync-ed
     *
     * @param remoteResource    json representation of a remote resource
     * @param localResource local resource
     * @return  whether the resource was modified
     */
    boolean isModified(CatalogItem remoteResource, Resource localResource);

    /**
     *
     * @param remoteResource    json representation of a remote resource
     * @param localResource local resource
     * @return  message to print in the UI
     */
    String getMessage(CatalogItem remoteResource, Resource localResource);
}
