package com.adobe.acs.commons.images.impl;

import com.adobe.acs.commons.images.NamedImageTransformUrlService;
import com.adobe.acs.commons.util.PathInfoUtil;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        label = "ACS AEM Commons - Named Image Transformer URL Service",
        description = "Service to parse and generate URLs for named image transforms.",
        configurationFactory = true,
        metatype = true
)
@Service
public class NamedImageTransformUrlServiceImpl implements NamedImageTransformUrlService {
    private static final Logger log = LoggerFactory.getLogger(NamedImageTransformUrlServiceImpl.class);

    @Override
    public String[] getTransformNames(SlingHttpServletRequest request) {
        String[] suffixes = PathInfoUtil.getSuffixSegments(request);
        if (suffixes.length < 2) {
            log.warn("Named Transform Image Servlet requires at least one named transform");
            return new String[]{}; // empty array
        }

        int endIndex = suffixes.length - 1;
        // Its OK to check; the above check ensures there are 2+ segments
        if (StringUtils.isNumeric(PathInfoUtil.getSuffixSegment(request, suffixes.length - 2))) {
            endIndex--;
        }

        return (String[]) ArrayUtils.subarray(suffixes, 0, endIndex);
    }

    @Override
    public String getRequestUrl(Resource imageResource, String[] transformNames, long timestamp) {
        StringBuilder sb = new StringBuilder(imageResource.getPath()).append(".transform/");

        for (String transformName : transformNames) {
            sb.append(transformName).append("/");
        }
        sb.append(timestamp).append("/image.png");

        return sb.toString();
    }

    @Override
    public String getRequestUrl(Resource imageResource, String transformName, long timestamp) {
        return getRequestUrl(imageResource, new String[] {transformName}, timestamp);
    }

}
