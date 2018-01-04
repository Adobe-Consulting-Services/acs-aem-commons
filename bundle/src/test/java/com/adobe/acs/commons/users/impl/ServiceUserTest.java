package com.adobe.acs.commons.users.impl;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class ServiceUserTest {

    @Test
    public void testServiceUser() throws EnsureServiceUserException {
        String[] aces = new String[1];
        aces[0] = "type=allow;privileges=jcr:read,rep:write;path=/content/dam;rep:glob=/jcr:content/*;rep:ntNames=cq:Page,dam:Asset;rep:itemNames=jcr:title,jcr:description;rep:prefixes=cq,dam";

        Map<String, Object> config = new HashMap<String, Object>();
        config.put(EnsureServiceUser.PROP_PRINCIPAL_NAME, "test-service-user");
        config.put(EnsureServiceUser.PROP_ACES, aces);

        ServiceUser serviceUser = new ServiceUser(config);

        Assert.assertEquals("test-service-user", serviceUser.getPrincipalName());
        Assert.assertEquals("/home/users/system", serviceUser.getIntermediatePath());

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
    public void testServiceUser_blankGlob() throws EnsureServiceUserException {
        String[] aces = new String[1];
        aces[0] = "type=allow;privileges=jcr:read;path=/content/dam;rep:glob=;";

        Map<String, Object> config = new HashMap<String, Object>();
        config.put(EnsureServiceUser.PROP_PRINCIPAL_NAME, "test-service-user");
        config.put(EnsureServiceUser.PROP_ACES, aces);

        ServiceUser serviceUser = new ServiceUser(config);

        Assert.assertEquals("test-service-user", serviceUser.getPrincipalName());
        Assert.assertEquals("/home/users/system", serviceUser.getIntermediatePath());

        for (Ace ace : serviceUser.getAces()) {
            assertFalse(ace.hasRepGlob());
            assertEquals(null, ace.getRepGlob());
        }
    }

    @Test
    public void testServiceUser_NoGlob() throws EnsureServiceUserException {
        String[] aces = new String[1];
        aces[0] = "type=allow;privileges=jcr:read;path=/content/dam;";

        Map<String, Object> config = new HashMap<String, Object>();
        config.put(EnsureServiceUser.PROP_PRINCIPAL_NAME, "test-service-user");
        config.put(EnsureServiceUser.PROP_ACES, aces);

        ServiceUser serviceUser = new ServiceUser(config);

        Assert.assertEquals("test-service-user", serviceUser.getPrincipalName());
        Assert.assertEquals("/home/users/system", serviceUser.getIntermediatePath());

        for (Ace ace : serviceUser.getAces()) {
            assertFalse(ace.hasRepGlob());
            assertEquals(null, ace.getRepGlob());
        }
    }

    @Test(expected=EnsureServiceUserException.class)
    public void testServiceUser_ProtectedSystemUser() throws EnsureServiceUserException {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(EnsureServiceUser.PROP_PRINCIPAL_NAME, "cryptoservice");

        new ServiceUser(config);
    }


    @Test
    public void testServiceUser_principalName() throws EnsureServiceUserException {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(EnsureServiceUser.PROP_PRINCIPAL_NAME, "test-service-user");

        ServiceUser serviceUser = new ServiceUser(config);

        Assert.assertEquals("test-service-user", serviceUser.getPrincipalName());
        Assert.assertEquals("/home/users/system", serviceUser.getIntermediatePath());
    }

    @Test
    public void testServiceUser_relativePrincipalName1() throws EnsureServiceUserException {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(EnsureServiceUser.PROP_PRINCIPAL_NAME, "folder/test-service-user");

        ServiceUser serviceUser = new ServiceUser(config);

        Assert.assertEquals("test-service-user", serviceUser.getPrincipalName());
        Assert.assertEquals("/home/users/system/folder", serviceUser.getIntermediatePath());
    }

    @Test
    public void testServiceUser_relativePrincipalName2() throws EnsureServiceUserException {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(EnsureServiceUser.PROP_PRINCIPAL_NAME, "./folder/test-service-user");

        ServiceUser serviceUser = new ServiceUser(config);

        Assert.assertEquals("test-service-user", serviceUser.getPrincipalName());
        Assert.assertEquals("/home/users/system/folder", serviceUser.getIntermediatePath());
    }

    @Test
    public void testServiceUser_relativePrincipalName3() throws EnsureServiceUserException {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(EnsureServiceUser.PROP_PRINCIPAL_NAME, "/folder/test-service-user");

        ServiceUser serviceUser = new ServiceUser(config);

        Assert.assertEquals("test-service-user", serviceUser.getPrincipalName());
        Assert.assertEquals("/home/users/system/folder", serviceUser.getIntermediatePath());
    }

    @Test
    public void testServiceUser_absoluteRealtivePrincipalName2() throws EnsureServiceUserException {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(EnsureServiceUser.PROP_PRINCIPAL_NAME, "/home/users/system/folder/test-service-user");

        ServiceUser serviceUser = new ServiceUser(config);

        Assert.assertEquals("test-service-user", serviceUser.getPrincipalName());
        Assert.assertEquals("/home/users/system/folder", serviceUser.getIntermediatePath());
    }

}