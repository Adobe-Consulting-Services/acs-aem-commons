/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.rewriter.Generator;
import org.apache.sling.rewriter.GeneratorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service
@Property(name = "pipeline.type", value = "xml-generator")
public final class XMLParserGeneratorFactory implements GeneratorFactory {

    private static final Logger log = LoggerFactory.getLogger(XMLParserGeneratorFactory.class);

    @Override
    public Generator createGenerator() {
        try {
            return new XMLParserGenerator();
        } catch (Exception e) {
            log.error("Unable to create parser", e);
            return null;
        }
    }

}
