package com.adobe.acs.commons.cloudservices;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.replication.Replicator;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import java.util.*;
import java.util.regex.Pattern;

public final class CloudServiceUtil {
    private static final Logger log = LoggerFactory.getLogger(CloudServiceUtil.class);
    private static final Pattern CONF_PATH_PATTERN = Pattern.compile("((^/(libs|apps|conf/global)/settings/cloudconfigs/?)|(^/conf/[^/]+/settings/cloudconfigs/?))(.|\\n)*");

    private static final String[] FOLDER_RESOURCE_TYPES = {
            JcrConstants.NT_FOLDER,
            JcrResourceConstants.NT_SLING_FOLDER,
            JcrResourceConstants.NT_SLING_ORDERED_FOLDER
    };

    private CloudServiceUtil() { }

    public static String getTitle(final Resource configResource) {
        if (configResource == null) {
            return null;
        }

        return configResource.getValueMap().get(JcrConstants.JCR_CONTENT + "/" + JcrConstants.JCR_TITLE,
                configResource.getValueMap().get(JcrConstants.JCR_TITLE, configResource.getName()));
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



    public static boolean isConfigurationContainer(Resource resource) {
        return (resource != null && resource.isResourceType("sling:Folder")
                && !"cloudconfigs".equals(resource.getName()));
    }

    public static boolean hasSetting(Resource resource, String settingPath) {
        return (resource != null && resource.getChild(settingPath) != null);
    }

    public static Collection<String> getPropertiesQuickaction(Resource configResource) {
        return getQuickaction(configResource,
                new String[]{ Privilege.JCR_MODIFY_PROPERTIES },
                new String[] { "cq-confadmin-actions-properties-activator" });
    }

    public static Collection<String> getReplicationQuickactions(Resource configResource) {
        return getQuickaction(configResource,
                new String[]{ Replicator.REPLICATE_PRIVILEGE },
                new String[] {
                    "cq-confadmin-actions-publish-activator",
                    "cq-confadmin-actions-unpublish-activator"
                });
    }

    public static Collection<String> getDeleteQuickaction(Resource configResource) {
        return getQuickaction(configResource,
                new String[]{ Privilege.JCR_REMOVE_NODE },
                new String[] { "cq-confadmin-actions-delete-activator" });
    }

    public static Collection<String> getCreateQuickaction(Resource configResource) {
        return getQuickaction(configResource,
                new String[]{ Privilege.JCR_WRITE },
                new String[] { "cq-confadmin-actions-createconfig-activator" });
    }

    private static Collection<String> getQuickaction(Resource configResource, String[] privilegeNames, String[] quickActions) {
        try {
            if (hasPrivileges(configResource, privilegeNames)) {
                return Arrays.asList(quickActions);
            }
        } catch (RepositoryException e) {
            log.error("Could not determine if the user [ {} ] has [ {} ] permissions on [ {} ]", new String[]{
                    configResource.getResourceResolver().getUserID(),
                    StringUtils.join(privilegeNames, "," ),
                    configResource.getPath()});
            return Collections.EMPTY_LIST;
        }

        return Collections.EMPTY_LIST;
    }

    private static boolean hasPrivileges(Resource resource, String... privilegeNames) throws RepositoryException {
        final AccessControlManager accessControlManager = resource.getResourceResolver().adaptTo(Session.class).getAccessControlManager();

        final List<Privilege> privileges = new ArrayList<>();

        for (String privilegeName : privilegeNames) {
            final Privilege privilege = accessControlManager.privilegeFromName(privilegeName);
            if (privilege != null) {
                privileges.add(privilege);
            }
        }

        return accessControlManager.hasPrivileges(resource.getPath(), privileges.toArray(new Privilege[privileges.size()]));
    }
}