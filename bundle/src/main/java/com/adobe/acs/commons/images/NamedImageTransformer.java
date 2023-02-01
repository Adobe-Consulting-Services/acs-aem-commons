/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */

package com.adobe.acs.commons.images;

import org.osgi.annotation.versioning.ProviderType;

import com.day.image.Layer;
import org.apache.sling.api.resource.ValueMap;

import java.util.Map;

@ProviderType
@SuppressWarnings("squid:S1214")
public interface NamedImageTransformer {
    /**
     * The OSGi config property used to identify the named transform.
     */
    String PROP_NAME = "name";

    /**
     * Transforms the param layer using all the parameterized image transformers defined in this instances OSGi
     * configuration.
     *
     * @param layer the image layer to transform
     * @return the transform image layer; or if no modifications are possible (invalid named transforms/named
     * transform parameters) the layer unmodified
     */
    Layer transform(Layer layer);

    /**
     * Returns the name of this transform as defined in this instance's OSGi configuration. The transform name is also
     * used to construct URLs for the <code>NamedTransformImageServlet</code>
     *
     * @return the transform name
     */
    String getTransformName();

    /**
     * @return the ImageTransforms types and their params for this Named Image Transform
     */
    Map<String, ValueMap> getImageTransforms();
}
