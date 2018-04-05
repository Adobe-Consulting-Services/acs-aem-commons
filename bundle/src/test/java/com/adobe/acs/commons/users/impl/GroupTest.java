
package com.adobe.acs.commons.users.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GroupTest {

    @Test
    public void testGroupMemberOf() throws EnsureAuthorizableException {

        Map<String, Object> config = new HashMap<String, Object>();
        config.put(EnsureGroup.PROP_MEMBER_OF, new String[] { "contributors", "user-administrators" });
        config.put(EnsureGroup.PROP_PRINCIPAL_NAME, "mycorp-authors");
        config.put(EnsureGroup.PROP_ACES, new String[] {});

        Group group = new Group(config);

        assertEquals("mycorp-authors", group.getPrincipalName());
        assertTrue("memberOf missing value contributors.", group.getMemberOf().contains("contributors"));
        assertTrue("memberOf missing value user-administrators.", group.getMemberOf().contains("user-administrators"));
    }

    @Test
    public void testGroupMemberOf_missing() throws EnsureAuthorizableException {

        Map<String, Object> config = new HashMap<String, Object>();
        config.put(EnsureGroup.PROP_MEMBER_OF, new String[] { "contributors", "user-administrators" });
        config.put(EnsureGroup.PROP_PRINCIPAL_NAME, "mycorp-authors");
        config.put(EnsureGroup.PROP_ACES, new String[] {});

        Group group = new Group(config);

        assertEquals("mycorp-authors", group.getPrincipalName());
        assertTrue("memberOf missing value contributors.", group.getMemberOf().contains("contributors"));
        assertTrue("memberOf missing value user-administrators.", group.getMemberOf().contains("user-administrators"));

        group.addMembership("contributors");

        List<String> missingMemberOf = group.getMissingMemberOf();
        assertEquals(1, missingMemberOf.size());
        assertEquals("user-administrators", missingMemberOf.get(0));
    }
}
