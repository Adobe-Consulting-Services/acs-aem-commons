/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2026 Adobe
 *
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
 */
package com.adobe.acs.commons.users.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class GroupTest {

    @Test
    public void testGroupMemberOf() throws EnsureAuthorizableException {

        Map<String, Object> config = new HashMap<String, Object>();
        config.put(EnsureGroup.PROP_MEMBER_OF, new String[]{"contributors", "user-administrators"});
        config.put(EnsureGroup.PROP_PRINCIPAL_NAME, "mycorp-authors");
        config.put(EnsureGroup.PROP_ACES, new String[]{});

        Group group = new Group(config);

        assertEquals("mycorp-authors", group.getPrincipalName());
        assertTrue(group.getMemberOf().contains("contributors"), "memberOf missing value contributors.");
        assertTrue(group.getMemberOf().contains("user-administrators"), "memberOf missing value user-administrators.");
    }

    @Test
    public void testGroupMemberOf_missing() throws EnsureAuthorizableException {

        Map<String, Object> config = new HashMap<String, Object>();
        config.put(EnsureGroup.PROP_MEMBER_OF, new String[]{"contributors", "user-administrators"});
        config.put(EnsureGroup.PROP_PRINCIPAL_NAME, "mycorp-authors");
        config.put(EnsureGroup.PROP_ACES, new String[]{});

        Group group = new Group(config);

        assertEquals("mycorp-authors", group.getPrincipalName());
        assertTrue(group.getMemberOf().contains("contributors"), "memberOf missing value contributors.");
        assertTrue(group.getMemberOf().contains("user-administrators"), "memberOf missing value user-administrators.");

        group.addMembership("contributors");

        List<String> missingMemberOf = group.getMissingMemberOf();
        assertEquals(1, missingMemberOf.size());
        assertEquals("user-administrators", missingMemberOf.get(0));
    }
}
