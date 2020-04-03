/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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
package com.adobe.acs.commons.email.process.impl;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.testing.jcr.MockValue;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.wcm.api.Page;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SendTemplatedEmailUtilsTest {

    private SimpleDateFormat sdf;

    @Mock
    private ValueMap vmap;

    private static final String PN_CALENDAR = "cq:lastModified";
    private static final String CALENDAR_TOSTRING = "2014-09-12 01:30";

    private static final String PN_LONG = "long";
    private static final String LONG_TOSTRING = "123456";

    private static final String PN_STR_ARRAY = "stringArray";
    private static final String STR_ARRAY_TOSTRING = "apple, banana, carrot";

    private static final String PN_TITLE = "jcr:Title";
    private static final String STR_TOSTRING = "My Title";

    private static final String PN_EMAIL = "profile/email";

    @Test
    public void testGetPayloadProperties_NullResource() throws Exception {

        Resource payloadRes = mock(Resource.class);
        Map<String, String> props = SendTemplatedEmailUtils.getPayloadProperties(payloadRes, sdf);
        assertEquals(0, props.size());
    }

    @Test
    public void testGetPayloadProperties_Asset() throws Exception {

        // set up jcr properties
        mockJcrProperties();

        Resource payloadRes = mock(Resource.class);
        Resource mdRes = mock(Resource.class);
        when(payloadRes.getResourceType()).thenReturn("dam:Asset");

        when(payloadRes.getChild(JcrConstants.JCR_CONTENT + "/" + DamConstants.METADATA_FOLDER)).thenReturn(mdRes);

        // mock valueMap
        when(mdRes.getValueMap()).thenReturn(vmap);
        Map<String, String> props = SendTemplatedEmailUtils.getPayloadProperties(payloadRes, sdf);

        assertEquals(props.get(PN_CALENDAR), CALENDAR_TOSTRING);
        assertEquals(props.get(PN_TITLE), STR_TOSTRING);
        assertEquals(props.get(PN_LONG), LONG_TOSTRING);
        assertEquals(props.get(PN_STR_ARRAY), STR_ARRAY_TOSTRING);
    }

    @Test
    public void testGetPayloadProperties_Page() throws Exception {

        // set up jcr properties
        mockJcrProperties();

        Resource payloadRes = mock(Resource.class);
        Resource jcrRes = mock(Resource.class);

        Page payloadPage = mock(Page.class);
        when(payloadRes.adaptTo(Page.class)).thenReturn(payloadPage);
        when(payloadPage.getContentResource()).thenReturn(jcrRes);

        // mock valueMap
        when(jcrRes.getValueMap()).thenReturn(vmap);
        Map<String, String> props = SendTemplatedEmailUtils.getPayloadProperties(payloadRes, sdf);

        assertEquals(props.get(PN_CALENDAR), CALENDAR_TOSTRING);
        assertEquals(props.get(PN_TITLE), STR_TOSTRING);
        assertEquals(props.get(PN_LONG), LONG_TOSTRING);
        assertEquals(props.get(PN_STR_ARRAY), STR_ARRAY_TOSTRING);
    }

    @Test
    public void testGetEmailAddrsNull() throws Exception {

        ResourceResolver resolver = mock(ResourceResolver.class);
        String userPath = "/doesnotexist";

        when(resolver.getResource(userPath)).thenReturn(null);
        String[] emails = SendTemplatedEmailUtils.getEmailAddrsFromUserPath(resolver, userPath);
        assertEquals(0, emails.length);
    }

    @Test
    public void testGetEmailAddrs_User() throws Exception {

        String userPath = "/home/users/a/admin";
        MockValue[] emailVal = new MockValue[] { new MockValue("admin@adobe.com") };

        ResourceResolver resolver = mock(ResourceResolver.class);
        Resource userRes = mock(Resource.class);
        Authorizable adminUser = mock(Authorizable.class);

        when(resolver.getResource(userPath)).thenReturn(userRes);
        when(userRes.adaptTo(Authorizable.class)).thenReturn(adminUser);

        when(adminUser.isGroup()).thenReturn(false);
        when(adminUser.hasProperty(PN_EMAIL)).thenReturn(true);
        when(adminUser.getProperty(PN_EMAIL)).thenReturn(emailVal);

        String[] emails = SendTemplatedEmailUtils.getEmailAddrsFromUserPath(resolver, userPath);

        assertEquals(1, emails.length);
        assertEquals("admin@adobe.com", emails[0]);
    }

    @Test
    public void testGetEmailAddrs_Group() throws Exception {

        // mock group and users
        final String groupPath = "/home/users/g/group";
        final List<Authorizable> groupMembers = new ArrayList<Authorizable>();

        Authorizable user1 = mock(Authorizable.class);
        Authorizable user2 = mock(Authorizable.class);

        when(user1.hasProperty(PN_EMAIL)).thenReturn(true);
        when(user1.getProperty(PN_EMAIL)).thenReturn(new MockValue[] { new MockValue("user1@adobe.com") });

        when(user2.hasProperty(PN_EMAIL)).thenReturn(true);
        when(user2.getProperty(PN_EMAIL)).thenReturn(new MockValue[] { new MockValue("user2@adobe.com") });

        groupMembers.add(user1);
        groupMembers.add(user2);

        ResourceResolver resolver = mock(ResourceResolver.class);
        Resource groupRes = mock(Resource.class);
        Authorizable groupAuth = mock(Authorizable.class);
        Group userGroup = mock(Group.class);

        when(resolver.getResource(groupPath)).thenReturn(groupRes);
        when(groupRes.adaptTo(Authorizable.class)).thenReturn(groupAuth);

        when(groupAuth.isGroup()).thenReturn(true);
        when(groupRes.adaptTo(Group.class)).thenReturn(userGroup);
        when(userGroup.getMembers()).thenReturn(groupMembers.iterator());

        String[] emails = SendTemplatedEmailUtils.getEmailAddrsFromUserPath(resolver, groupPath);
        assertEquals(2, emails.length);
        assertEquals("user1@adobe.com", emails[0]);
        assertEquals("user2@adobe.com", emails[1]);
    }

    private void mockJcrProperties() {
        sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm");

        // Mock jcr properties
        Map<String, Object> jcrProps = new HashMap<String, Object>();
        jcrProps.put(PN_TITLE, "My Title");
        Calendar lastMod = Calendar.getInstance();
        lastMod.set(2014, 8, 12, 1, 30);
        jcrProps.put(PN_CALENDAR, lastMod);
        jcrProps.put(PN_STR_ARRAY, new String[] { "apple", "banana", "carrot" });
        long l = 123456;
        jcrProps.put(PN_LONG, l);

        // mock value map to return jcr properties
        when(vmap.entrySet()).thenReturn(jcrProps.entrySet());
    }

    @After
    public final void tearDown() {
        Mockito.reset();
    }

}