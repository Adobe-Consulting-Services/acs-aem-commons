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

package com.adobe.acs.commons.images.impl;

import com.adobe.acs.commons.images.ImageTransformer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import com.adobe.acs.commons.images.NamedImageTransformer;
import com.adobe.acs.commons.util.ParameterUtil;
import com.adobe.acs.commons.util.TypeUtil;
import com.day.image.Layer;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Component(property = "webconsole.configurationFactory.nameHint=Transformer: {name}",
    configurationPolicy = ConfigurationPolicy.REQUIRE)
@Reference(
        name = "imageTransformers",
        referenceInterface = ImageTransformer.class,
        policy = ReferencePolicy.DYNAMIC,
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE
)
public class NamedImageTransformerImpl implements NamedImageTransformer {
    private static final Logger log = LoggerFactory.getLogger(NamedImageTransformerImpl.class);

    private Map<String, ImageTransformer> imageTransformers = new HashMap<String, ImageTransformer>();
    
    /* Transformer Configuration Name */
    private static final String DEFAULT_TRANSFORM_NAME = "";

        private static final String PROP_NAME = "name";

    private String transformName = DEFAULT_TRANSFORM_NAME;

    /* Image Transform Configurations */
        private static final String PROP_TRANSFORMS = "transforms";

    private Map<String, ValueMap> transforms =
            Collections.synchronizedMap(new LinkedHashMap<String, ValueMap>());

    /**
     * @inheritDoc
     */
    public final Layer transform(Layer layer) {

        for (final Map.Entry<String, ValueMap> entry : this.transforms.entrySet()) {
            final ImageTransformer imageTransformer = this.imageTransformers.get(entry.getKey());
            if (imageTransformer == null) {
                log.warn("Skipping transform. Missing ImageTransformer for type: {}", entry.getKey());
                continue;
            }

            final ValueMap transformParams = entry.getValue();
            layer = imageTransformer.transform(layer, transformParams);
        }

        return layer;
    }

    /**
     * @inheritDoc
     */
    @Override
    public String getTransformName() {
        return this.transformName;
    }

    public final Map<String, ValueMap> getImageTransforms() {
        return this.transforms;
    }

    @Activate
    protected final void activate(final Map<String, String> properties) throws Exception {
        this.transformName = PropertiesUtil.toString(properties.get(PROP_NAME), DEFAULT_TRANSFORM_NAME);

        log.info("Registering Named Image Transformer: {}", this.transformName);

        final Map<String, String> map = ParameterUtil.toMap(PropertiesUtil.toStringArray(
                properties.get(PROP_TRANSFORMS), new String[]{}), ":", true, null);


        for (final Map.Entry<String, String> entry : map.entrySet()) {
            final String[] params = StringUtils.split(entry.getValue(), "&");
            final Map<String, String> values = ParameterUtil.toMap(params, "=", true, null);

            log.debug("ImageTransform params for [ {} ] ~> {}", entry.getKey(), values);

            // Order matters so use a LinkedHashMap
            this.transforms.put(entry.getKey(), TypeUtil.toValueMap(values));
        }

        log.info("Named Images Transforms: {}", this.transforms.size());
        for (final Map.Entry<String, ValueMap> entry : this.transforms.entrySet()) {
            log.info("{} ~> {}", entry.getKey(), entry.getValue());
        }
    }

    protected final void bindImageTransformers(final ImageTransformer service, final Map<Object, Object> props) {
        final String type = PropertiesUtil.toString(props.get(ImageTransformer.PROP_TYPE), null);
        if (type != null) {
            imageTransformers.put(type, service);
        }
    }

    protected final void unbindImageTransformers(final ImageTransformer service, final Map<Object, Object> props) {
        final String type = PropertiesUtil.toString(props.get(ImageTransformer.PROP_TYPE), null);
        if (type != null) {
            imageTransformers.remove(type);
        }
    }
}