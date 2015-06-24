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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.images.ImageTransformer;
import com.adobe.acs.commons.images.transformers.impl.composites.MultiplyBlendComposite;
import com.day.image.Layer;

/**
 * ACS AEM Commons - Image Transformer - Multiply Color Blend
 */
//@formatter:off
@Component
@Properties({
        @Property(
                name = ImageTransformer.PROP_TYPE,
                value = MultiplyBlendImageTransformerImpl.TYPE
        )
})
@Service
//@formatter:on
public class MultiplyBlendImageTransformerImpl implements ImageTransformer {
    private static final Logger log = LoggerFactory.getLogger(MultiplyBlendImageTransformerImpl.class);

    static final String TYPE = "multiply";

    private static final String KEY_ALPHA = "alpha";
    private static final String KEY_ALPHA_ALIAS = "a";

    private static final String KEY_COLOR = "color";
    private static final String KEY_COLOR_ALIAS = "c";

    private static final String KEY_RED = "red";
    private static final String KEY_RED_ALIAS = "r";

    private static final String KEY_GREEN = "green";
    private static final String KEY_GREEN_ALIAS = "g";

    private static final String KEY_BLUE = "blue";
    private static final String KEY_BLUE_ALIAS = "b";

    private static final int DEFAULT_COLOR_VALUE = 255;

    @Override
    public final Layer transform(final Layer layer, final ValueMap properties) {

        if (properties == null || properties.isEmpty()) {
            log.warn("Transform [ {} ] requires parameters.", TYPE);
            return layer;
        }

        log.debug("Transforming with [ {} ]", TYPE);

        float alpha = normalizeAlpha(properties.get(KEY_ALPHA, properties.get(KEY_ALPHA_ALIAS, 0.0)).floatValue());

        Color color = getColor(properties);
        Layer filter = new Layer(layer.getWidth(), layer.getHeight(), color);

        BufferedImage image = merge(layer.getImage(), filter.getImage(), alpha);
        Layer result = new Layer(image);
        return result;
    }

    private BufferedImage merge(final BufferedImage original, final BufferedImage colorBlend, float alpha) {
        BufferedImage image = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB);

        // Create the background
        Graphics2D graphics = image.createGraphics();
        graphics.setComposite(AlphaComposite.Clear);
        graphics.fillRect(0, 0, original.getWidth(), original.getHeight());

        // Add the original image
        graphics.setComposite(AlphaComposite.Src);
        graphics.drawImage(original, 0, 0, null);

        // Merge in the color filter
        graphics.setComposite(new MultiplyBlendComposite(alpha));
        graphics.drawImage(colorBlend, 0, 0, null);
        graphics.dispose();

        return image;
    }

    private Color getColor(final ValueMap properties) {
        Color color = getHexColor(properties);

        if (color == null) {
            color = getRGBColor(properties);
        }
        return color;
    }

    private Color getHexColor(final ValueMap properties) {
        String hexcolor = properties.get(KEY_COLOR, properties.get(KEY_COLOR_ALIAS, String.class));
        Color color = null;
        if (hexcolor != null) {
            try {
                color = Color.decode("0x" + hexcolor);
            } catch (NumberFormatException ex) {
                log.warn("Invalid hex color specified: {}", hexcolor);
            }
        }
        return color;
    }

    private Color getRGBColor(final ValueMap properties) {
        Color color;
        int red = normalizeRGB(properties.get(KEY_RED, properties.get(KEY_RED_ALIAS, DEFAULT_COLOR_VALUE)));
        int green = normalizeRGB(properties.get(KEY_GREEN, properties.get(KEY_GREEN_ALIAS, DEFAULT_COLOR_VALUE)));
        int blue = normalizeRGB(properties.get(KEY_BLUE, properties.get(KEY_BLUE_ALIAS, DEFAULT_COLOR_VALUE)));
        color = new Color(red, green, blue);
        return color;
    }

    private float normalizeAlpha(float alpha) {
        if (alpha > 1) {
            alpha = 1f;
        } else if (alpha < 0) {
            alpha = 0f;
        }

        return alpha;
    }

    private int normalizeRGB(int rgbValue) {
        if (rgbValue > DEFAULT_COLOR_VALUE) {
            rgbValue = DEFAULT_COLOR_VALUE;
        } else if (rgbValue < 0) {
            rgbValue = 0;
        }
        return rgbValue;
    }

}
