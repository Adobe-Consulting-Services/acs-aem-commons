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
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ACS AEM Commons - Image Transformer - Scale
 * ImageTransformer that scales the layer. Accepts two params: scale, round. Scale is a double value (e.g. "0.5") or a
 * percentage (e.g. "50%").
 * Rounding is one of "round" (using Math.round), "up" (using Math.ceil) or "down" (using Math.floor) with "round"
 * being the default
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

    private Map<String, ImageTransformer> imageTransformers = new ConcurrentHashMap<String, ImageTransformer>();


    @Reference
    ResizeImageTransformerImpl resizeImageTransformer;

    @Override
    public final Layer transform(Layer layer, final ValueMap properties) {

        if (properties == null || properties.isEmpty()) {
            log.warn("Transform [ {} ] requires parameters.", TYPE);
            return layer;
        }

        log.debug("Transforming with [ {} ]", TYPE);

        String scaleString = properties.get(KEY_SCALE, "1");
        String round = properties.get(KEY_ROUND, "round");

        double scale = 1;
        try {
            if (scaleString.endsWith("%")) {
                double percentage = Double.parseDouble(StringUtils.substringBeforeLast(scaleString, "%"));
                scale = percentage / 100;
            }
            else {
                scale = Double.parseDouble(scaleString);
            }
        }
        catch (NumberFormatException e) {
            scale = 1;
        }

        if (scale != 1) {

            int currentWidth = layer.getWidth();
            int currentHeight = layer.getHeight();

            double newWidth = scale * currentWidth;
            double newHeight = scale * currentHeight;

            if ("up".equals(round)) {
                newWidth = Math.ceil(newWidth);
                newHeight = Math.ceil(newHeight);
            }
            else if ("down".equals(round)) {
                newWidth = (int) Math.floor(newWidth);
                newHeight = (int) Math.floor(newHeight);
            }
            else { // "round"
                newWidth = (int) Math.round(newWidth);
                newHeight = (int) Math.round(newHeight);
            }

            layer = resizeImageTransformer.resize(layer, (int)newWidth, (int)newHeight);
        }

        return layer;
    }

}
