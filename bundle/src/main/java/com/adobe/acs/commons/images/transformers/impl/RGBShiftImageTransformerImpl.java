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

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.images.ImageTransformer;
import com.day.image.Layer;

/**
 * ACS AEM Commons - Image Transformer - RGB Shift
 */
//@formatter:off
@Component
@Properties({
        @Property(
                name = ImageTransformer.PROP_TYPE,
                value = RGBShiftImageTransformerImpl.TYPE
        )
})
@Service
//@formatter:on
public class RGBShiftImageTransformerImpl implements ImageTransformer {
    private static final Logger log = LoggerFactory.getLogger(RGBShiftImageTransformerImpl.class);

    static final String TYPE = "rgb-shift";

    private static final String KEY_RED = "red";
    private static final String KEY_RED_ALIAS = "r";

    private static final String KEY_GREEN = "green";
    private static final String KEY_GREEN_ALIAS = "g";

    private static final String KEY_BLUE = "blue";
    private static final String KEY_BLUE_ALIAS = "b";

    private static final double DEFAULT_SHIFT_VALUE = 0;
    private static final float MAX_SHIFT_VALUE = 1f;
    private static final float MIN_SHIFT_VALUE = -1f;

    private static final int MIN_COLOR_VALUE = 0;
    private static final int MAX_COLOR_VALUE = 255;

    @Override
    public final Layer transform(final Layer layer, final ValueMap properties) {

        if (properties == null || properties.isEmpty()) {
            log.warn("Transform [ {} ] requires parameters.", TYPE);
            return layer;
        }

        log.debug("Transforming with [ {} ]", TYPE);

        float red = normalizeRGB(properties.get(KEY_RED, properties.get(KEY_RED_ALIAS, DEFAULT_SHIFT_VALUE))
                .floatValue());
        float green = normalizeRGB(properties.get(KEY_GREEN, properties.get(KEY_GREEN_ALIAS, DEFAULT_SHIFT_VALUE))
                .floatValue());
        float blue = normalizeRGB(properties.get(KEY_BLUE, properties.get(KEY_BLUE_ALIAS, DEFAULT_SHIFT_VALUE))
                .floatValue());

        int redShift = Math.round(red * MAX_COLOR_VALUE);
        int greenShift = Math.round(green * MAX_COLOR_VALUE);
        int blueShift = Math.round(blue * MAX_COLOR_VALUE);

        BufferedImage image = shift(layer.getImage(), redShift, greenShift, blueShift);

        Layer result = new Layer(image);
        return result;
    }

    private BufferedImage shift(final BufferedImage original, final int redShift, final int greenShift,
            final int blueShift) {
        BufferedImage updated = new BufferedImage(original.getWidth(), original.getHeight(),
                BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < original.getWidth(); x++) {
            for (int y = 0; y < original.getHeight(); y++) {
                Color pixel = new Color(original.getRGB(x, y));

                int red = pixel.getRed() + redShift;
                if (red > MAX_COLOR_VALUE) {
                    red = MAX_COLOR_VALUE;
                } else if (red < MIN_COLOR_VALUE) {
                    red = MIN_COLOR_VALUE;
                }

                int green = pixel.getGreen() + greenShift;
                if (green > MAX_COLOR_VALUE) {
                    green = MAX_COLOR_VALUE;
                } else if (green < MIN_COLOR_VALUE) {
                    green = MIN_COLOR_VALUE;
                }

                int blue = pixel.getBlue() + blueShift;
                if (blue > MAX_COLOR_VALUE) {
                    blue = MAX_COLOR_VALUE;
                } else if (blue < MIN_COLOR_VALUE) {
                    blue = MIN_COLOR_VALUE;
                }

                Color shifted = new Color(red, green, blue);
                updated.setRGB(x, y, shifted.getRGB());

            }
        }

        return updated;
    }

    private float normalizeRGB(float rgbValue) {
        if (rgbValue > MAX_SHIFT_VALUE) {
            rgbValue = MAX_SHIFT_VALUE;
        } else if (rgbValue < MIN_SHIFT_VALUE) {
            rgbValue = MIN_SHIFT_VALUE;
        }
        return rgbValue;
    }

}
