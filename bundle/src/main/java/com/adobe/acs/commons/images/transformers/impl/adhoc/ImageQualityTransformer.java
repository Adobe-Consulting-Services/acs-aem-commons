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

package com.adobe.acs.commons.images.transformers.impl.adhoc;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageQualityTransformer {
    private static final Logger log = LoggerFactory.getLogger(ImageQualityTransformer.class);

    private static final String KEY_QUALITY = "quality";

    // Magic number used by AEM as the default/fallback quality
    private static final int DEFAULT_QUALITY = 82;

    private static final int MAX_QUALITY = 100;

    private static final int MIN_QUALITY = 0;

    private static final String MIME_TYPE_GIF = "image/gif";

    private static final int IMAGE_GIF_MAX_QUALITY = 255;

    /**
     * Derive and normalize quality parameters.
     *
     * @param mimeType   the mimeType
     * @param properties the ValueMap containing the quality params
     * @return the quality transformation for the specified mimeType
     */
    public static final double getQuality(final String mimeType, final ValueMap properties) {

        log.debug("Transforming with [ quality ]");

        double quality = properties.get(KEY_QUALITY, DEFAULT_QUALITY);

        if (quality > MAX_QUALITY) {
            quality = DEFAULT_QUALITY;
        } else if (quality < MIN_QUALITY) {
            quality = MIN_QUALITY;
        }

        quality = quality / 100D;

        if (StringUtils.equals(MIME_TYPE_GIF, mimeType)) {
            quality = quality * IMAGE_GIF_MAX_QUALITY;
        }

        return quality;
    }
}
