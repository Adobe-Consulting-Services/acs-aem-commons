package com.adobe.acs.commons.users.impl;

import com.adobe.acs.commons.util.ParameterUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ACE OSGi Config Format
 *
 * type=allow;privileges=jcr:read,rep:write;path=/content/foo;rep:glob=/jcr:content/*
 *
 * REQUIRED
 * - type = allow | deny
 * - privileges = privilegeNames comma separated
 * - path = absolute path (must exist in JCR)
 *
 * OPTIONAL
 * - rep:glob = rep glob expression
 * - rep:ntNames = ntNames expression
 */
public final class Ace {
    private static final Logger log = LoggerFactory.getLogger(Ace.class);

    private static final String PARAM_DELIMITER = ";";
    private static final String KEY_VALUE_SEPARATOR = "=";
    private static final String LIST_SEPARATOR = ",";

    public static final String TYPE = "type";
    public static final String PATH = "path";
    public static final String PRIVILEGES = "privileges";
    public static final String REP_GLOB = AccessControlConstants.REP_GLOB;
    public static final String REP_NT_NAMES = AccessControlConstants.REP_NT_NAMES;

    private String type;
    private String path;
    private String repGlob;
    private String repNtNames;
    private final List<String> privilegeNames = new ArrayList<String>();
    private boolean exists = false;

    public Ace(String raw) throws EnsureServiceUserException {
        String[] segments = StringUtils.split(raw, PARAM_DELIMITER);

        for (String segment : segments) {
            AbstractMap.SimpleEntry<String, String> entry = ParameterUtil.toSimpleEntry(segment, KEY_VALUE_SEPARATOR);

            if (StringUtils.equals(TYPE, entry.getKey())) {
                this.type = StringUtils.stripToNull(entry.getValue());
            } else if (StringUtils.equals(PATH, entry.getKey())) {
                this.path = StringUtils.stripToNull(entry.getValue());
            } else if (StringUtils.equals(REP_GLOB, entry.getKey())) {
                this.repGlob = StringUtils.stripToNull(entry.getValue());
            } else if (StringUtils.equals(REP_NT_NAMES, entry.getKey())) {
                this.repNtNames = StringUtils.stripToNull(entry.getValue());
            } else if (StringUtils.equals(PRIVILEGES, entry.getKey())) {
                for (String privilege : StringUtils.split(entry.getValue(), LIST_SEPARATOR)) {
                    privilege = StringUtils.stripToNull(privilege);
                    if (privilege != null) {
                        this.privilegeNames.add(privilege);
                    }
                }
            }
        }

        validate(this.type, this.path, this.privilegeNames);
    }

    protected void validate(String type, String path, List<String> privilegeNames) throws EnsureServiceUserException {
        if (!ArrayUtils.contains(new String[] { "allow", "deny"}, type)) {
            throw new EnsureServiceUserException("Ensure Service User requires valid type. [ " + type + " ] type is invalid");
        } else if (!StringUtils.startsWith(path , "/")) {
            throw new EnsureServiceUserException("Ensure Service User requires an absolute path. [ " + path + " ] path is invalid");
        } else if (privilegeNames.size() < 1) {
            throw new EnsureServiceUserException("Ensure Service User requires at least 1 privilege to apply.");
        }
    }

    public boolean isAllow() {
        return StringUtils.equals("allow", this.type);
    }

    public String getContentPath() {
        return path;
    }

    public String getRepGlob() {
        return repGlob;
    }

    public List<String> getPrivilegeNames() {
        return privilegeNames;
    }

    public List<Privilege> getPrivileges(AccessControlManager accessControlManager) {
        final List<Privilege> privileges = new ArrayList<Privilege>();

        for (String privilegeName : getPrivilegeNames()) {
            try {
                privileges.add(accessControlManager.privilegeFromName(privilegeName));
            } catch (RepositoryException e) {
                log.error("Unable to convert provided privilege name [ {} ] to a JCR Privilege. Skipping...", privilegeName);
            }
        }

        return privileges;
    }


    public void setExists(boolean exists) {
        this.exists = exists;
    }

    public boolean isExists() {
        return exists;
    }

    public boolean hasRepGlob() {
        return StringUtils.isNotBlank(getRepGlob());
    }

    public boolean hasRepNtNames() {
        return StringUtils.isNotBlank(getRepNtNames());
    }

    public String getRepNtNames() {
        return repNtNames;
    }

    public boolean isSameAs(JackrabbitAccessControlEntry actual) throws RepositoryException {
        // Allow vs Deny
        if (actual.isAllow() != this.isAllow()) {
            return false;
        }

        // Privileges
        final List<String> actualPrivileges = Arrays.asList(AccessControlUtils.namesFromPrivileges(actual.getPrivileges()));
        if (!CollectionUtils.isEqualCollection(actualPrivileges, getPrivilegeNames())) {
            return false;
        }

        // rep:glob
        final Value actualRepGlob = actual.getRestriction(AccessControlConstants.REP_GLOB);
        if (this.hasRepGlob() && actualRepGlob == null) {
            // configuration has rep:glob, but the actual does not
            return false;
        } else if (!this.hasRepGlob() && actualRepGlob != null) {
            // configuration does NOT have rep:glob, but actual does
            return false;
        } else if (this.hasRepGlob() && actualRepGlob != null) {
            // configuration has rep:glob and actual does too
            if (!StringUtils.equals(actualRepGlob.toString(), this.getRepGlob())) {
                return false;
            }
        } else {
            // neither has rep:glob so they match

        }

        // rep:ntNames
        final Value actualRepNtNames =  actual.getRestriction(AccessControlConstants.REP_NT_NAMES);
        if (this.hasRepNtNames() && actualRepNtNames == null) {
            // configuration has rep:glob, but the actual does not
            return false;
        } else if (!this.hasRepNtNames() && actualRepNtNames != null) {
            // configuration does NOT have rep:glob, but actual does
            return false;
        } else if (this.hasRepNtNames() && actualRepNtNames != null) {
            // configuration has rep:ntNames and actual does too
            if (!StringUtils.equals(actualRepNtNames.toString(), this.getRepNtNames())) {
                return false;
            }
        } else {
            // neither has rep:ntNames so they match
        }

        return true;
    }
}