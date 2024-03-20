/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.granite.ui.components;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.osgi.annotation.versioning.ProviderType;

/**
 * NamespacedTransformedResourceProvider
 * <p>
 * Transforms a resource underlying children with a namespace.
 * </p>
 */
@ProviderType
public interface NamespacedTransformedResourceProvider {
    
    /**
     * Transforms a resource underlying children with a namespace.
     * Children under the resource will have various properties (the ones configured under the service) prefixed with the namespace
     * @param request
     * @param targetResource
     * @return Wrapped resource
     */
    Resource transformResourceWithNameSpacing(SlingHttpServletRequest request, Resource targetResource);
    
}
