package com.adobe.acs.commons.users.impl;


import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.oak.commons.PathUtils;
import org.apache.jackrabbit.vault.util.PathUtil;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ServiceUser {
    private static final Logger log = LoggerFactory.getLogger(ServiceUser.class);

    private final String PATH_SYSTEM_USERS = "/home/users/system";
    private final String principalName;
    private final String intermediatePath;
    private final List<Ace> aces = new ArrayList<Ace>();

    public ServiceUser(Map<String, Object> config) throws EnsureServiceUserException {
        String tmp = PropertiesUtil.toString(config.get(EnsureServiceUser.PROP_PRINCIPAL_NAME), null);

        if (StringUtils.contains(tmp, "/")) {
            tmp = StringUtils.removeStart(tmp, PATH_SYSTEM_USERS);
            tmp = StringUtils.removeStart(tmp, "/");
            this.principalName = PathUtils.getName(tmp);
            this.intermediatePath = PathUtil.makePath(PATH_SYSTEM_USERS, StringUtils.removeEnd(tmp, this.principalName));
        } else {
            this.principalName = tmp;
            this.intermediatePath = "/home/users/system";
        }

        // Check the principal name for validity
        if (StringUtils.isBlank(this.principalName)) {
            throw new EnsureServiceUserException("No Principal Name provided to Ensure Service User");
        } else if (ProtectedSystemUsers.isProtected(this.principalName)) {
            throw new EnsureServiceUserException(String.format("[ %s ] is an System User provided by AEM or ACS AEM Commons. You cannot ensure this user.", this.principalName));
        }

        for (String entry : PropertiesUtil.toStringArray(config.get(EnsureServiceUser.PROP_ACES), new String[0])) {
            try {
                getAces().add(new Ace(entry));
            } catch (EnsureServiceUserException e) {
                log.warn("Malformed ACE config [ " + entry + " ] for Service User [ " + StringUtils.defaultIfEmpty(this.principalName, "NOT PROVIDED") + " ]", e);
            }
        }
    }

    public boolean hasAceAt(String path) {
        for (Ace ace : getAces()) {
            if (StringUtils.equals(path, ace.getContentPath())) {
                return true;
            }
        }

        return false;
    }

    public String getIntermediatePath() {
        return intermediatePath;
    }

    public String getPrincipalName() {
        return principalName;
    }

    public List<Ace> getAces() {
        return aces;
    }

    public Ace getAce(JackrabbitAccessControlEntry actual) throws RepositoryException {
        for (Ace ace : getAces()) {
            if (ace.isSameAs(actual)) {
                return ace;
            }
        }

        return null;
    }

    public List<Ace> getMissingAces() {
        final List<Ace> result = new ArrayList<Ace>();
        for (final Ace ace : getAces()) {
            if (!ace.isExists()) {
                result.add(ace);
            }
        }

        return result;
    }
}