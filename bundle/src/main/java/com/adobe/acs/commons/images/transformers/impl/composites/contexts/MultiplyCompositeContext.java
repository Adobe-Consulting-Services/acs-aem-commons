/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
package com.adobe.acs.commons.images.transformers.impl.composites.contexts;

import java.awt.CompositeContext;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * Applies a multiply blend to the models.
 * 
 * Follows the rules defined here:
 * http://helpx.adobe.com/after-effects/using/blending
 * -modes-layer-styles.html#Multiply
 * 
 * Based on
 * http://www.java2s.com/Code/Java/2D-Graphics-GUI/BlendCompositeDemo.htm
 */
public class MultiplyCompositeContext implements CompositeContext {

    private static final int ALPHA_MASK = 24;
    private static final int BLEND_SHIFT = 8;

    private final float alpha;

    public MultiplyCompositeContext(float alpha) {
        this.alpha = alpha;
    }

    @Override
    public void compose(Raster src, Raster destIn, WritableRaster dstOut) {

        int width = Math.min(src.getWidth(), destIn.getWidth());
        int height = Math.min(src.getHeight(), destIn.getHeight());

        int[] srcPixels = new int[width];
        int[] destPixels = new int[width];

        // Get a row of pixels.
        for (int y = 0; y < height; y++) {
            src.getDataElements(0, y, width, 1, srcPixels);
            destIn.getDataElements(0, y, width, 1, destPixels);

            // Each pixel in the row
            for (int x = 0; x < width; x++) {

                // pixels are stored as INT_ARGB
                int srcPixel = srcPixels[x];
                int destPixel = destPixels[x];
                int result = 0;
                int tmp = 0;

                for (ColorMask mask : ColorMask.values()) {

                    int srcColor = (srcPixel >> mask.getMask()) & ColorMask.MAX_DEPTH;
                    int destColor = (destPixel >> mask.getMask()) & ColorMask.MAX_DEPTH;
                    tmp = blendColor(srcColor, destColor);

                    tmp = processColorOpacity(tmp, destColor);
                    result = result | (tmp << mask.getMask());
                }

                int srcAlpha = (srcPixel >> ALPHA_MASK) & ColorMask.MAX_DEPTH;
                int destAlpha = (destPixel >> ALPHA_MASK) & ColorMask.MAX_DEPTH;

                tmp = blendAlpha(srcAlpha, destAlpha);
                tmp = processAlphaOpacity(tmp, destAlpha);
                result = result | (tmp << ALPHA_MASK);
                destPixels[x] = result;
            }
            dstOut.setDataElements(0, y, width, 1, destPixels);
        }

    }

    private int blendColor(int src, int dest) {
        return (src * dest) >> BLEND_SHIFT;

    }

    private int processColorOpacity(int blended, int dest) {
        int tmp = blended - dest;
        tmp = (int) (tmp * alpha);
        tmp = dest + tmp;
        tmp = tmp & ColorMask.MAX_DEPTH;
        return tmp;
    }

    private int blendAlpha(int src, int dest) {
        int tmp = (src * dest) / ColorMask.MAX_DEPTH;
        tmp = src + dest - tmp;
        return Math.min(ColorMask.MAX_DEPTH, tmp);
    }

    private int processAlphaOpacity(int blended, int dest) {

        int tmp = blended - dest;
        tmp = (int) (tmp * alpha);
        tmp = dest - tmp;
        tmp = tmp & ColorMask.MAX_DEPTH;
        return tmp;

    }

    @Override
    public void dispose() {
        // no-op
    }

    public float getAlpha() {
        return alpha;
    }
}
