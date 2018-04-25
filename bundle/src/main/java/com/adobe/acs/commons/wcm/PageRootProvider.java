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
package com.adobe.acs.commons.wcm;

import aQute.bnd.annotation.ConsumerType;
import com.day.cq.wcm.api.Page;
import org.apache.sling.api.resource.Resource;

/**
 * Service to fetch the site root page (i.e. home page) for a given resource.
 */
@ConsumerType
public interface PageRootProvider {
    /**
     * Returns the root page for the provided resource. The root page is selected
     * via the regex(es) provided in the PageRootProviderImpl's OSGi configuration.
     * @param resource The Resource for which to return the root page
     * @return Root page
     */
    Page getRootPage(Resource resource);

    /**
     * Returns the root path for the provided resource path. The root path is selected
     * via the regex(es) provided in the PageRootProviderImpl's OSGi configuration.
     * @param resourcePath The path for which to return the root path
     * @return Root path
     */
    String getRootPagePath(String resourcePath);
}
