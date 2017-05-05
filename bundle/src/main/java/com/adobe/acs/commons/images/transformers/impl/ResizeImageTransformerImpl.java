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
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.images.ImageTransformer;
import com.day.cq.wcm.foundation.AdaptiveImageHelper;
import com.day.image.Layer;

/**
 * ACS AEM Commons - Image Transformer - Resize
 * ImageTransformer that resizes the layer. Accepts two Integer params: height and width.
 * If either is left blank the missing dimension will be computed based on the original layer's
 * aspect ratio
 */
@Component
@Properties({
        @Property(
                name = ImageTransformer.PROP_TYPE,
                value = ResizeImageTransformerImpl.TYPE
        )
})
@Service
public class ResizeImageTransformerImpl implements ImageTransformer {
    private static final Logger log = LoggerFactory.getLogger(ResizeImageTransformerImpl.class);

    static final String TYPE = "resize";

    static final String KEY_WIDTH = "width";
    static final String KEY_WIDTH_ALIAS = "w";

    static final String KEY_HEIGHT = "height";
    static final String KEY_HEIGHT_ALIAS = "h";

    private static final int DEFAULT_MAX_DIMENSION = 50000;
    private int maxDimension = DEFAULT_MAX_DIMENSION;
    @Property(label = "Max dimension in px",
            description = "Maximum size height and width can be re-sized to. [ Default: 50000 ]",
            intValue = DEFAULT_MAX_DIMENSION)
    public static final String PROP_MAX_DIMENSION = "max-dimension";

    @Override
    public final Layer transform(final Layer layer, final ValueMap properties) {
        if (properties == null || properties.isEmpty()) {
            log.warn("Transform [ {} ] requires parameters.", TYPE);
            return layer;
        }

        log.debug("Transforming with [ {} ]", TYPE);

        int width = properties.get(KEY_WIDTH, properties.get(KEY_WIDTH_ALIAS, 0));
        int height = properties.get(KEY_HEIGHT, properties.get(KEY_HEIGHT_ALIAS, 0));
        
        int originalWidth = layer.getWidth();
        int originalHeight = layer.getHeight();

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

      //If original image aspect ratio is different from the final image aspect ratio, then resize it and give letterboxing/pillarboxing
        if(isExpectedAspectRatioSameAsOriginal(originalWidth, originalHeight, width, height) != 0) {
            if(isExpectedAspectRatioSameAsOriginal(originalWidth, originalHeight, width, height) < 0) {
                ////If original aspect ratio is lesser than the final aspect ratio, fix the height of the final image and calculate the width based on the original image aspect ratio
                int widthToResize = (height * originalWidth)/originalHeight;
                layer.resize(widthToResize, height);
            } else if(isExpectedAspectRatioSameAsOriginal(originalWidth, originalHeight, width, height) > 0){
                //If original aspect ratio is greater than the final aspect ratio, fix the width of the final image and calculate the height based on the original image aspect ratio
                int heightToResize = (width * originalHeight)/originalWidth;
                layer.resize(width, heightToResize);
            }
            Layer backgroundLayer = placeResizedImageLayerOverFinalLayer(layer,
                    width, height);
            return backgroundLayer;
        } else {
            layer.resize(width, height);
        }

        return layer;
    }

    private double isExpectedAspectRatioSameAsOriginal(int originalWidth, int originalHeight, int finalWidth, int finalHeight) {
        float aspectRatioOfOriginal = originalWidth / (float)originalHeight;
        float aspectRatioOfFinal = finalWidth / (float)finalHeight;
        return Double.compare(aspectRatioOfOriginal, aspectRatioOfFinal);
    }

    private Layer placeResizedImageLayerOverFinalLayer(final Layer layer,
            int expectedWidth, int expectedHeight) {
        Layer backgroundLayer = AdaptiveImageHelper.renderScaledPlaceholderImage(expectedWidth, expectedHeight);
        backgroundLayer.colorize(Color.BLACK, Color.BLACK);
        addLayer(backgroundLayer, layer);
        return backgroundLayer;
    }
    
    //Used to place one layer above another layer
    private void addLayer(Layer backgroundLayer, Layer resizedImageLayer) {
        if(backgroundLayer.getHeight() == resizedImageLayer.getHeight()) {
            //Pillarboxing
            backgroundLayer.blit(resizedImageLayer, (int)((backgroundLayer.getWidth() - resizedImageLayer.getWidth())/2), 0, resizedImageLayer.getWidth(),
                resizedImageLayer.getHeight(), 0, 0);
        } else if (backgroundLayer.getWidth() == resizedImageLayer.getWidth()) {
            //Letterboxing
            backgroundLayer.blit(resizedImageLayer, 0, (int)((backgroundLayer.getHeight() - resizedImageLayer.getHeight())/2), resizedImageLayer.getWidth(),
                    resizedImageLayer.getHeight(), 0, 0);
        } 
    }
    @Activate
    protected final void activate(final Map<String, String> config) {
        maxDimension = PropertiesUtil.toInteger(config.get(PROP_MAX_DIMENSION), DEFAULT_MAX_DIMENSION);
    }
}
