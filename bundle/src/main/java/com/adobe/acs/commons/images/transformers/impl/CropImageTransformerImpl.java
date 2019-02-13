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
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;

/**
 * ACS AEM Commons - Image Transformer - Crop with Smart-Bounding
 */
@Component
@Property(
        name = ImageTransformer.PROP_TYPE,
        value = CropImageTransformerImpl.TYPE)
@Service
public class CropImageTransformerImpl implements ImageTransformer {
    private static  final Logger log = LoggerFactory.getLogger(CropImageTransformerImpl.class);

    static final String TYPE = "crop";

    private static final String KEY_BOUNDS = "bounds";

    private static final String KEY_SMART_BOUNDING = "smart";

    private static final int NUM_BOUNDS_PARAMS = 4;

    private static final int PARAM_INDEX_X = 0;

    private static final int PARAM_INDEX_Y = 1;

    private static final int PARAM_INDEX_WIDTH = 2;

    private static final int PARAM_INDEX_HEIGHT = 3;

    @Override
    public final Layer transform(final Layer layer, final ValueMap properties) {
        if (properties == null || properties.isEmpty()) {
            log.warn("Transform [ {} ] requires parameters.", TYPE);
            return layer;
        }

        log.debug("Transforming with [ {} ]", TYPE);

        final boolean smartBounding = properties.get(KEY_SMART_BOUNDING, true);
        final String[] bounds = StringUtils.split(properties.get(KEY_BOUNDS, ""), ",");

        if (bounds.length == NUM_BOUNDS_PARAMS) {
            int x = parseLength(bounds[PARAM_INDEX_X], layer.getWidth());
            int y = parseLength(bounds[PARAM_INDEX_Y], layer.getHeight());

            int width = parseLength(bounds[PARAM_INDEX_WIDTH], layer.getWidth());
            int height = parseLength(bounds[PARAM_INDEX_HEIGHT], layer.getHeight());

            Rectangle rectangle = new Rectangle();

            if (smartBounding) {
                rectangle = this.getSmartBounds(x, y, width, height, layer.getWidth(),
                        layer.getHeight());
            } else {
                rectangle.setBounds(x, y, width, height);
            }

            layer.crop(rectangle);

            if (smartBounding && layer.getWidth() != width || layer.getHeight() != height) {
                log.debug("SmartBounding resulted in an image of an incorrect size (based on crop params). "
                        + "resizing to: [ width: {}, height: {} ]", width, height);
                layer.resize(width, height);
            }
        }

        return layer;
    }

    @SuppressWarnings("squid:S2589")
    private Rectangle getSmartBounds(int x, int y, int width, int height, int layerWidth, int layerHeight) {
        final Rectangle rectangle = new Rectangle();

        final int x2 = x + width;
        final int y2 = y + height;

        if (x2 >= layerWidth && y2 >= layerHeight) {
            // Both exceed, pick the dimension to use as the "master" clip

            final float clipPercentX = ((float) (x2 - layerWidth) / width);
            final float clipPercentY = ((float) (y2 - layerHeight) / height);

            if (clipPercentX >= clipPercentY) {
                // The proportional amount to clip is greatest on the x-axis, so trim both dimensions by the same %
                return constrainByWidth(x, y, width, height, layerWidth, x2);
            } else {
                // The proportional amount to clip is greatest on the y-axis, so trim both dimensions by the same %
                return constrainByHeight(x, y, width, height, layerHeight, y2);
            }
        } else if (x2 >= layerWidth && y2 < layerHeight) {
            return constrainByWidth(x, y, width, height, layerWidth, x2);
        } else if (x2 < layerWidth && y2 >= layerHeight) {
            return constrainByHeight(x, y, width, height, layerHeight, y2);
        }

        // Crop within layer size
        rectangle.setBounds(x, y, width, height);
        return rectangle;
    }

    private Rectangle constrainByHeight(final int x, final int y, final int width, final int height,
                                        final int layerHeight, final int y2) {
        final Rectangle rectangle = new Rectangle();

        // Compute amount of overflow for y (that requires clipping)
        final int deltaY = y2 - layerHeight;

        // Compute amount to clip width (X) constrained by the % of total width that was removed  for deltaY
        // (Amount clipped from Y should be proportionately equals to amount clipped from Y)
        int deltaX = Math.round(((float) deltaY / height) * width);

        // Set the bounds to be the new clipped width/height
        rectangle.setBounds(x, y, width - deltaX, height - deltaY);

        return rectangle;
    }

    private Rectangle constrainByWidth(final int x, final int y, final int width, final int height,
                                       final int layerWidth, final int x2) {
        final Rectangle rectangle = new Rectangle();

        // Compute amount of overflow for x (that requires clipping)
        int deltaX = x2 - layerWidth;

        // Compute amount to clip height (Y) constrained by the % of total width that was removed  for deltaX
        // (Amount clipped from Y should be proportionately equals to amount clipped from X)
        int deltaY = Math.round(((float) deltaX / width) * height);

        // Set the bounds to be the new clipped width/height
        rectangle.setBounds(x, y, width - deltaX, height - deltaY);

        return rectangle;
    }

    private static int parseLength(String lengthStr, int totalLength) {
        int lengthPx;
        if (lengthStr.endsWith("%")) {
            String percentageStr = lengthStr.substring(0, lengthStr.length() - 1);
            double percentage = Double.parseDouble(percentageStr);
            lengthPx = (int) Math.round((percentage / 100) * totalLength);
        } else {
            lengthPx = Integer.parseInt(lengthStr);
        }
        return lengthPx;
    }

}
