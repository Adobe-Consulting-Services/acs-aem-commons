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
package com.adobe.acs.commons.ondeploy;

import com.day.cq.search.QueryBuilder;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * A script that runs the first time it is deployed to an AEM server.
 */
public interface OnDeployScript {
    /**
     * Execute the script, passing in a resourceResolver and queryBuilder instance.
     *
     * @param resourceResolver Resource resolver.
     * @param queryBuilder Query builder.
     */
    void execute(ResourceResolver resourceResolver, QueryBuilder queryBuilder) throws Exception;
}
