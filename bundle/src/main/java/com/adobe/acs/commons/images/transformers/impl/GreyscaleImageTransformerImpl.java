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

package com.adobe.acs.commons.images.transformers.impl;

import com.adobe.acs.commons.images.ImageTransformer;
import com.day.image.Layer;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ACS AEM Commons - Image Transformer - Greyscale
 */
@Component
@Properties({
        @Property(
                name = ImageTransformer.PROP_TYPE,
                value = GreyscaleImageTransformerImpl.TYPE
        )
})
@Service
public class GreyscaleImageTransformerImpl implements ImageTransformer {
    private static final Logger log = LoggerFactory.getLogger(GreyscaleImageTransformerImpl.class);

    static final String TYPE = "greyscale";

    @Override
    public final Layer transform(final Layer layer, final ValueMap properties) {
        log.debug("Transforming with [ {} ]", TYPE);

        layer.grayscale();

        return layer;
    }
}
