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
    public void testServiceUser() {
        String[] aces = new String[1];
        aces[0] = "type=allow;privileges=jcr:read,rep:write;path=/content/dam;rep:glob=/jcr:content/*";

        Map<String, Object> config = new HashMap<String, Object>();
        config.put(EnsureServiceUser.PROP_PRINCIPAL_NAME, "test-service-user");
        config.put(EnsureServiceUser.PROP_ACES, aces);

        EnsureServiceUser.ServiceUser serviceUser = new EnsureServiceUser.ServiceUser(config);

        Assert.assertEquals("test-service-user", serviceUser.getPrincipalName());
        Assert.assertEquals("/home/users/system", serviceUser.getIntermediatePath());

        for (EnsureServiceUser.Ace ace : serviceUser.getAces()) {
            assertEquals(true, ace.isAllow());
            assertEquals("jcr:read", ace.getPrivilegeNames().get(0));
            assertEquals("rep:write", ace.getPrivilegeNames().get(1));
            assertEquals("/content/dam", ace.getPath());
            assertEquals("/jcr:content/*", ace.getRepGlob());
        }

        assertTrue(serviceUser.hasAceAt("/content/dam"));
    }
}