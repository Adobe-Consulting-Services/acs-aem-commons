/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.images.impl;

import com.day.cq.commons.ImageHelper;
import com.day.cq.wcm.foundation.Image;
import com.day.image.Layer;
import org.apache.sling.api.resource.Resource;

import javax.jcr.RepositoryException;
import java.io.IOException;

public class SocialRemoteImageImpl extends Image {
    private final Resource resource;

    public SocialRemoteImageImpl(Resource resource, String name) {
        super(resource, name);
        this.resource = resource;
    }

    public Layer getLayer(boolean cropped, boolean resized, boolean rotated)
            throws IOException, RepositoryException {
        final Layer layer = ImageHelper.createLayer(resource);

        if (layer != null) {
            if (cropped) {
                crop(layer);
            }

            if (resized) {
                resize(layer);
            }

            if (rotated) {
                rotate(layer);
            }
        }
        return layer;
    }
}
