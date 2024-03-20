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
package com.adobe.acs.commons.rewriter.impl;

import com.adobe.acs.commons.ccvar.PropertyAggregatorService;

import com.adobe.acs.commons.ccvar.PropertyConfigService;
import org.apache.sling.rewriter.Transformer;
import org.apache.sling.rewriter.TransformerFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link TransformerFactory} defined to create new {@link ContentVariableTransformer} objects and pass in the reference to
 * the service used to aggregate properties.
 */
@Component(service = TransformerFactory.class, property = {
        "pipeline.type=ccvar-transformer"
})
public class ContentVariableTransformerFactory implements TransformerFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ContentVariableTransformerFactory.class);

    @Reference
    private PropertyAggregatorService propertyAggregatorService;

    @Reference
    private PropertyConfigService propertyConfigService;

    @Override
    public Transformer createTransformer() {
        LOG.trace("Content Variable Transformer");
        return new ContentVariableTransformer(propertyAggregatorService, propertyConfigService);
    }
}
