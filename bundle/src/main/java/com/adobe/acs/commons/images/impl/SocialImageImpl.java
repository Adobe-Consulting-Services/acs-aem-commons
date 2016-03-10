package com.adobe.acs.commons.images.impl;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;

import com.day.cq.commons.ImageHelper;
import com.day.cq.wcm.foundation.Image;
import com.day.image.Layer;

public class SocialImageImpl extends Image {

	private final Resource res;
	
	public SocialImageImpl(Resource arg0, String name) {
		super(arg0, name);
		res = arg0;
	}

    public Layer getLayer(boolean cropped, boolean resized, boolean rotated)
            throws IOException, RepositoryException {
        Layer layer = null;
        Session session = res.getResourceResolver().adaptTo(Session.class);
        Resource contentRes = res.getChild("jcr:content");
        Node node = session.getNode(contentRes.getPath());
        Property data = node.getProperty("jcr:data");
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
