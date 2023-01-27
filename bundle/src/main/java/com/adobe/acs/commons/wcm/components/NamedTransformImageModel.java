package com.adobe.acs.commons.wcm.components;

import com.day.cq.wcm.foundation.Image;
import org.osgi.annotation.versioning.ProviderType;

import java.util.List;

@ProviderType
public interface NamedTransformImageModel {
    String getLinkURL();

    Image getImage();

    boolean isReady();

}
