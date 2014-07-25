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
package com.adobe.acs.commons.images.transformers.impl.composites;

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.RasterFormatException;

import com.adobe.acs.commons.images.transformers.impl.composites.contexts.MultiplyCompositeContext;

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
public class MultiplyBlendComposite implements Composite {

    private static final DirectColorModel COLOR_MODEL = (DirectColorModel) ColorModel.getRGBdefault();

    private final float alpha;

    public MultiplyBlendComposite(float alpha) {
        this.alpha = alpha;
    }

    @Override
    public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {

        if (!isValidColorModel(srcColorModel) || !isValidColorModel(dstColorModel)) {
            throw new RasterFormatException("Invalid color model provided.");
        }

        return new MultiplyCompositeContext(alpha);

    }

    private static boolean isValidColorModel(ColorModel cm) {

        if (!(cm instanceof DirectColorModel)) {
            return false;
        }

        DirectColorModel dcm = (DirectColorModel) cm;

        return (dcm.getAlphaMask() == COLOR_MODEL.getAlphaMask() && dcm.getRedMask() == COLOR_MODEL.getRedMask()
                && dcm.getGreenMask() == COLOR_MODEL.getGreenMask() && dcm.getBlueMask() == COLOR_MODEL.getBlueMask());
    }

}
