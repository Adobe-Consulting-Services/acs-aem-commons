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
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * ACS AEM Commons - Image Transformer - Scale
 * ImageTransformer that scales the layer. Accepts two params: scale, round. Scale is a double value (e.g. "0.5") or a
 * percentage (e.g. "50%").
 * Rounding is one of "round" (using Math.round), "up" (using Math.ceil), or "down" (using Math.floor)
 * "round" being the default
 *
 */
@Component
@Properties({
        @Property(
                name = ImageTransformer.PROP_TYPE,
                value = ScaleImageTransformerImpl.TYPE
        )
})
@Service
public class ScaleImageTransformerImpl implements ImageTransformer {
    private static final Logger log = LoggerFactory.getLogger(ScaleImageTransformerImpl.class);

    static final String TYPE = "scale";

    private static final String KEY_SCALE = "scale";
    private static final String KEY_ROUND = "round";

    private static final String ROUND_UP = "up";
    private static final String ROUND_DOWN = "down";

    @Reference(target = "(" + ImageTransformer.PROP_TYPE + "=" + ResizeImageTransformerImpl.TYPE  +")")
    ImageTransformer resizeImageTransformer;

    @Override
    public final Layer transform(Layer layer, final ValueMap properties) {

        if (properties == null || properties.isEmpty()) {
            log.warn("Transform [ {} ] requires parameters.", TYPE);
            return layer;
        }

        log.debug("Transforming with [ {} ]", TYPE);

        Double scale = properties.get(KEY_SCALE, 1D);
        String round = StringUtils.trim(properties.get(KEY_ROUND, String.class));

        if (scale == null) {
            log.warn("Could not derive a Double value for key [ {} ] from value [ {} ]",
                    KEY_SCALE, properties.get(KEY_SCALE, String.class));
            scale = 1D;
        }

        if (scale != 1D) {

            int currentWidth = layer.getWidth();
            int currentHeight = layer.getHeight();

            double newWidth = scale * currentWidth;
            double newHeight = scale * currentHeight;

            if (StringUtils.equals(ROUND_UP, round)) {
                newWidth = (int) Math.ceil(newWidth);
                newHeight = (int) Math.ceil(newHeight);
            } else if (StringUtils.equals(ROUND_DOWN, round)) {
                newWidth = (int) Math.floor(newWidth);
                newHeight = (int) Math.floor(newHeight);
            } else {
                // "round"
                newWidth = (int) Math.round(newWidth);
                newHeight = (int) Math.round(newHeight);
            }


            // Invoke the ResizeImageTransformer with the new values

            final ValueMap params = new ValueMapDecorator(new HashMap<String, Object>());
            params.put(ResizeImageTransformerImpl.KEY_WIDTH, (int)newWidth);
            params.put(ResizeImageTransformerImpl.KEY_HEIGHT, (int)newHeight);

            layer = resizeImageTransformer.transform(layer, params);
        }

        return layer;
    }

}
