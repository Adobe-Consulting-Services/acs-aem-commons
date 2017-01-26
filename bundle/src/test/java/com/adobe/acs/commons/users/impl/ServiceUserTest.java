package com.adobe.acs.commons.users.impl;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class ServiceUserTest {

    @Test
    public void testServiceUser() throws EnsureServiceUserException {
        String[] aces = new String[1];
        aces[0] = "type=allow;privileges=jcr:read,rep:write;path=/content/dam;rep:glob=/jcr:content/*";

        Map<String, Object> config = new HashMap<String, Object>();
        config.put(EnsureServiceUser.PROP_ENSURE_IMMEDIATELY, "test-service-user");
        config.put(EnsureServiceUser.PROP_ACES, aces);

        ServiceUser serviceUser = new ServiceUser(config);

        Assert.assertEquals("test-service-user", serviceUser.getPrincipalName());
        Assert.assertEquals("/home/users/system", serviceUser.getIntermediatePath());

        for (Ace ace : serviceUser.getAces()) {
            assertEquals(true, ace.isAllow());
            assertEquals("jcr:read", ace.getPrivilegeNames().get(0));
            assertEquals("rep:write", ace.getPrivilegeNames().get(1));
            assertEquals("/content/dam", ace.getContentPath());
            assertEquals("/jcr:content/*", ace.getRepGlob());
        }

        assertTrue(serviceUser.hasAceAt("/content/dam"));
    }
}