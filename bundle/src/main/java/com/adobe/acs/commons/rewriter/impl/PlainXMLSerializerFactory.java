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

import javax.xml.transform.TransformerConfigurationException;

import org.osgi.service.component.annotations.Component;
import org.apache.sling.rewriter.Serializer;
import org.apache.sling.rewriter.SerializerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class PlainXMLSerializerFactory implements SerializerFactory {

    private static final Logger log = LoggerFactory.getLogger(PlainXMLSerializerFactory.class);

    @Override
    public Serializer createSerializer() {
        try {
            return new PlainXMLSerializer();
        } catch (TransformerConfigurationException e) {
            log.error("Unable to create serializer", e);
            return null;
        }
    }
}
