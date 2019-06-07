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
package com.adobe.acs.commons.rewriter.impl;

import com.adobe.acs.commons.properties.PropertyAggregatorService;

import org.apache.sling.rewriter.Transformer;
import org.apache.sling.rewriter.TransformerFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = TransformerFactory.class, property = {
        "pipeline.type=templated-transformer"
})
public class TemplatedTransformerFactory implements TransformerFactory {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Reference
    private PropertyAggregatorService propertyAggregatorService;

    @Override
    public Transformer createTransformer() {
        log.trace("Templated Transformer");
        return new TemplatedTransformer(propertyAggregatorService);
    }
}
