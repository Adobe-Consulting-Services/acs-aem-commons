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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component(
        label = "ACS AEM Commons - Image Transformer - Quality"
)
@Properties({
        @Property(
                name = ImageTransformer.PROP_TYPE,
                value = QualityImageTransformerImpl.TYPE,
                propertyPrivate = true
        )
})
@Service
public class QualityImageTransformerImpl implements ImageTransformer {
    private static final Logger log = LoggerFactory.getLogger(QualityImageTransformerImpl.class);

    static final String TYPE = "quality";

    private static final String KEY_QUALITY = "quality";

    private static final int MAX_QUALITY = 100;

    private static final int MIN_QUALITY = 0;

    private static final String MIME_TYPE_GIF = "image/gif";

    private static final float IMAGE_GIF_MAX_QUALITY = 255;

    @Override
    public final Layer transform(final Layer layer, final ValueMap properties) {
        log.debug("Transforming with [ {} ]", TYPE);

        int quality = properties.get(KEY_QUALITY, MAX_QUALITY);

        if (quality >= MAX_QUALITY) {
            // Quality be being requested is max aka original layer quality, so do nothing.
            return layer;
        } else if (quality < MIN_QUALITY) {
            quality = MIN_QUALITY;
        }

        double convertedQuality = quality / 100f;

        if (StringUtils.equals(MIME_TYPE_GIF, layer.getMimeType())) {
            convertedQuality = convertedQuality * IMAGE_GIF_MAX_QUALITY;
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            layer.write(layer.getMimeType(), convertedQuality, baos);
            return new Layer(new ByteArrayInputStream(baos.toByteArray()));
        } catch (IOException e) {
            log.warn("Could not collect image as an OutputStream to adjust Image quality.");
        }

        return layer;
    }
}
