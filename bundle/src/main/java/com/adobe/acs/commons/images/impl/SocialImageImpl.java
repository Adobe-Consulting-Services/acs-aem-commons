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

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;

import com.day.cq.commons.ImageHelper;
import com.day.cq.wcm.foundation.Image;
import com.day.image.Layer;

public class SocialImageImpl extends Image {

    private final Resource res;

    public SocialImageImpl(Resource resource, String name) {
        super(resource, name);
        res = resource;
    }

    public Layer getLayer(boolean cropped, boolean resized, boolean rotated)
            throws IOException, RepositoryException {
        Layer layer = null;
        Session session = res.getResourceResolver().adaptTo(Session.class);
        Resource contentRes = res.getChild(JcrConstants.JCR_CONTENT);
        Node node = session.getNode(contentRes.getPath());
        Property data = node.getProperty(JcrConstants.JCR_DATA);
        if (data != null) {
            layer = ImageHelper.createLayer(data);
            if (layer != null && cropped) {
                crop(layer);
            }
            if (layer != null && resized) {
                resize(layer);
            }
            if (layer != null && rotated) {
                rotate(layer);
            }
        }
        return layer;
    }

}
