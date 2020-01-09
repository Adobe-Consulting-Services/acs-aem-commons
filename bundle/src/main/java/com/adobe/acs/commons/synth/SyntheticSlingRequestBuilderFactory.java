/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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

package com.adobe.acs.commons.synth;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;


@Component(service = SyntheticSlingRequestBuilderFactory.class)
public class SyntheticSlingRequestBuilderFactory {

    @Reference
    private SyntheticRequestDispatcherFactory dispatcherFactory;

    /**
     * SyntheticSlingRequestBuilderFactory - produces a SyntheticSlingRequestBuilder
     * @see SyntheticSlingRequestBuilder
     * @param resourceResolver
     * @param targetResourcePath the target resource path of the synthetic sling request. a request always requires a target resource
     * @return
     */
    public SyntheticSlingRequestBuilder getBuilder(ResourceResolver resourceResolver, String targetResourcePath){
        return new SyntheticSlingRequestBuilder(dispatcherFactory, resourceResolver, targetResourcePath);
    }

    /**
     * SyntheticSlingRequestBuilderFactory - produces a SyntheticSlingRequestBuilder
     * @see SyntheticSlingRequestBuilder
     * @param resourceResolver
     * @param targetResource the target resource of the synthetic sling request. a request always requires a target resource
     * @return
     */
    public SyntheticSlingRequestBuilder getBuilder(ResourceResolver resourceResolver, Resource targetResource){
        return new SyntheticSlingRequestBuilder(dispatcherFactory, resourceResolver, targetResource);
    }
}