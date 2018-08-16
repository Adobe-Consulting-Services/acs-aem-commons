package com.adobe.acs.commons.cloudservices.impl;

import com.day.cq.commons.jcr.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.resource.JcrResourceConstants;

import java.util.Arrays;
import java.util.regex.Pattern;

public class ConfUtil {
    private static final Pattern CONF_PATH_PATTERN = Pattern.compile("((^/(libs|apps|conf/global)/settings/cloudconfigs/?)|(^/conf/[^/]+/settings/cloudconfigs/?))(.|\\n)*");

    private static final String[] FOLDER_RESOURCE_TYPES = {
            JcrConstants.NT_FOLDER,
            JcrResourceConstants.NT_SLING_FOLDER,
            JcrResourceConstants.NT_SLING_ORDERED_FOLDER
    };

    private ConfUtil() {};

    public static String getTitle(Resource resource) {
        if (resource == null) {
            return null;
        }

        return resource.getValueMap().get(JcrConstants.JCR_CONTENT + "/" + JcrConstants.JCR_TITLE,
                resource.getValueMap().get(JcrConstants.JCR_TITLE, resource.getName()));
    }

    public static boolean isFolder(final Resource resource) {
        if (resource == null) {
            return false;
        }

        return Arrays.stream(FOLDER_RESOURCE_TYPES).anyMatch(rt -> resource.isResourceType(rt));
    }

    public static boolean isCloudConfiguration(final Resource resource) {
        return CONF_PATH_PATTERN.matcher(resource.getPath()).matches();
    }
}
