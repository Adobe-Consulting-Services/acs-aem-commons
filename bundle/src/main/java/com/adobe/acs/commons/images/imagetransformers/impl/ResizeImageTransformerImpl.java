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

package com.adobe.acs.commons.images.imagetransformers.impl;

import com.adobe.acs.commons.images.ImageTransformer;
import com.day.image.Layer;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        label = "ACS AEM Commons - Image Transformer - Resize",
        description = "ImageTransformer that resizes the layer. Accepts two Integer params: height and width."
                + "If either is left blank the missing dimension will be computed based on the original layer's "
                + "aspect ratio")
@Properties({
        @Property(
                name = ImageTransformer.PROP_TYPE,
                value = ResizeImageTransformerImpl.TYPE,
                propertyPrivate = true
        )
})
@Service
public class ResizeImageTransformerImpl implements ImageTransformer {
    private final Logger log = LoggerFactory.getLogger(ResizeImageTransformerImpl.class);

    static final String TYPE = "resize";

    private static final int SYSTEM_MAX_DIMENSION = 50000;
    private static final String KEY_WIDTH = "width";
    private static final String KEY_HEIGHT = "height";

    @Override
    public Layer transform(final Layer layer, final ValueMap properties) {
        if(properties == null || properties.isEmpty()) {
            log.warn("Transform [ {} ] requires parameters.", TYPE);
            return layer;
        }

        log.debug("Transforming with [ {} ]", TYPE);

        int width = properties.get(KEY_WIDTH, 0);
        int height = properties.get(KEY_HEIGHT, 0);

        if(width > SYSTEM_MAX_DIMENSION) {
            width = SYSTEM_MAX_DIMENSION;
        }

        if(height > SYSTEM_MAX_DIMENSION) {
            height = SYSTEM_MAX_DIMENSION;
        }

        if(width < 1 && height < 1) {
            width = layer.getWidth();
            height = layer.getHeight();
        } else if(width < 1) {
            final float aspect =  (float)height / layer.getHeight();
            width =  Math.round(layer.getWidth() * aspect);
        } else if (height < 1) {
            final float aspect =  (float)width / layer.getWidth();
            height = Math.round(layer.getHeight() * aspect);
        }

        layer.resize(width, height);

        return layer;
    }
}
