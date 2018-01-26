
package com.adobe.acs.commons.users.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Group extends AbstractAuthorizable {

    private static final Logger log = LoggerFactory.getLogger(Group.class);

    private static final String PATH_GROUPS = "/home/groups";

    private final List<String> memberOf;
    private final List<String> membership = new ArrayList<>();

    public Group(Map<String, Object> config) throws EnsureServiceUserException {
        super(config);

        String[] memberOfArr = PropertiesUtil.toStringArray(config.get(EnsureGroup.PROP_MEMBER_OF), new String[0]);
        memberOf = Arrays.asList(memberOfArr);
    }

    public List<String> getMemberOf() {
        return memberOf;
    }

    @Override
    public String getDefaultPath() {
        return PATH_GROUPS;
    }

    public void addMembership(String groupName) {
        membership.add(groupName);
    }

    public List<String> getMissingMemberOf() {
        return memberOf.stream().filter(group -> !membership.contains(group)).collect(Collectors.toList());
    }
}
