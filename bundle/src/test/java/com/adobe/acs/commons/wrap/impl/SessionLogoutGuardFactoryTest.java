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
package com.adobe.acs.commons.wrap.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import javax.jcr.Session;

import com.adobe.acs.commons.wrap.impl.SessionLogoutGuardFactory;
import com.adobe.acs.commons.wrap.jackrabbit.JackrabbitSessionIWrap;
import com.adobe.acs.commons.wrap.jcr.SessionIWrap;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.junit.Test;

public class SessionLogoutGuardFactoryTest {

    @Test
    public void testUseBestWrapper() {
        assertNull("null should pass through", SessionLogoutGuardFactory.useBestWrapper(null));

        JackrabbitSession mockJackSession = mock(JackrabbitSession.class);
        Session jackWrapper = SessionLogoutGuardFactory.useBestWrapper(mockJackSession);
        assertNotNull("wrap jackSession should not be null", jackWrapper);
        assertTrue("jackWrapper instance of JackrabbitSessionIWrap: " + jackWrapper.getClass().getName(),
                jackWrapper instanceof JackrabbitSessionIWrap);
        assertNotNull("jackWrapper.unwrapSession() instanceof JackrabbitSession",
                ((JackrabbitSessionIWrap) jackWrapper).unwrapSession());

        jackWrapper.logout();
        verify(mockJackSession, times(0)).logout();

        assertSame("jackWrapper should not get rewrapped",
                jackWrapper, SessionLogoutGuardFactory.useBestWrapper(jackWrapper));

        Session mockJcrSession = mock(Session.class);
        Session jcrWrapper = SessionLogoutGuardFactory.useBestWrapper(mockJcrSession);
        assertNotNull("wrap jcrSession should not be null", jcrWrapper);
        assertTrue("jcrWrapper instance of SessionIWrap: " + jcrWrapper.getClass().getName(),
                jcrWrapper instanceof SessionIWrap);
        assertNotNull("jcrWrapper.unwrapSession() instanceof Session",
                ((SessionIWrap) jcrWrapper).unwrapSession());

        jcrWrapper.logout();
        verify(mockJcrSession, times(0)).logout();

        assertSame("jcrWrapper should not get rewrapped",
                jcrWrapper, SessionLogoutGuardFactory.useBestWrapper(jcrWrapper));
    }
}
