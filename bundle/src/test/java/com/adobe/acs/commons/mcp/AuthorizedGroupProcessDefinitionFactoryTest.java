/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.mcp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.jcr.RepositoryException;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.Group;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verify basic functionality of group authorization checks
 */
public class AuthorizedGroupProcessDefinitionFactoryTest {

    public AuthorizedGroupProcessDefinitionFactoryTest() {
    }

    String[] allowedGroups = new String[]{"administrators", "other-group"};
    AuthorizedGroupProcessDefinitionFactory instance;

    @Before
    public void setup() {
        instance = new AuthorizedGroupProcessDefinitionFactoryImpl(allowedGroups);
    }

    /**
     * Test if administrator recognized
     */
    @Test
    public void testAdmin() throws RepositoryException {
        User user = mockUser("administrators");
        assertEquals(true, instance.isAllowed(user));
    }

    /**
     * Test of other group allowed
     */
    @Test
    public void testAuthorizedGroup() throws RepositoryException {
        User user = mockUser("other-group");
        assertEquals(true, instance.isAllowed(user));
    }

    /**
     * Test of disallowed user
     */
    @Test
    public void testUnauthorized() throws RepositoryException {
        User user = mockUser("not-the-admin");
        assertEquals(false, instance.isAllowed(user));
    }
    
    /**
     * Test of user in many groups
     */
    @Test
    public void testAuthorized() throws RepositoryException {
        User user = mockUser("not-the-admin", "somthing-else", "another-group", "administrators");
        assertEquals(true, instance.isAllowed(user));
    }    

    private User mockUser(String... groupNames) throws RepositoryException {
        User user = mock(User.class);
        Iterator<Group> groups = mockGroups(groupNames);
        when(user.memberOf()).thenReturn(groups);
        return user;
    }
    
    private Iterator<Group> mockGroups(String... groupNames) {
        try {
            List<Group> groupList = new ArrayList<>();
            for (String groupName : groupNames) {
                Group group = mock(Group.class);
                when(group.getID()).thenReturn(groupName);
                groupList.add(group);
            }
            return groupList.iterator();
        } catch (RepositoryException ex) {
            // ignore it here, we are just mocking
            return null;
        }

    }

    private class AuthorizedGroupProcessDefinitionFactoryImpl extends AuthorizedGroupProcessDefinitionFactory<ProcessDefinition> {

        String[] groups;

        AuthorizedGroupProcessDefinitionFactoryImpl(String[] groups) {
            this.groups = groups;
        }

        public String[] getAuthorizedGroups() {
            return groups;
        }

        public ProcessDefinition createProcessDefinitionInstance() {
            return null;
        }

        public String getName() {
            return "Test factory";
        }
    }

}
