package com.adobe.acs.commons.images;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

public interface NamedImageTransformUrlService {

    String[] getTransformNames(SlingHttpServletRequest request);

    String getRequestUrl(Resource imageResource, String[] transformNames, long timestamp);

    String getRequestUrl(Resource imageResource, String transformName, long timestamp);

}
