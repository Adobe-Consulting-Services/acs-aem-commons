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
package com.adobe.acs.commons.wrap.jackrabbit;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.jcr.Node;
import javax.jcr.Property;

import com.adobe.acs.commons.wrap.jackrabbit.JackrabbitSessionIWrap;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JackrabbitSessionIWrapTest {

    @Mock
    JackrabbitSession session;

    @Mock
    UserManager userManager;

    @Mock
    PrincipalManager principalManager;

    @Mock
    Node mockNode;

    @Mock
    Property mockProperty;

    @Before
    public void setUp() throws Exception {
        when(session.getPrincipalManager()).thenReturn(principalManager);
        when(session.getUserManager()).thenReturn(userManager);
        when(session.hasPermission(anyString(), anyString(), anyString())).thenReturn(true);
        when(session.getItemOrNull("/item/null")).thenReturn(null);
        when(session.getItemOrNull("/item/node")).thenReturn(mockNode);
        when(session.getItemOrNull("/item/property")).thenReturn(mockProperty);
        when(session.getNodeOrNull("/item/null")).thenReturn(null);
        when(session.getNodeOrNull("/item/node")).thenReturn(mockNode);
        when(session.getPropertyOrNull("/item/null")).thenReturn(null);
        when(session.getPropertyOrNull("/item/property")).thenReturn(mockProperty);
    }

    @Test
    public void testWrapper() throws Exception {
        JackrabbitSession wrapper = new JackrabbitSessionWrapper(session);
        assertSame("UserManager should be same as mocked", userManager, wrapper.getUserManager());
        assertSame("PrincipalManager should be same as mocked", principalManager, wrapper.getPrincipalManager());
        assertTrue("hasPermission should return true and be counted.",
                wrapper.hasPermission("/path", "perm1", "perm2"));

        assertNull("getItemOrNull(/item/null) should return null", wrapper.getItemOrNull("/item/null"));
        assertNull("getNodeOrNull(/item/null) should return null", wrapper.getNodeOrNull("/item/null"));
        assertNull("getPropertyOrNull(/item/null) should return null", wrapper.getPropertyOrNull("/item/null"));

        verify(session, times(1)).getItemOrNull("/item/null");
        verify(session, times(1)).getNodeOrNull("/item/null");
        verify(session, times(1)).getPropertyOrNull("/item/null");

        assertSame("getItemOrNull(/item/node) should return mockNode", mockNode, wrapper.getItemOrNull("/item/node"));
        assertSame("getItemOrNull(/item/property) should return mockProperty", mockProperty, wrapper.getItemOrNull("/item/property"));

        verify(session, times(1)).getItemOrNull("/item/node");
        verify(session, times(1)).getItemOrNull("/item/property");

        assertSame("getNodeOrNull(/item/node) should return mockNode", mockNode, wrapper.getNodeOrNull("/item/node"));
        assertSame("getPropertyOrNull(/item/property) should return mockProperty", mockProperty, wrapper.getPropertyOrNull("/item/property"));

        verify(session, times(1)).getNodeOrNull("/item/node");
        verify(session, times(1)).getPropertyOrNull("/item/property");
    }

    static class JackrabbitSessionWrapper implements JackrabbitSessionIWrap {
        final JackrabbitSession session;

        public JackrabbitSessionWrapper(final JackrabbitSession session) {
            this.session = session;
        }

        @Override
        public JackrabbitSession unwrapSession() {
            return session;
        }
    }
}
