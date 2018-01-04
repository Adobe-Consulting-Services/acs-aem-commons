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

package com.adobe.acs.commons.images;

import aQute.bnd.annotation.ConsumerType;

import com.day.image.Layer;

import org.apache.sling.api.resource.ValueMap;

@ConsumerType
@SuppressWarnings("squid:S1214")
public interface ImageTransformer {
    /**
     * OSGi Property used to identify the ImageTransformer.
     */
    String PROP_TYPE = "type";

    /**
     * Transform the provided layer using the transformation parameters provided in properties.
     *
     * @param layer the image layer to transform
     * @param properties transformation parameters
     * @return the transformed layer; or if layer could not be transformed (invalid properties) the layer unmodified
     */
    Layer transform(Layer layer, ValueMap properties);
}