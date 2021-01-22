/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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
package com.adobe.acs.commons.properties;

import java.util.Map;

import com.day.cq.wcm.api.Page;

import org.apache.sling.api.resource.Resource;

public interface PropertyAggregatorService {

    /**
     * Iterates up the content tree to aggregate all the current page properties and inherited
     * page properties. Assigns the appropriate namespace to the properties as well.
     *
     * @param resource The content resource of a page
     * @return The map of properties
     */
    Map<String, Object> getProperties(Resource resource);

    /**
     * Overloaded method from above. Passes the content resource of the page.
     *
     * @param page The page to gather properties from
     * @return The map of properties
     */
    Map<String, Object> getProperties(Page page);
}
