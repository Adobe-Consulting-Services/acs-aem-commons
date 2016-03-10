package com.adobe.acs.commons.images.impl;

import java.io.IOException;

import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.Resource;

import com.day.cq.commons.ImageHelper;
import com.day.cq.wcm.foundation.Image;
import com.day.image.Layer;

public class SocialRemoteImageImpl extends Image {

	private final Resource res;
	
	public SocialRemoteImageImpl(Resource arg0, String name) {
		super(arg0, name);
		res = arg0;
	}

    public Layer getLayer(boolean cropped, boolean resized, boolean rotated)
            throws IOException, RepositoryException {
        Layer layer = null;
        layer = ImageHelper.createLayer(res);
        if (layer != null) {
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
