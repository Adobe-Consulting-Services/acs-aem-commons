package com.adobe.acs.commons.wcm.pwa.impl;

import com.day.cq.commons.jcr.JcrConstants;
import org.apache.sling.api.resource.Resource;

/**
 * Provides helper methods for resources.
 */
public final class ResourceHelper {

    private ResourceHelper() {

    }

    /**
     * Returns {@code true} if specified {@code resource} matches one of the
     * specified {@code resourceTypes}.
     *
     * @param resource Resource to verify
     * @param resourceTypes Resource types
     * @return {@code true} if the resource matches a resource type,
     *         {@code false} otherwise
     */
    public static boolean isResourceType(Resource resource, String... resourceTypes) {
        if (resource != null && resourceTypes != null) {
            for (String resourceType : resourceTypes) {
                Resource child = resource.getChild(JcrConstants.JCR_CONTENT);
                if (child != null) {
                    resource = child;
                }
                if (resource.isResourceType(resourceType)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if specified {@code resource} is a configuration
     * container.
     *
     * @param resource Resource to verify
     * @return {@code true} if the resource represents a configuration
     *         container, {@code false} otherwise
     */
    public static boolean isConfigurationContainer(Resource resource) {
        return (resource != null && resource.isResourceType("sling:Folder")
                && !"settings".equals(resource.getName()));
    }

    /**
     * Returns {@code true} if specified {@code resource} has the configuration
     * setting {@code settingPath}.
     *
     * @param resource Resource to verify
     * @param settingPath Path of the setting to check for existence
     * @return {@code true} if the resource has the specified setting,
     *         {@code false} otherwise
     */
    public static boolean hasSetting(Resource resource, String settingPath) {
        return (resource != null && resource.getChild(settingPath) != null);
    }


    public static boolean isCloudConfiguration(Resource resource) {
        if (resource != null) {
            Resource parent = resource;

            do {
                if ("cloudconfigs".equals(parent.getName())) {
                    return true;
                }
                parent = parent.getParent();
            } while (parent != null);
        }
        return false;
    }

}