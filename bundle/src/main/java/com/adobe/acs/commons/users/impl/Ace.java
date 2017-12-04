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

    private static final String PROP_TYPE = "type";
    private static final String PROP_PATH = "path";
    private static final String PROP_PRIVILEGES = "privileges";
    private static final String PROP_REP_GLOB = AccessControlConstants.REP_GLOB;
    private static final String PROP_REP_NT_NAMES = AccessControlConstants.REP_NT_NAMES;
    private static final String PROP_REP_ITEM_NAMES = AccessControlConstants.REP_ITEM_NAMES;
    private static final String PROP_REP_PREFIXES = AccessControlConstants.REP_PREFIXES;


    private String type;
    private String path;
    private String repGlob = null;
    private List<String> repNtNames = new ArrayList<String>();
    private List<String> repItemNames = new ArrayList<String>();
    private List<String> repPrefixes = new ArrayList<String>();
    private final List<String> privilegeNames = new ArrayList<String>();
    private boolean exists = false;

    @SuppressWarnings("squid:S3776")
    public Ace(String raw) throws EnsureServiceUserException {
        String[] segments = StringUtils.split(raw, PARAM_DELIMITER);

        for (String segment : segments) {
            AbstractMap.SimpleEntry<String, String> entry = ParameterUtil.toSimpleEntry(segment, KEY_VALUE_SEPARATOR);

            if (entry == null) {
                continue;
            }
            if (StringUtils.equals(PROP_TYPE, entry.getKey())) {
                this.type = StringUtils.stripToNull(entry.getValue());
            } else if (StringUtils.equals(PROP_PATH, entry.getKey())) {
                this.path = StringUtils.stripToNull(entry.getValue());
            } else if (StringUtils.equals(PROP_REP_GLOB, entry.getKey())) {
                this.repGlob = StringUtils.stripToEmpty(entry.getValue());
            } else if (StringUtils.equals(PROP_REP_NT_NAMES, entry.getKey())) {
                this.repNtNames.addAll(Arrays.asList(StringUtils.split(StringUtils.stripToEmpty(entry.getValue()), LIST_SEPARATOR)));
            } else if (StringUtils.equals(PROP_REP_ITEM_NAMES, entry.getKey())) {
                this.repItemNames.addAll(Arrays.asList(StringUtils.split(StringUtils.stripToEmpty(entry.getValue()), LIST_SEPARATOR)));
            } else if (StringUtils.equals(PROP_REP_PREFIXES, entry.getKey())) {
                this.repPrefixes.addAll(Arrays.asList(StringUtils.split(StringUtils.stripToEmpty(entry.getValue()), LIST_SEPARATOR)));
            } else if (StringUtils.equals(PROP_PRIVILEGES, entry.getKey())) {
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

    /** rep:glob **/

    public String getRepGlob() {
        return repGlob;
    }

    public boolean hasRepGlob() {
        return getRepGlob() != null;
    }

    /** rep:ntNames **/

    public List<String> getRepNtNames() {
        return repNtNames;
    }

    public boolean hasRepNtNames() {
        return !getRepNtNames().isEmpty();
    }


    /** rep:itemNames **/

    public List<String> getRepItemNames() {
        return repItemNames;
    }

    public boolean hasRepItemNames() {
        return !getRepItemNames().isEmpty();
    }

    /** rep:prefixes **/

    public List<String> getRepPrefixes() {
        return repPrefixes;
    }

    public boolean hasRepPrefixes() {
        return !getRepPrefixes().isEmpty();
    }

    /**
     * Determines if the configured ACE is the same as the actual ACE in the JCR.
     * @param actual the actual ACE in the JCR
     * @return true if both ACEs are logically the same, false if not.
     * @throws RepositoryException
     */
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

        // We are converting the single value RepGlob into a List for convenience
        if(!isRestrictionValid(this.hasRepGlob(), actual.getRestrictions(AccessControlConstants.REP_GLOB), Arrays.asList(new String[]{this.getRepGlob()}))) {
            return false;
        }

        // rep:ntNames
        if(!isRestrictionValid(this.hasRepNtNames(), actual.getRestrictions(AccessControlConstants.REP_NT_NAMES), this.getRepNtNames())) {
            return false;
        }

        // rep:itemNames
        if(!isRestrictionValid(this.hasRepItemNames(), actual.getRestrictions(AccessControlConstants.REP_ITEM_NAMES), this.getRepItemNames())) {
            return false;
        }

        // rep:prefixes
        if(!isRestrictionValid(this.hasRepPrefixes(), actual.getRestrictions(AccessControlConstants.REP_PREFIXES), this.getRepPrefixes())) {
            return false;
        }

        return true;
    }

    @SuppressWarnings("squid:S2589")
    private boolean isRestrictionValid(boolean configExists, Value[] actualValues, List<String> configValues) {
        final ArrayList<String> actualRestrictions = new ArrayList<String>();

        if (configExists && actualValues == null) {
            // configuration has rep:glob, but the actual does not
            return false;
        } else if (!configExists && actualValues != null) {
            // configuration does NOT have rep:XXX, but actual does
            return false;
        } else if (configExists && actualValues != null) {
            // configuration has rep:XXX and actual does too
            for (final Value value : actualValues) {
                actualRestrictions.add(value.toString());
            }

            if (!CollectionUtils.isEqualCollection(actualRestrictions, configValues)) {
                return false;
            }
        } else {
            // neither has this rep:XXX so they match
        }

        return true;
    }
}