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
import java.awt.Dimension;

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
 * ACS AEM Commons - Image Transformer - Letter/Pillar Box ImageTransformer that
 * resizes the layer. Accepts two Integer params: height and width. If either is
 * left blank the missing dimension will be computed based on the original
 * layer's aspect ratio. If the newly resized image does not fit into the
 * original dimensions, this will create a background layer
 */
//@formatter:off
@Component(
        service = ImageTransformer.class,
        property = {
                ImageTransformer.PROP_TYPE + "=" + LetterPillarBoxImageTransformerImpl.TYPE
        }
)
@Designate(ocd = LetterPillarBoxImageTransformerImpl.Config.class)
//@formatter:on
public class LetterPillarBoxImageTransformerImpl implements ImageTransformer {
    private static final Logger log = LoggerFactory.getLogger(LetterPillarBoxImageTransformerImpl.class);

    static final String TYPE = "letter-pillar-box";

    private static final String KEY_WIDTH = "width";
    private static final String KEY_WIDTH_ALIAS = "w";

    private static final String KEY_HEIGHT = "height";
    private static final String KEY_HEIGHT_ALIAS = "h";

    private static final String KEY_ALPHA = "alpha";
    private static final String KEY_ALPHA_ALIAS = "a";

    private static final String KEY_COLOR = "color";
    private static final String KEY_COLOR_ALIAS = "c";

    private static final Color TRANSPARENT = new Color(255, 255, 255, 0);

    private static final int MAX_ALPHA = 255;

    private static final int DEFAULT_MAX_DIMENSION = 50000;
    private int maxDimension = DEFAULT_MAX_DIMENSION;

    @ObjectClassDefinition(
            name = "ACS AEM Commons - Named Image Transform - Letter Pillar Box"
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

        if ((properties == null) || properties.isEmpty()) {
            log.warn("Transform [ {} ] requires parameters.", TYPE);
            return layer;
        }

        log.debug("Transforming with [ {} ]", TYPE);

        Dimension newSize = getResizeDimensions(properties, layer);
        Color color = getColor(properties);
        Layer resized = resize(layer, newSize);

        Layer result = build(newSize, resized, color);

        return result;
    }

    /*
     * Creates the actual pillar/letter boxing.
     */
    private Layer build(Dimension size, Layer img, Color color) {

        Layer merged = createLayer(size, color);

        int startXpos = 0;
        int startYpos = 0;
        int imgHeight = img.getHeight();
        int imgWidth = img.getWidth();
        if (imgHeight == size.height) {
            // Pillar
            startXpos = calculateStartPosition(size.width, imgWidth);
        } else if (imgWidth == size.width) {
            // Letter
            startYpos = calculateStartPosition(size.height, imgHeight);
        }

        merged.blit(img, startXpos, startYpos, imgWidth, imgHeight, 0, 0);
        return merged;
    }

    Layer createLayer(Dimension size, Color color) {
        return new Layer(size.width, size.height, color);
    }

    /*
     * Resizes the layer but keeps original aspect ratio. Thus preparing it for
     * the boxing.
     */
    private Layer resize(Layer original, Dimension newDimensions) {

        final Dimension origDimensions = new Dimension(original.getWidth(), original.getHeight());
        final int fixedDimension = getFixedDimension(origDimensions, newDimensions);
        float newWidth = newDimensions.width;
        float newHeight = newDimensions.height;

        if (fixedDimension < 0) {

            // Height is "fixed", calculate width
            newWidth = (origDimensions.width * newDimensions.height) / (float) origDimensions.height;
        } else if (fixedDimension > 0) {

            // Width is "fixed", calculate height
            newHeight = (newDimensions.width * origDimensions.height) / (float) origDimensions.width;
        }

        original.resize(Math.round(newWidth), Math.round(newHeight));
        return original;
    }

    /*
     * Calculates whether width or height is being used for resize basis.
     *
     * Returns an indicator value on which dimension is the "fixed" dimension
     *
     * Zero if the aspect ratios are the same Negative if the width should be
     * fixed Positive if the height should be fixed
     *
     * @param start the dimensions of the original image
     *
     * @param end the dimensions of the final image
     *
     * @return a value indicating which dimension is fixed
     */
    private int getFixedDimension(Dimension start, Dimension end) {
        double startRatio = start.getWidth() / start.getHeight();
        double finalRatio = end.getWidth() / end.getHeight();
        return Double.compare(startRatio, finalRatio);
    }

    private Dimension getResizeDimensions(final ValueMap properties, final Layer layer) {

        int targetWidth = properties.get(KEY_WIDTH, properties.get(KEY_WIDTH_ALIAS, 0));
        int targetHeight = properties.get(KEY_HEIGHT, properties.get(KEY_HEIGHT_ALIAS, 0));

        int startWidth = layer.getWidth();
        int startHeight = layer.getHeight();

        if (targetWidth > maxDimension) {
            targetWidth = maxDimension;
        }

        if (targetHeight > maxDimension) {
            targetHeight = maxDimension;
        }

        if ((targetWidth < 1) && (targetHeight < 1)) {
            targetWidth = startWidth;
            targetHeight = startHeight;
        } else if (targetWidth < 1) {
            final float aspect = (float) targetHeight / startHeight;
            targetWidth = Math.round(startWidth * aspect);
        } else if (targetHeight < 1) {
            final float aspect = (float) targetWidth / startWidth;
            targetHeight = Math.round(startHeight * aspect);
        }
        return new Dimension(targetWidth, targetHeight);
    }

    private Color getColor(final ValueMap properties) {
        String hexcolor = properties.get(KEY_COLOR, properties.get(KEY_COLOR_ALIAS, String.class));
        int alpha = normalizeAlpha(properties.get(KEY_ALPHA, properties.get(KEY_ALPHA_ALIAS, 0.0)).floatValue());

        Color color = TRANSPARENT;
        if (hexcolor != null) {
            try {
                Color parsed = Color.decode("0x" + hexcolor);
                color = new Color(parsed.getRed(), parsed.getGreen(), parsed.getBlue(), alpha);
            } catch (NumberFormatException ex) {
                log.warn("Invalid hex color specified: {}", hexcolor);
                color = TRANSPARENT;
            }
        }
        return color;
    }

    private int normalizeAlpha(float alpha) {
        if (alpha > 1) {
            alpha = 1f;
        } else if (alpha < 0) {
            alpha = 0f;
        }

        return Math.round(alpha * MAX_ALPHA);
    }

    private int calculateStartPosition(int originalSize, int newSize) {
        int diff = originalSize - newSize;
        int start = diff / 2;
        return start;
    }

    @Activate
    protected final void activate(LetterPillarBoxImageTransformerImpl.Config config) {
        maxDimension = config.max$_$dimension();
    }

}
