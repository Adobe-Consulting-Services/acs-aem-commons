/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class AbstractAuthorizableTest {

    @Test
    public void testServiceUser() throws EnsureAuthorizableException {
        String[] aces = new String[1];
        aces[0] = "type=allow;privileges=jcr:read,rep:write;path=/content/dam;rep:glob=/jcr:content/*;rep:ntNames=cq:Page,dam:Asset;rep:itemNames=jcr:title,jcr:description;rep:prefixes=cq,dam";

        Map<String, Object> config = new HashMap<String, Object>();
        config.put(EnsureServiceUser.PROP_PRINCIPAL_NAME, "test-service-user");
        config.put(EnsureServiceUser.PROP_ACES, aces);

        FakeAuthorizable serviceUser = new FakeAuthorizable(config);

        assertEquals("test-service-user", serviceUser.getPrincipalName());
        assertEquals("/home/fake", serviceUser.getIntermediatePath());

        for (Ace ace : serviceUser.getAces()) {
            assertEquals(true, ace.isAllow());
            assertEquals("jcr:read", ace.getPrivilegeNames().get(0));
            assertEquals("rep:write", ace.getPrivilegeNames().get(1));
            assertEquals("/content/dam", ace.getContentPath());

            assertTrue(ace.hasRepGlob());
            assertEquals("/jcr:content/*", ace.getRepGlob());

            assertTrue(ace.hasRepNtNames());
            assertEquals("cq:Page", ace.getRepNtNames().get(0));
            assertEquals("dam:Asset", ace.getRepNtNames().get(1));

            assertTrue(ace.hasRepItemNames());
            assertEquals("jcr:title", ace.getRepItemNames().get(0));
            assertEquals("jcr:description", ace.getRepItemNames().get(1));

            assertTrue(ace.hasRepPrefixes());
            assertEquals("cq", ace.getRepPrefixes().get(0));
            assertEquals("dam", ace.getRepPrefixes().get(1));
        }

        assertTrue(serviceUser.hasAceAt("/content/dam"));
    }


    @Test
    public void testServiceUser_blankGlob() throws EnsureAuthorizableException {
        String[] aces = new String[1];
        aces[0] = "type=allow;privileges=jcr:read;path=/content/dam;rep:glob=;";

        Map<String, Object> config = new HashMap<String, Object>();
        config.put(EnsureServiceUser.PROP_PRINCIPAL_NAME, "test-service-user");
        config.put(EnsureServiceUser.PROP_ACES, aces);

        FakeAuthorizable serviceUser = new FakeAuthorizable(config);

        assertEquals("test-service-user", serviceUser.getPrincipalName());
        assertEquals("/home/fake", serviceUser.getIntermediatePath());

        for (Ace ace : serviceUser.getAces()) {
            assertFalse(ace.hasRepGlob());
            assertEquals(null, ace.getRepGlob());
        }
    }

    @Test
    public void testServiceUser_NoGlob() throws EnsureAuthorizableException {
        String[] aces = new String[1];
        aces[0] = "type=allow;privileges=jcr:read;path=/content/dam;";

        Map<String, Object> config = new HashMap<String, Object>();
        config.put(EnsureServiceUser.PROP_PRINCIPAL_NAME, "test-service-user");
        config.put(EnsureServiceUser.PROP_ACES, aces);

        FakeAuthorizable serviceUser = new FakeAuthorizable(config);

        assertEquals("test-service-user", serviceUser.getPrincipalName());
        assertEquals("/home/fake", serviceUser.getIntermediatePath());

        for (Ace ace : serviceUser.getAces()) {
            assertFalse(ace.hasRepGlob());
            assertEquals(null, ace.getRepGlob());
        }
    }

    @Test
    public void testServiceUser_AcesWithSpaces_1552() throws EnsureAuthorizableException {
        String[] aces= new String[] {
                "\ntype=allow;privileges=jcr:versionManagement,jcr:read,crx:replicate,rep:write,jcr:lockManagement,jcr:modifyProperties;path=/content/dam",
                "\n  type=allow;privileges=jcr:versionManagement,jcr:read,crx:replicate,rep:write,jcr:lockManagement,jcr:modifyProperties;path=/content ",
                "\n  \ttype=allow;privileges=jcr:versionManagement,jcr:read,crx:replicate,rep:write,jcr:lockManagement,jcr:modifyProperties;path=/content/projects \t",
                "\n  \t\ttype=allow;privileges=jcr:all;path=/var/workflow \t \n",
                "\n\n\n\n\ntype=allow;privileges=jcr:all;path=/etc/workflow\n    "
        };

        Map<String, Object> config = new HashMap<String, Object>();
        config.put(EnsureServiceUser.PROP_PRINCIPAL_NAME, "test-service-user");
        config.put(EnsureServiceUser.PROP_ACES, aces);

        FakeAuthorizable serviceUser = new FakeAuthorizable(config);

        assertEquals("test-service-user", serviceUser.getPrincipalName());
        assertEquals("/home/fake", serviceUser.getIntermediatePath());
        assertEquals(5, serviceUser.getAces().size());

        for (Ace ace : serviceUser.getAces()) {
            assertTrue(ace.isAllow());
            assertFalse(ace.getContentPath().contains("\n"));
            assertFalse(ace.getContentPath().contains("\t"));
            assertFalse(ace.getContentPath().contains(" "));
        }
    }

    @Test(expected=EnsureAuthorizableException.class)
    public void testServiceUser_ProtectedSystemUser() throws EnsureAuthorizableException {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(EnsureServiceUser.PROP_PRINCIPAL_NAME, "cryptoservice");

        new ServiceUser(config);
    }


    @Test
    public void testServiceUser_principalName() throws EnsureAuthorizableException {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(EnsureServiceUser.PROP_PRINCIPAL_NAME, "test-service-user");

        FakeAuthorizable serviceUser = new FakeAuthorizable(config);

        assertEquals("test-service-user", serviceUser.getPrincipalName());
        assertEquals("/home/fake", serviceUser.getIntermediatePath());
    }

    @Test
    public void testServiceUser_relativePrincipalName1() throws EnsureAuthorizableException {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(EnsureServiceUser.PROP_PRINCIPAL_NAME, "folder/test-service-user");

        FakeAuthorizable serviceUser = new FakeAuthorizable(config);

        assertEquals("test-service-user", serviceUser.getPrincipalName());
        assertEquals("/home/fake/folder", serviceUser.getIntermediatePath());
    }

    @Test
    public void testServiceUser_relativePrincipalName2() throws EnsureAuthorizableException {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(EnsureServiceUser.PROP_PRINCIPAL_NAME, "./folder/test-service-user");

        FakeAuthorizable serviceUser = new FakeAuthorizable(config);

        assertEquals("test-service-user", serviceUser.getPrincipalName());
        assertEquals("/home/fake/folder", serviceUser.getIntermediatePath());
    }

    @Test
    public void testServiceUser_relativePrincipalName3() throws EnsureAuthorizableException {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(EnsureServiceUser.PROP_PRINCIPAL_NAME, "/folder/test-service-user");

        FakeAuthorizable serviceUser = new FakeAuthorizable(config);

        assertEquals("test-service-user", serviceUser.getPrincipalName());
        assertEquals("/home/fake/folder", serviceUser.getIntermediatePath());
    }

    @Test
    public void testServiceUser_absoluteRealtivePrincipalName2() throws EnsureAuthorizableException {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(EnsureServiceUser.PROP_PRINCIPAL_NAME, "/home/fake/folder/test-service-user");

        FakeAuthorizable serviceUser = new FakeAuthorizable(config);

        assertEquals("test-service-user", serviceUser.getPrincipalName());
        assertEquals("/home/fake/folder", serviceUser.getIntermediatePath());
    }

    private class FakeAuthorizable extends AbstractAuthorizable {

        public FakeAuthorizable(Map<String, Object> config) throws EnsureAuthorizableException {
            super(config);
        }

        @Override
        public String getDefaultPath() {
            return "/home/fake";
        }
    }

}