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
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ACS AEM Commons - Image Transformer - Sharpening
 * ImageTransformer that sharpens the layer. Accepts op_usm as parameter.
 *
 * op_usm is unsharp mask technique. It accepts 2 comma separated values: amount, radius.
 * amount: filter strength factor (real 0…5)
 * radius: filter kernel radius in pixels (real 0…250)
 *
 */
@Component
@Properties({
        @Property(name = ImageTransformer.PROP_TYPE, value = SharpenImageTransformerImpl.TYPE)
})
@Service
public class SharpenImageTransformerImpl implements ImageTransformer {
    private static final Logger log = LoggerFactory.getLogger(SharpenImageTransformerImpl.class);

    static final String TYPE = "sharpen";

    private static final String KEY_UNSHARP_MASK = "op_usm";
    private static final int NUM_SHARPEN_PARAMS = 2;


    @Override
    public final Layer transform(final Layer layer, final ValueMap properties) {

        if (properties == null || properties.isEmpty()) {
            log.warn("Transform [ {} ] requires parameters.", TYPE);
            return layer;
        }

        log.debug("Transforming with [ {} ]", TYPE);

        String unsharpenMask = StringUtils.trim(properties.get(KEY_UNSHARP_MASK, String.class));

        try {
            if(!unsharpenMask.isEmpty()) {
                String[] param = unsharpenMask.split(",");

                // Support is provided for amount and radius only.
                if(param.length == NUM_SHARPEN_PARAMS) {
                    float amount = Float.parseFloat(param[0]);
                    float radius = Float.parseFloat(param[1]);
                    layer.sharpen(amount, radius);
                } else {
                    log.warn("Transform [ {} ] requires 2 parameters.", TYPE);
                }
            }
        } catch (NumberFormatException exception) {
            log.warn("Transform [ {} ] requires floating type values.", TYPE);
            log.error("Exception occured while parsing string to float", exception);
        }
        return layer;
    }
}
