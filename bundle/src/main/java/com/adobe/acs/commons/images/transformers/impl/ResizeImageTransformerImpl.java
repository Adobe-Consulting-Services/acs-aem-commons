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

import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.images.ImageTransformer;
import com.day.image.Layer;

/**
 * ACS AEM Commons - Image Transformer - Resize
 * ImageTransformer that resizes the layer. Accepts two Integer params: height and width.
 * If either is left blank the missing dimension will be computed based on the original layer's
 * aspect ratio
 */

@Component(
        service = ImageTransformer.class,
        property = {
                ImageTransformer.PROP_TYPE + "=" + ResizeImageTransformerImpl.TYPE
        }
)
@Designate(ocd = ResizeImageTransformerImpl.Config.class)
public class ResizeImageTransformerImpl implements ImageTransformer {
    private static final Logger log = LoggerFactory.getLogger(ResizeImageTransformerImpl.class);

    static final String TYPE = "resize";

    static final String KEY_WIDTH = "width";
    static final String KEY_WIDTH_ALIAS = "w";

    static final String KEY_HEIGHT = "height";
    static final String KEY_HEIGHT_ALIAS = "h";

    private static final int DEFAULT_MAX_DIMENSION = 50000;
    private int maxDimension = DEFAULT_MAX_DIMENSION;

    @ObjectClassDefinition(
            name = "ACS AEM Commons - Named Image Transform - Resize"
    )
    public @interface Config {
        @AttributeDefinition(
                name = "Max dimension in px",
                description = "Maximum size height and width can be re-sized to. [ Default: 50000 ]",
                defaultValue = "" + DEFAULT_MAX_DIMENSION
        )
        int max$_$dimension() default DEFAULT_MAX_DIMENSION;

    }

    @Override
    public final Layer transform(final Layer layer, final ValueMap properties) {
        if (properties == null || properties.isEmpty()) {
            log.warn("Transform [ {} ] requires parameters.", TYPE);
            return layer;
        }

        log.debug("Transforming with [ {} ]", TYPE);

        int width = properties.get(KEY_WIDTH, properties.get(KEY_WIDTH_ALIAS, 0));
        int height = properties.get(KEY_HEIGHT, properties.get(KEY_HEIGHT_ALIAS, 0));

        if (width > maxDimension) {
            width = maxDimension;
        }

        if (height > maxDimension) {
            height = maxDimension;
        }

        if (width < 1 && height < 1) {
            width = layer.getWidth();
            height = layer.getHeight();
        } else if (width < 1) {
            final float aspect = (float) height / layer.getHeight();
            width = Math.round(layer.getWidth() * aspect);
        } else if (height < 1) {
            final float aspect = (float) width / layer.getWidth();
            height = Math.round(layer.getHeight() * aspect);
        }

        layer.resize(width, height);

        return layer;
    }


    @Activate
    protected final void activate(ResizeImageTransformerImpl.Config config) {
        maxDimension = config.max$_$dimension();
    }
}
