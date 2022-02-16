/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package com.adobe.acs.commons.users.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Group extends AbstractAuthorizable {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(Group.class);

    private static final String PATH_GROUPS = "/home/groups";

    private final List<String> memberOf;
    private final List<String> membership = new ArrayList<>();

    public Group(Map<String, Object> config) throws EnsureAuthorizableException {
        super(config);

        String[] memberOfArr = PropertiesUtil.toStringArray(config.get(EnsureGroup.PROP_MEMBER_OF), new String[0]);
        memberOf = Arrays.asList(memberOfArr);
    }

    public List<String> getMemberOf() {
        return Collections.unmodifiableList(memberOf);
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
