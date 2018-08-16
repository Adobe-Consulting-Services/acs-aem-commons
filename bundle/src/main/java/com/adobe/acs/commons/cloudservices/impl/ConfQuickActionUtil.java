package com.adobe.acs.commons.cloudservices.impl;

import com.day.cq.replication.Replicator;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import java.util.*;

public class ConfQuickActionUtil {
    private static final Logger log = LoggerFactory.getLogger(ConfQuickActionUtil.class);

    private ConfQuickActionUtil() {}

    public static Collection<String> getPropertiesQuickaction(Resource configResource) {
        return getQuickaction(configResource,
                new String[]{ Privilege.JCR_READ },
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


    public static Collection<String> getCreateQuickaction(Resource confResource) {
        return getQuickaction(confResource,
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
            return Collections.emptyList();
        }

        return Collections.emptyList();
    }

    private static boolean hasPrivileges(Resource resource, String... privilegeNames) throws RepositoryException {
        final AccessControlManager accessControlManager = resource.getResourceResolver().adaptTo(Session.class).getAccessControlManager();

        final List<Privilege> privileges = new ArrayList<>();

        for (final String privilegeName : privilegeNames) {
            final Privilege privilege = accessControlManager.privilegeFromName(privilegeName);
            if (privilege != null) {
                privileges.add(privilege);
            }
        }

        return accessControlManager.hasPrivileges(resource.getPath(), privileges.toArray(new Privilege[privileges.size()]));
    }
}
